package gradlelens

import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.invokeActionByShortcut
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.popups.documentationHintEditorPane
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.PopupItemUiComponent
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.ui.keyboard.RemoteKeyboard
import com.intellij.driver.sdk.ui.ui
import com.intellij.driver.sdk.waitFor
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val EDIT_MODULE_DESCRIPTION_ACTION =
    "com.github.porotkin.gradlelens.EditModuleDescription"
private const val EDIT_MODULE_DESCRIPTION_TEXT = "Edit Module Description…"

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
    fun showsModuleDescriptionsOfProjectOpenedThroughSymlink() {
        val link = createTempDirectory("gradle-lens-integration-test-link")
            .resolve("multi-module")
            .createSymbolicLinkPointingTo(multiModuleProject())

        ideContext("showsModuleDescriptionsOfProjectOpenedThroughSymlink", link).runInIde {
            projectView {
                projectViewTree.waitAnyTextsContains("Core module")
                projectViewTree.waitAnyTextsContains("Library module")
            }
        }
    }

    @Test
    fun hidesAndShowsAllModuleDescriptions() {
        runInMultiModuleProject("hidesAndShowsAllModuleDescriptions") {
            projectView { projectViewTree.waitAnyTextsContains("Core module") }
            moduleDescriptionsMode("None").click()
            projectView {
                projectViewTree.waitNoTextsContain("Core module")
                projectViewTree.waitNoTextsContain("Library module")
            }
            moduleDescriptionsMode("All Modules").click()
            projectView {
                projectViewTree.waitAnyTextsContains("Core module")
                projectViewTree.waitAnyTextsContains("Library module")
            }
        }
    }

    @Test
    fun showsDescriptionsOfSelectedModulesOnly() {
        runInMultiModuleProject("showsDescriptionsOfSelectedModulesOnly") {
            projectView { projectViewTree.waitAnyTextsContains("Core module") }
            moduleDescriptionsMode("Selected Modules").click()
            projectView {
                projectViewTree.waitNoTextsContain("Core module")
                projectViewTree.waitNoTextsContain("Library module")
                projectViewTree.clickPath("multi-module", "core", fullMatch = false)
                projectViewTree.waitAnyTextsContains("Core module")
                projectViewTree.clickPath("multi-module", "lib", fullMatch = false)
                projectViewTree.waitAnyTextsContains("Library module")
                projectViewTree.waitNoTextsContain("Core module")
            }
        }
    }

    @Test
    fun keepsModeAndFollowsSelectionAfterRestartInAnotherPane() {
        ideContext(
            "keepsModeAndFollowsSelectionAfterRestartInAnotherPane",
            multiModuleProject(),
        ).apply {
            runInIde {
                projectView { projectViewTree.waitAnyTextsContains("Core module") }
                moduleDescriptionsMode("Selected Modules").click()
                changeProjectView("Scope", "Project Files")
            }
            runInIde {
                changeProjectView("ProjectPane", null)
                moduleDescriptionsMode("Selected Modules")
                    .apply { assertTrue(isSelected()) }
                    .click()
                projectView {
                    projectViewTree.clickPath("multi-module", "core", fullMatch = false)
                    projectViewTree.waitAnyTextsContains("Core module")
                    projectViewTree.waitNoTextsContain("Library module")
                }
            }
        }
    }

    @Test
    fun hidesModuleDescriptionsWhenLegacySettingDisabledThem() {
        ideContext(
            "hidesModuleDescriptionsWhenLegacySettingDisabledThem",
            multiModuleProject(),
        ).apply {
            paths.configDir.resolve("options").createDirectories().resolve("other.xml").writeText(
                """
                |<application>
                |  <component name="PropertyService"><![CDATA[{
                |  "keyToString": {
                |    "gradle-lens.module-descriptions.enabled": "false"
                |  }
                |}]]></component>
                |</application>
                |""".trimMargin(),
            )
        }.runInIde {
            moduleDescriptionsMode("None").apply { assertTrue(isSelected()) }.click()
            moduleDescriptionsMode("All Modules").click()
            projectView { projectViewTree.waitAnyTextsContains("Core module") }
        }
    }

    @Test
    fun editsModuleDescriptionsWithoutGradleSync() {
        val project = runInMultiModuleProject("editsModuleDescriptionsWithoutGradleSync") {
            projectView {
                projectViewTree.waitAnyTextsContains("Library module")
                projectViewTree.rightClickPath("multi-module", "core", fullMatch = false)
            }
            driver.ui.popupMenu().select(EDIT_MODULE_DESCRIPTION_TEXT)
            keyboard {
                typeText("Edited core")
                enter()
            }
            projectView { projectViewTree.waitAnyTextsContains("Edited core") }
            editModuleDescription("lib") {
                right()
                typeText(" v2")
            }
            projectView { projectViewTree.waitAnyTextsContains("Library module v2") }
        }

        assertEquals(
            "plugins {\n    java\n}\n\ndescription = \"Edited core\"\n",
            project.resolve("core/build.gradle.kts").readText(),
        )
        assertEquals(
            "description=Library module v2\n",
            project.resolve("lib/gradle.properties").readText(),
        )
    }

    @Test
    fun addsAndClearsModuleDescriptionsWithoutGradleSync() {
        val project = runInMultiModuleProject("addsAndClearsModuleDescriptionsWithoutGradleSync") {
            projectView { projectViewTree.waitAnyTextsContains("Core module") }
            editModuleDescription("app") { typeText("App module") }
            projectView { projectViewTree.waitAnyTextsContains("App module") }
            editModuleDescription("legacy") { typeText("Legacy module") }
            projectView { projectViewTree.waitAnyTextsContains("Legacy module") }
            editModuleDescription("core") { backspace() }
            projectView { projectViewTree.waitNoTextsContain("Core module") }
        }

        assertEquals(
            "plugins {\n    java\n}\n\n" +
                    "description = \"App module\"\n\n" +
                    "dependencies {\n    implementation(projects.core)\n}\n",
            project.resolve("app/build.gradle.kts").readText(),
        )
        assertEquals(
            "description=Legacy module\nkotlin.code.style=official\n",
            project.resolve("legacy/gradle.properties").readText(),
        )
        assertEquals(
            "plugins {\n    java\n}\n",
            project.resolve("core/build.gradle.kts").readText(),
        )
    }

    @Test
    fun offersDescriptionEditingOnlyOnModuleDirectories() {
        runInMultiModuleProject("offersDescriptionEditingOnlyOnModuleDirectories") {
            projectView { projectViewTree.waitAnyTextsContains("Core module") }
            assertTrue(EDIT_MODULE_DESCRIPTION_TEXT in contextMenuItems("multi-module", "core"))
            assertFalse(EDIT_MODULE_DESCRIPTION_TEXT in contextMenuItems("multi-module"))
            assertFalse(EDIT_MODULE_DESCRIPTION_TEXT in contextMenuItems("multi-module", "docs"))
        }
    }

    @Test
    fun reloadsModuleDescriptionsOnGradleSync() {
        runInMultiModuleProject("reloadsModuleDescriptionsOnGradleSync") { project ->
            val buildScript = project.resolve("core/build.gradle.kts")
            projectView { projectViewTree.waitAnyTextsContains("Core module") }
            buildScript.writeText(buildScript.readText().replace("Core module", "Synced core"))
            driver.invokeAction("ExternalSystem.RefreshAllProjects", component = component)
            projectView { projectViewTree.waitAnyTextsContains("Synced core", timeout = 5.minutes) }
        }
    }

    @Test
    fun listsDependentModulesInQuickDocumentationAndOpensTheirDeclarations() {
        runInMultiModuleProject(
            "listsDependentModulesInQuickDocumentationAndOpensTheirDeclarations",
        ) {
            projectView { projectViewTree.waitAnyTextsContains("Core module") }
            showQuickDocumentation("lib")
            documentationHintEditorPane { waitAnyTextsContains("No modules") }
            keyboard { escape() }

            showQuickDocumentation("core")
            documentationHintEditorPane { waitOneText(":lib").click() }
            assertEquals("project(\":core\"))", textAtCaret())
            codeEditor().invokeActionByShortcut("CloseContent")

            showQuickDocumentation("core")
            documentationHintEditorPane { waitOneText(":app").click() }
            assertEquals("projects.core)", textAtCaret())
        }
    }
}

@Remote("com.intellij.ide.projectView.ProjectView")
private interface ProjectView {
    fun changeView(viewId: String, subId: String?)
}

private fun multiModuleProject(): Path =
    createTempDirectory("gradle-lens-integration-test").toRealPath().also {
        File("src/integrationTest/projects/multi-module").copyRecursively(it.toFile())
    }

private fun ideContext(testName: String, project: Path): IDETestContext =
    Starter.newContext(
        testName,
        TestCase(IdeInfo.IdeaUltimate, LocalProjectInfo(project))
            .withVersion(IntegrationTestIde.VERSION),
    ).apply {
        PluginConfigurator(this)
            .installPluginFromPath(Path(System.getProperty("path.to.build.plugin")))
    }

private fun IDETestContext.runInIde(actions: IdeaFrameUI.() -> Unit) {
    val result = runIdeWithDriver().useDriverAndCloseIde {
        ideFrame {
            waitForIndicators(5.minutes)
            actions()
        }
    }

    assertEquals(emptyList<Error>(), pluginErrorsIn(result))
}

private fun runInMultiModuleProject(
    testName: String,
    actions: IdeaFrameUI.(project: Path) -> Unit,
): Path {
    val project = multiModuleProject()
    ideContext(testName, project).runInIde { actions(project) }
    return project
}

private fun IdeaFrameUI.editModuleDescription(module: String, typing: RemoteKeyboard.() -> Unit) {
    projectView {
        projectViewTree.clickPath("multi-module", module, fullMatch = false)
        projectViewTree.invokeActionByShortcut(EDIT_MODULE_DESCRIPTION_ACTION)
    }
    keyboard {
        typing()
        enter()
    }
}

private fun IdeaFrameUI.moduleDescriptionsMode(mode: String): PopupItemUiComponent {
    projectView {
        toolWindowHeader.moveMouse()
        toolWindowHeader.optionsButton.click()
    }
    menuItem("Appearance").click()
    menuItem("Module Descriptions").click()
    return menuItem(mode)
}

private fun IdeaFrameUI.menuItem(text: String): PopupItemUiComponent =
    driver.ui.x(PopupItemUiComponent::class.java) {
        and(
            or(byClass("ActionMenu"), byClass("ActionMenuItem")),
            byText(text),
            not("ancestor::div[${byClass("ActionMenu")}]"),
        )
    }

private fun IdeaFrameUI.contextMenuItems(vararg path: String): List<String> {
    projectView { projectViewTree.rightClickPath(*path, fullMatch = false) }
    val items = driver.ui.popupMenu().waitFound().itemsList()
    keyboard { escape() }
    return items
}

private fun IdeaFrameUI.changeProjectView(viewId: String, subId: String?) =
    driver.withContext(OnDispatcher.EDT) {
        service<ProjectView>(singleProject()).changeView(viewId, subId)
    }

private fun IdeaFrameUI.showQuickDocumentation(module: String) =
    projectView {
        projectViewTree.clickPath("multi-module", module, fullMatch = false)
        projectViewTree.invokeActionByShortcut("QuickJavaDoc")
    }

private fun IdeaFrameUI.textAtCaret(): String =
    codeEditor().run { getLineText(getCaretLine()).substring(getCaretColumn() - 1) }

private fun UiComponent.waitNoTextsContain(text: String) =
    waitFor("No texts containing '$text' in $this", timeout = 15.seconds) { !hasSubtext(text) }

private fun pluginErrorsIn(result: IDEStartResult): List<Error> =
    ErrorReporterToCI.collectErrors(result.runContext.logsDir)
        .filter { "gradlelens." in it.stackTraceContent }
