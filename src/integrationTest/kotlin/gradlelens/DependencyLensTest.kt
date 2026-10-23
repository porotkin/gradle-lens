package gradlelens

import com.intellij.driver.client.Remote
import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.sdk.Project
import com.intellij.driver.sdk.openFile
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.waitFor
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

class DependencyLensTest {
    @Test
    @Disabled("io.github.porotkin.dependency-lens 0.1.0 is pending Gradle Plugin Portal approval")
    fun marksFindingsOfDependencyLensTaskInBuildScript() {
        val project = fixtureProject("dependency-lens")

        ideContext("marksFindingsOfDependencyLensTaskInBuildScript", project).runInIde {
            driver.openFile("app/build.gradle.kts", waitForCodeAnalysis = false)
            runGradle(project, "dependencyLens")
            waitFor("Dependency Lens finding on project(\":unused\")", timeout = 5.minutes) {
                ("project(\":unused\")" to "Not needed to compile :app js/main") in highlights()
            }
        }
    }
}

@Remote("org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction", plugin = "com.intellij.gradle")
private interface GradleExecuteTaskAction {
    fun runGradle(project: Project, executor: Any?, workDirectory: String, fullCommandLine: String)
}

private fun IdeaFrameUI.runGradle(project: Path, commandLine: String) =
    driver.withContext(OnDispatcher.EDT) {
        utility(GradleExecuteTaskAction::class)
            .runGradle(singleProject(), executor = null, "$project", commandLine)
    }

private fun IdeaFrameUI.highlights(): List<Pair<String, String?>> =
    codeEditor().getAllHighlights().map { it.getText() to it.getDescription() }
