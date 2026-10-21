package gradlelens

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.models.IDEStartResult
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.report.Error
import com.intellij.ide.starter.report.ErrorReporterToCI
import com.intellij.ide.starter.runner.Starter
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.time.Duration.Companion.minutes

class ModuleDescriptionsTest {
    @Test
    fun showsModuleDescriptionInProjectView() {
        val project = createTempDirectory("gradle-lens-integration-test").toRealPath()
        File("src/integrationTest/projects/multi-module").copyRecursively(project.toFile())

        val result = Starter.newContext(
            "showsModuleDescriptionInProjectView",
            TestCase(IdeInfo.IdeaUltimate, LocalProjectInfo(project))
                .withVersion(IntegrationTestIde.VERSION),
        ).apply {
            PluginConfigurator(this).installPluginFromPath(Path(System.getProperty("path.to.build.plugin")))
        }.runIdeWithDriver().useDriverAndCloseIde {
            ideFrame {
                waitForIndicators(5.minutes)
                projectView { projectViewTree.waitAnyTextsContains("Core module") }
            }
        }

        assertEquals(emptyList<Error>(), pluginErrorsIn(result))
    }
}

private fun pluginErrorsIn(result: IDEStartResult): List<Error> =
    ErrorReporterToCI.collectErrors(result.runContext.logsDir)
        .filter { "gradlelens." in it.stackTraceContent }
