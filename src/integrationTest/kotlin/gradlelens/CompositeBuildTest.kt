package gradlelens

import com.intellij.driver.sdk.invokeActionByShortcut
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.popups.documentationHintEditorPane
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompositeBuildTest {
    @Test
    fun showsModuleDescriptionsOfIncludedBuilds() {
        ideContext("showsModuleDescriptionsOfIncludedBuilds", fixtureProject("acme-store")).runInIde {
            projectView {
                projectViewTree.expandPath("acme-store", "common", fullMatch = false)
                projectViewTree.expandPath("acme-store", "server", fullMatch = false)
                projectViewTree.waitAnyTextsContains("Shared code")
                projectViewTree.waitAnyTextsContains("Shared core")
                projectViewTree.waitAnyTextsContains("Server core")
            }
        }
    }

    @Test
    fun listsDependentModulesOfIncludedBuildsAndOpensTheirDeclarations() {
        ideContext(
            "listsDependentModulesOfIncludedBuildsAndOpensTheirDeclarations",
            fixtureProject("acme-store"),
        ).runInIde {
            projectView { projectViewTree.waitAnyTextsContains("Shared code") }
            showQuickDocumentation("acme-store", "common", "core")
            documentationHintEditorPane {
                waitAnyTextsContains(":common:core")
                waitOneText(":server:core").click()
            }
            assertEquals("commonLibs.core)", textAtCaret())
            codeEditor().invokeActionByShortcut("CloseContent")

            showQuickDocumentation("acme-store", "common", "core")
            documentationHintEditorPane { waitOneText(":server:billing:invoices").click() }
            assertEquals("\"common:core\")", textAtCaret())
            codeEditor().invokeActionByShortcut("CloseContent")

            showQuickDocumentation("acme-store", "server", "core")
            documentationHintEditorPane { waitOneText(":server:app").click() }
            assertEquals("project(\":core\"))", textAtCaret())
        }
    }
}
