package gradlelens.description

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

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
