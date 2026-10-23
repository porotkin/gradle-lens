package gradlelens.dependencylens

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.DAWarning
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerContributor
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerExtension
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerProject
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import gradlelens.dependencies.CoordinatesNotation
import gradlelens.dependencies.ProjectNotation
import org.jetbrains.plugins.gradle.util.GradleConstants

class DependencyLensAnalyzerExtension : DependencyAnalyzerExtension {
    override fun isApplicable(systemId: ProjectSystemId): Boolean = systemId == GradleConstants.SYSTEM_ID

    override fun createContributor(
        project: Project,
        parentDisposable: Disposable,
    ): DependencyAnalyzerContributor {
        val gradleExtension = DependencyAnalyzerExtension.EP_NAME.extensionList
            .first { it !is DependencyLensAnalyzerExtension && it.isApplicable(GradleConstants.SYSTEM_ID) }
        return FindingsContributor(gradleExtension.createContributor(project, parentDisposable))
    }
}

private class FindingsContributor(
    private val gradle: DependencyAnalyzerContributor,
) : DependencyAnalyzerContributor by gradle {
    override fun getDependencies(externalProject: DependencyAnalyzerProject): List<DependencyAnalyzerDependency> {
        val dependencies = gradle.getDependencies(externalProject)
        val directory = ExternalSystemApiUtil.getExternalProjectPath(externalProject.module)
            ?.let(LocalFileSystem.getInstance()::findFileByPath)
            ?: return dependencies
        val findings = readFindings(directory).ifEmpty { return dependencies }
        val replacements = HashMap<DependencyAnalyzerDependency, DependencyAnalyzerDependency>()
        fun replace(dependency: DependencyAnalyzerDependency): DependencyAnalyzerDependency =
            replacements.getOrPut(dependency) {
                val warnings = findings
                    .filter { it.classpath == dependency.scope.name && it.isAbout(dependency.data) }
                    .distinctBy { it.message }
                    .map { DAWarning(it.issue.title, it.message) }
                Flagged(dependency, dependency.parent?.let(::replace), dependency.status + warnings)
            }
        return dependencies.map(::replace)
    }
}

private data class Flagged(
    val original: DependencyAnalyzerDependency,
    override val parent: DependencyAnalyzerDependency?,
    override val status: List<DependencyAnalyzerDependency.Status>,
) : DependencyAnalyzerDependency by original

fun Finding.isAbout(data: DependencyAnalyzerDependency.Data): Boolean =
    when (data) {
        is DependencyAnalyzerDependency.Data.Artifact ->
            dependency == CoordinatesNotation("${data.groupId}:${data.artifactId}")

        is DependencyAnalyzerDependency.Data.Module -> dependency is ProjectNotation && dependency.name == data.name
    }
