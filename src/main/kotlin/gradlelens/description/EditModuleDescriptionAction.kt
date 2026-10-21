package gradlelens.description

import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import gradlelens.model.GradleModule
import gradlelens.model.LocalGradleModel
import org.jetbrains.plugins.gradle.util.GradleConstants

class EditModuleDescriptionAction : DumbAwareAction() {
    override fun getActionUpdateThread() =
        ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.selectedModule()
            ?.descriptionFilesByPrecedence()?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val module = e.selectedModule() ?: return
        val psiManager = PsiManager.getInstance(project)
        val target = chooseDescriptionFile(
            module.descriptionFilesByPrecedence()
                .mapNotNull { psiManager.findFile(it)?.let(::descriptionFileOf) },
        ) ?: return

        val panel = NewItemSimplePopupPanel()
        val field = panel.textField.apply {
            text = module.description.orEmpty()
            selectAll()
        }

        val popup = NewItemPopupUtil.createNewItemPopup("Description of ${module.identityPath}", panel, field)
        panel.setApplyAction { event ->
            popup.closeOk(event)
            saveDescription(project, module, target, field.text.trim().ifEmpty { null })
        }
        popup.showInBestPositionFor(e.dataContext)
    }
}

private fun AnActionEvent.selectedModule(): GradleModule? {
    val project = project
        ?: return null
    val directory = getData(CommonDataKeys.VIRTUAL_FILE)
        ?.takeIf(VirtualFile::isDirectory)
        ?: return null

    return project.service<LocalGradleModel>().modules.find(directory)
}

private fun GradleModule.descriptionFilesByPrecedence(): List<VirtualFile> {
    val fileSystem = LocalFileSystem.getInstance()
    return listOfNotNull(
        buildScript
            ?.takeIf { it.name.endsWith(GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION) }
            ?.let(fileSystem::findFileByIoFile),
        if (isBuildRoot) null else fileSystem.findFileByPath(directory)
            ?.findChild(GradleConstants.GRADLE_PROPERTIES_FILE_NAME),
    )
}

private fun saveDescription(
    project: Project,
    module: GradleModule,
    target: DescriptionFile,
    description: String?,
) {
    if (description == module.description)
        return

    WriteCommandAction.writeCommandAction(project, target.psiFile)
        .withName("Edit Module Description")
        .run<RuntimeException> {
            if (!target.psiFile.isValid)
                return@run

            target.write(description)

            project.service<LocalGradleModel>()
                .update(module.directory) {
                    it.copy(description = description)
                }
        }
}
