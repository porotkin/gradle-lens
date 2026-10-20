package gradlelens.description

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.ProjectViewListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class RefreshOnGradleDataLoaded : ProjectActivity {
    override suspend fun execute(project: Project) =
        ExternalProjectsManager.getInstance(project).runWhenInitialized {
            refreshProjectView(project)
        }
}

class RefreshOnGradleSync(private val project: Project) : ProjectDataImportListener {
    override fun onImportFinished(projectPath: String?) =
        refreshProjectView(project)
}

private fun refreshProjectView(project: Project) =
    ApplicationManager.getApplication().invokeLater(
        { ProjectView.getInstance(project).refresh() },
        project.disposed,
    )

val SELECTED_FILES = Key.create<Set<VirtualFile>>("gradle-lens.project-view.selected-files")

class RefreshOnSelectionChange(private val project: Project) : ProjectViewListener, TreeSelectionListener {
    override fun paneShown(current: AbstractProjectViewPane, previous: AbstractProjectViewPane?) {
        val tree = current.tree ?: return
        tree.removeTreeSelectionListener(this)
        tree.addTreeSelectionListener(this)
    }

    override fun valueChanged(e: TreeSelectionEvent) {
        val tree = e.source as? JTree ?: return
        project.putUserData(
            SELECTED_FILES,
            tree.selectionPaths.orEmpty().mapNotNullTo(HashSet()) {
                TreeUtil.getLastUserObject(ProjectViewNode::class.java, it)?.virtualFile
            },
        )
        if (moduleDescriptionsMode != ModuleDescriptionsMode.SELECTED) return

        val pane = ProjectView.getInstance(project).currentProjectViewPane ?: return
        for (path in e.paths) {
            pane.updateFrom(path, false, false)
        }
    }
}
