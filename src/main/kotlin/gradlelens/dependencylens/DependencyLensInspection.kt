package gradlelens.dependencylens

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import gradlelens.dependencies.DependencyNotation
import gradlelens.dependencies.findDeclaration
import gradlelens.model.LocalGradleModel
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

class DependencyLensInspection : LocalInspectionTool() {
    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        val buildScript = file.virtualFile
            ?: return null

        if (file !is KtFile) {
            return null
        }

        if (!file.project.service<LocalGradleModel>().modules.isBuildScript(buildScript)) {
            return null
        }

        return readFindings(buildScript.parent)
            .problemsIn(file, manager, isOnTheFly)
            .toTypedArray()
    }
}

fun List<Finding>.problemsIn(
    script: KtFile,
    manager: InspectionManager,
    isOnTheFly: Boolean,
): List<ProblemDescriptor> {
    val undeclaredRoutes = filter { it.issue == Issue.UNDECLARED }
        .flatMapTo(HashSet()) { it.declarations }

    return groupBy { it.markIn(script) }
        .mapNotNull { (mark, findings) ->
            mark?.let {
                manager.createProblemDescriptor(
                    it.statement,
                    it.issue.describe(findings),
                    isOnTheFly,
                    it.issue.fixes(findings, undeclaredRoutes),
                    it.issue.highlightType,
                )
            }
        }
}

private data class Mark(
    val statement: PsiElement,
    val issue: Issue,
)

private fun Finding.markIn(script: KtFile): Mark? =
    findDeclaration(script, declarations)?.let { Mark(it.statement(), issue) }

private fun PsiElement.statement(): PsiElement =
    parentsWithSelf.first { it.parent is KtBlockExpression }

private fun Issue.describe(findings: List<Finding>): String {
    val messages = HtmlBuilder()
        .appendWithSeparators(HtmlChunk.text(" "), findings.map { it.message }.distinct().map { HtmlChunk.li().addText(it) })
        .wrapWith(HtmlChunk.ul())

    return HtmlBuilder()
        .append(HtmlChunk.text(declarationTitle).bold())
        .append(": ")
        .append(messages)
        .wrapWith(HtmlChunk.html())
        .toString()
}

private fun Issue.fixes(
    findings: List<Finding>,
    undeclaredRoutes: Set<DependencyNotation>,
): Array<LocalQuickFix> =
    if (this == Issue.UNUSED && findings.none { it.dependency in undeclaredRoutes }) {
        arrayOf(RemoveDependency())
    } else {
        LocalQuickFix.EMPTY_ARRAY
    }

private class RemoveDependency : PsiUpdateModCommandQuickFix() {
    override fun getFamilyName(): String =
        "Remove dependency"

    override fun applyFix(
        project: Project,
        element: PsiElement,
        updater: ModPsiUpdater,
    ) =
        element.delete()
}
