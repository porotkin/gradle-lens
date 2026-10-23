package gradlelens.dependencylens

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.BulkFileListenerBackgroundable
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import gradlelens.model.GradleModulesListener
import gradlelens.model.LocalGradleModel
import java.nio.file.Path

class RefreshFindingsAfterGradleTask : ExternalSystemTaskNotificationListener {
    override fun onEnd(
        projectPath: String,
        id: ExternalSystemTaskId,
    ) {
        if (id.type != ExternalSystemTaskType.EXECUTE_TASK) {
            return
        }

        val project = id.findProject()
            ?: return

        val reports = project.service<LocalGradleModel>().modules.directories
            .map { Path.of(it, FINDINGS_DIRECTORY) }
        VfsUtil.markDirtyAndRefresh(true, true, true, *reports.toTypedArray())
    }
}

class RehighlightOnFindingsChange(private val project: Project) : BulkFileListenerBackgroundable {
    override fun after(events: List<VFileEvent>) {
        if (events.any { affectsFindings(it.path) }) {
            DaemonCodeAnalyzer.getInstance(project).restart("Dependency Lens findings changed")
        }
    }
}

class RehighlightOnModulesChanged(private val project: Project) : GradleModulesListener {
    override fun modulesChanged() =
        DaemonCodeAnalyzer.getInstance(project).restart("Gradle modules changed")
}

fun affectsFindings(path: String): Boolean =
    "/$FINDINGS_DIRECTORY/" in "$path/" || path.endsWith("/$BUILD_DIRECTORY")
