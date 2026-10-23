package gradlelens.dependencies

import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationHandler
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import gradlelens.model.GradleModule
import gradlelens.model.LocalGradleModel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.gradle.service.resolve.getVersionCatalogFiles
import org.toml.lang.psi.TomlFile

class ModuleDependenciesDocumentationProvider :
    DocumentationProvider,
    ExternalDocumentationHandler {

    override fun generateDoc(
        element: PsiElement,
        originalElement: PsiElement?,
    ): String? {
        val directory = element as? PsiDirectory
            ?: return null

        val modules = directory.project.service<LocalGradleModel>().modules
        val module = modules.find(directory.virtualFile)
            ?: return null

        return renderDoc(module, modules.dependentsOf(module))
    }

    override fun handleExternalLink(
        psiManager: PsiManager,
        link: String,
        context: PsiElement?,
    ): Boolean {
        val usage = UsageLink.parse(link)
            ?: return false
        val modules = psiManager.project.service<LocalGradleModel>().modules
        val module = modules[usage.module]
            ?: return true
        val dependent = modules[usage.dependent]
            ?: return true
        val buildScript = dependent.buildScript
            ?.let(LocalFileSystem.getInstance()::findFileByIoFile)
            ?: return true

        ReadAction
            .nonBlocking<Int?> { declarationOffset(psiManager, buildScript, module, dependent) }
            .finishOnUiThread(ModalityState.defaultModalityState()) { offset ->
                OpenFileDescriptor(psiManager.project, buildScript, offset ?: 0)
                    .navigate(true)
            }
            .submit(AppExecutorUtil.getAppExecutorService())

        return true
    }
}

private fun renderDoc(
    module: GradleModule,
    dependents: List<GradleModule>,
): String =
    DEFINITION_START + module.identityPath + DEFINITION_END +
            SECTIONS_START + SECTION_HEADER_START + "Used by:" + SECTION_SEPARATOR +
            dependents.joinToString("<br>") {
                renderDependent(module, it)
            }.ifEmpty { "No modules" } +
            SECTION_END + SECTIONS_END

private fun renderDependent(
    module: GradleModule,
    dependent: GradleModule,
): String =
    dependent.buildScript
        ?.let { UsageLink(module.directory, dependent.directory).encode() }
        ?.let { """<a href="$it">${dependent.identityPath}</a>""" }
        ?: dependent.identityPath

private fun declarationOffset(
    psiManager: PsiManager,
    buildScript: VirtualFile,
    module: GradleModule,
    dependent: GradleModule,
): Int? {
    val script = psiManager.findFile(buildScript) as? KtFile
        ?: return null

    return findDeclaration(script, declarationNotations(module, dependent, versionCatalogs(script)))
        ?.textOffset
}

private fun versionCatalogs(script: KtFile): Map<String, TomlFile> {
    val module = ModuleUtilCore.findModuleForFile(script)
        ?: return emptyMap()

    return getVersionCatalogFiles(module)
        .mapNotNull { (name, file) -> (script.manager.findFile(file) as? TomlFile)?.let { name to it } }
        .toMap()
}
