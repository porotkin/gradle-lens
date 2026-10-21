package gradlelens.dependencies

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.jetbrains.plugins.gradle.model.data.GradleProjectBuildScriptData
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.gradleIdentityPathOrNull
import java.io.File

data class Dependent(
    val gradlePath: String,
    val buildScript: File?,
)

fun findGradleModule(directory: PsiDirectory): ModuleData? =
    ExternalSystemApiUtil.findModuleNode(directory.project, GradleConstants.SYSTEM_ID, directory.virtualFile.path)?.data

fun gradleModules(project: Project): Sequence<DataNode<ModuleData>> =
    ProjectDataManager.getInstance()
        .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
        .asSequence()
        .mapNotNull { it.externalProjectStructure }
        .flatMap { ExternalSystemApiUtil.findAll(it, ProjectKeys.MODULE) }

fun dependentsOf(
    target: ModuleData,
    modules: Sequence<DataNode<ModuleData>>,
): List<Dependent> =
    modules
        .filter { !it.data.isSameProjectAs(target) && it.dependsOn(target) }
        .mapNotNull { it.toDependent() }
        .sortedBy(Dependent::gradlePath)
        .toList()

private fun ModuleData.isSameProjectAs(other: ModuleData): Boolean =
    linkedExternalProjectPath == other.linkedExternalProjectPath

private fun DataNode<ModuleData>.dependsOn(target: ModuleData): Boolean =
    ExternalSystemApiUtil.findAllRecursively(this, ProjectKeys.MODULE_DEPENDENCY)
        .any { it.data.target.isSameProjectAs(target) }

private fun DataNode<ModuleData>.toDependent(): Dependent? =
    data.gradleIdentityPathOrNull?.let { Dependent(it, buildScript()) }

private fun DataNode<ModuleData>.buildScript(): File? =
    ExternalSystemApiUtil.find(this, GradleProjectBuildScriptData.KEY)?.data?.buildScriptSource
