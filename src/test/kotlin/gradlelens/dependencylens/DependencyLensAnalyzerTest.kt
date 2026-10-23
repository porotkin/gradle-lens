package gradlelens.dependencylens

import com.intellij.openapi.externalSystem.dependency.analyzer.DAArtifact
import com.intellij.openapi.externalSystem.dependency.analyzer.DAModule
import gradlelens.dependencies.CoordinatesNotation
import gradlelens.dependencies.DependencyNotation
import gradlelens.dependencies.ProjectNotation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyLensAnalyzerTest {
    @Test
    fun matchesArtifactsByGroupAndName() {
        val finding = findingAbout(CoordinatesNotation("org.example:lib"))

        assertTrue(finding.isAbout(DAArtifact("org.example", "lib", "1.0")))
        assertFalse(finding.isAbout(DAArtifact("org.example", "other", "1.0")))
        assertFalse(finding.isAbout(DAModule("lib")))
    }

    @Test
    fun matchesModulesByProjectName() {
        val finding = findingAbout(ProjectNotation(":feature:core"))

        assertTrue(finding.isAbout(DAModule("core")))
        assertFalse(finding.isAbout(DAModule("feature")))
        assertFalse(finding.isAbout(DAArtifact("feature", "core", "1.0")))
    }
}

private fun findingAbout(dependency: DependencyNotation) =
    Finding(Issue.UNUSED, "compileClasspath", dependency, listOf(dependency), "Not needed")
