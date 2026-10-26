package gradlelens.dependencylens

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandQuickFix
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
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

    return mapNotNull { finding ->
        findDeclaration(script, finding.declarations)?.let {
            manager.createProblemDescriptor(
                it.statement(),
                finding.message,
                isOnTheFly,
                finding.fixes(undeclaredRoutes),
                finding.issue.highlightType,
            )
        }
    }
}

private fun PsiElement.statement(): PsiElement =
    parentsWithSelf.first { it.parent is KtBlockExpression }

private fun Finding.fixes(undeclaredRoutes: Set<DependencyNotation>): Array<LocalQuickFix> =
    if (issue == Issue.UNUSED && dependency !in undeclaredRoutes) {
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
