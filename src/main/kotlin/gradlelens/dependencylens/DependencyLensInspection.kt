package gradlelens.dependencylens

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import gradlelens.dependencies.findDeclaration
import gradlelens.model.LocalGradleModel
import org.jetbrains.kotlin.psi.KtFile

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

        val findings = readFindings(buildScript.parent)
            .mapNotNull { finding ->
                finding.declarationIn(file)?.let {
                    manager.createProblemDescriptor(
                        it,
                        finding.message,
                        isOnTheFly,
                        LocalQuickFix.EMPTY_ARRAY,
                        finding.issue.highlightType,
                    )
                }
            }.toTypedArray()

        return findings
    }
}

fun Finding.declarationIn(script: KtFile): PsiElement? =
    findDeclaration(script, declarations)
