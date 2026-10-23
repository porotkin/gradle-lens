package gradlelens

import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
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
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.time.Duration.Companion.minutes

internal fun fixtureProject(name: String): Path =
    createTempDirectory("gradle-lens-integration-test").toRealPath().also {
        File("src/integrationTest/projects/$name").copyRecursively(it.toFile())
    }

internal fun ideContext(testName: String, project: Path): IDETestContext =
    Starter.newContext(
        testName,
        TestCase(IdeInfo.IdeaUltimate, LocalProjectInfo(project))
            .withVersion(IntegrationTestIde.VERSION),
    ).apply {
        PluginConfigurator(this)
            .installPluginFromPath(Path(System.getProperty("path.to.build.plugin")))
    }

internal fun IDETestContext.runInIde(actions: IdeaFrameUI.() -> Unit) {
    val result = runIdeWithDriver().useDriverAndCloseIde {
        ideFrame {
            waitForIndicators(5.minutes)
            actions()
        }
    }

    assertEquals(emptyList<Error>(), pluginErrorsIn(result))
}

private fun pluginErrorsIn(result: IDEStartResult): List<Error> =
    ErrorReporterToCI.collectErrors(result.runContext.logsDir)
        .filter { "gradlelens." in it.stackTraceContent }
