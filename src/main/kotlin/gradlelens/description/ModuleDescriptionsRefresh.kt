package gradlelens.description

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.ProjectViewListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.tree.TreeUtil
import gradlelens.model.GradleModulesListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class AttachProjectViewSelection : ProjectActivity {
    override suspend fun execute(project: Project) {
        withContext(Dispatchers.EDT) {
            ProjectView.getInstance(project).currentProjectViewPane?.tree
                ?.let(project.service<ProjectViewSelection>()::attach)
        }
    }
}

class RefreshOnModulesChanged(private val project: Project) : GradleModulesListener {
    override fun modulesChanged() =
        ApplicationManager.getApplication().invokeLater(
            { ProjectView.getInstance(project).refresh() },
            project.disposed,
        )
}

val SELECTED_FILES = Key.create<Set<VirtualFile>>("gradle-lens.project-view.selected-files")

class RefreshOnSelectionChange(private val project: Project) : ProjectViewListener {
    override fun paneShown(current: AbstractProjectViewPane, previous: AbstractProjectViewPane?) {
        current.tree?.let(project.service<ProjectViewSelection>()::attach)
    }
}

@Service(Service.Level.PROJECT)
class ProjectViewSelection(private val project: Project) : TreeSelectionListener, Disposable {
    private var tree: JTree? = null

    fun attach(tree: JTree) {
        this.tree?.removeTreeSelectionListener(this)
        tree.addTreeSelectionListener(this)
        this.tree = tree
    }

    override fun dispose() {
        tree?.removeTreeSelectionListener(this)
    }

    override fun valueChanged(e: TreeSelectionEvent) {
        val tree = e.source as? JTree
            ?: return

        project.putUserData(
            SELECTED_FILES,
            tree.selectionPaths.orEmpty().mapNotNullTo(HashSet()) {
                TreeUtil.getLastUserObject(ProjectViewNode::class.java, it)?.virtualFile
            },
        )

        if (moduleDescriptionsMode != ModuleDescriptionsMode.SELECTED) {
            return
        }

        val pane = ProjectView.getInstance(project).currentProjectViewPane
            ?: return

        for (path in e.paths) {
            pane.updateFrom(path, false, false)
        }
    }
}
