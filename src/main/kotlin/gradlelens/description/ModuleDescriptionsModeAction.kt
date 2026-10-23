package gradlelens.description

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.ProjectManager

sealed class ModuleDescriptionsModeAction(
    private val mode: ModuleDescriptionsMode,
) : DumbAwareToggleAction() {
    override fun getActionUpdateThread() =
        ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent) =
        moduleDescriptionsMode == mode

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        moduleDescriptionsMode = mode

        for (project in ProjectManager.getInstance().openProjects) {
            ProjectView.getInstance(project).refresh()
        }
    }
}

class ShowAllModuleDescriptionsAction :
    ModuleDescriptionsModeAction(ModuleDescriptionsMode.ALL)

class ShowSelectedModuleDescriptionsAction :
    ModuleDescriptionsModeAction(ModuleDescriptionsMode.SELECTED)

class HideModuleDescriptionsAction :
    ModuleDescriptionsModeAction(ModuleDescriptionsMode.NONE)
