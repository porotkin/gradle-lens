package gradlelens.description

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.ProjectManager

class ToggleModuleDescriptionsAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent) = moduleDescriptionsEnabled

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        moduleDescriptionsEnabled = state

        for (it in ProjectManager.getInstance().openProjects) {
            ProjectView.getInstance(it).refresh()
        }
    }
}
