package gradlelens

import com.intellij.driver.sdk.invokeActionByShortcut
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.ui.ui
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
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.minutes

class ModuleDescriptionsTest {
    @Test
    fun showsModuleDescriptionInProjectView() {
        runInMultiModuleProject("showsModuleDescriptionInProjectView") {
            projectView {
                projectViewTree.waitAnyTextsContains("Core module")
                projectViewTree.waitAnyTextsContains("Library module")
            }
        }
    }

    @Test
    fun editsModuleDescriptionsWithoutGradleSync() {
        val project = runInMultiModuleProject("editsModuleDescriptionsWithoutGradleSync") {
            projectView {
                projectViewTree.waitAnyTextsContains("Library module")
                projectViewTree.rightClickPath("multi-module", "core", fullMatch = false)
            }
            driver.ui.popupMenu().select("Edit Module Description…")
            keyboard {
                typeText("Edited core")
                enter()
            }
            projectView {
                projectViewTree.waitAnyTextsContains("Edited core")
                projectViewTree.clickPath("multi-module", "lib", fullMatch = false)
                projectViewTree.invokeActionByShortcut("com.github.porotkin.gradlelens.EditModuleDescription")
            }
            keyboard {
                typeText("Edited lib")
                enter()
            }
            projectView { projectViewTree.waitAnyTextsContains("Edited lib") }
        }

        assertEquals("description = \"Edited core\"\n", project.resolve("core/build.gradle.kts").readText())
        assertEquals("description=Edited lib\n", project.resolve("lib/gradle.properties").readText())
    }
}

private fun runInMultiModuleProject(
    testName: String,
    actions: IdeaFrameUI.() -> Unit,
): Path {
    val project = createTempDirectory("gradle-lens-integration-test")
    File("src/integrationTest/projects/multi-module").copyRecursively(project.toFile())

    val result = Starter.newContext(
        testName,
        TestCase(IdeInfo.IdeaUltimate, LocalProjectInfo(project))
            .withVersion(IntegrationTestIde.VERSION),
    ).apply {
        PluginConfigurator(this).installPluginFromPath(Path(System.getProperty("path.to.build.plugin")))
    }.runIdeWithDriver().useDriverAndCloseIde {
        ideFrame {
            waitForIndicators(5.minutes)
            actions()
        }
    }

    assertEquals(emptyList<Error>(), pluginErrorsIn(result))
    return project
}

private fun pluginErrorsIn(result: IDEStartResult): List<Error> =
    ErrorReporterToCI.collectErrors(result.runContext.logsDir)
        .filter { "gradlelens." in it.stackTraceContent }
