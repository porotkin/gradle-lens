package gradlelens.dependencies

import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationHandler
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import gradlelens.findGradleModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.gradle.util.gradleIdentityPathOrNull
import org.jetbrains.plugins.gradle.util.gradlePathOrNull

class ModuleDependenciesDocumentationProvider : DocumentationProvider, ExternalDocumentationHandler {
    override fun generateDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? {
        val directory = element as? PsiDirectory ?: return null
        val module = findGradleModule(directory.project, directory.virtualFile) ?: return null
        val path = module.gradleIdentityPathOrNull ?: return null
        val dependents = dependentsOf(module, gradleModules(directory.project))
        return renderDoc(path, module.gradlePathOrNull ?: path, dependents)
    }

    override fun handleExternalLink(
        psiManager: PsiManager,
        link: String,
        context: PsiElement?,
    ): Boolean {
        val usage = UsageLink.parse(link) ?: return false
        val buildScript = LocalFileSystem.getInstance().findFileByPath(usage.buildScript) ?: return false
        ReadAction.nonBlocking<Int?> { declarationOffset(psiManager, buildScript, usage.gradlePath) }
            .finishOnUiThread(ModalityState.defaultModalityState()) { offset ->
                OpenFileDescriptor(psiManager.project, buildScript, offset ?: 0).navigate(true)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
        return true
    }
}

private fun renderDoc(
    path: String,
    declaredAs: String,
    dependents: List<Dependent>,
): String =
    DEFINITION_START + path + DEFINITION_END +
            SECTIONS_START + SECTION_HEADER_START + "Used by:" + SECTION_SEPARATOR +
            dependents.joinToString("<br>") { renderDependent(it, declaredAs) }.ifEmpty { "No modules" } +
            SECTION_END + SECTIONS_END

private fun renderDependent(
    dependent: Dependent,
    declaredAs: String,
): String =
    dependent.buildScript
        ?.let { """<a href="${UsageLink(declaredAs, it.path).encode()}">${dependent.gradlePath}</a>""" }
        ?: dependent.gradlePath

private fun declarationOffset(
    psiManager: PsiManager,
    buildScript: VirtualFile,
    gradlePath: String,
): Int? =
    (psiManager.findFile(buildScript) as? KtFile)?.let { findDependencyDeclaration(it, gradlePath) }?.textOffset
