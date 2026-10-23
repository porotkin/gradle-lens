package gradlelens.model

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.gradle.model.data.GradleProjectBuildScriptData
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.gradleIdentityPathOrNull
import org.jetbrains.plugins.gradle.util.gradlePathOrNull

class LoadGradleModelOnStartup : ProjectActivity {
    override suspend fun execute(project: Project) =
        ExternalProjectsManager.getInstance(project)
            .runWhenInitialized(project.service<LocalGradleModel>()::reload)
}

class ReloadGradleModelOnSync(private val project: Project) : ProjectDataImportListener {
    override fun onImportFinished(projectPath: String?) =
        project.service<LocalGradleModel>().reload()
}

fun readGradleModules(project: Project): GradleModules =
    GradleModules(
        ProjectDataManager.getInstance()
            .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
            .asSequence()
            .mapNotNull { it.externalProjectStructure }
            .flatMap { ExternalSystemApiUtil.findAll(it, ProjectKeys.MODULE) }
            .mapNotNull { it.toGradleModule() }
            .asIterable(),
    )

private fun DataNode<ModuleData>.toGradleModule(): GradleModule? {
    val identityPath = data.gradleIdentityPathOrNull
        ?: return null

    return GradleModule(
        directory = data.linkedExternalProjectPath,
        identityPath = identityPath,
        path = data.gradlePathOrNull ?: identityPath,
        coordinates = data.group
            ?.takeUnless(String::isEmpty)
            ?.let { "$it:${data.externalName}" },
        description = data.description,
        buildScript = ExternalSystemApiUtil.find(this, GradleProjectBuildScriptData.KEY)
            ?.data?.buildScriptSource,
        dependencies = ExternalSystemApiUtil.findAllRecursively(this, ProjectKeys.MODULE_DEPENDENCY)
            .mapTo(HashSet()) { it.data.target.linkedExternalProjectPath },
    )
}
