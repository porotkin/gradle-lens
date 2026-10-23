package gradlelens

import com.intellij.driver.sdk.ui.components.common.popups.documentationHintEditorPane
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
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
    fun listsDependentModulesOfIncludedBuildsInQuickDocumentation() {
        ideContext(
            "listsDependentModulesOfIncludedBuildsInQuickDocumentation",
            fixtureProject("acme-store"),
        ).runInIde {
            projectView { projectViewTree.waitAnyTextsContains("Shared code") }
            showQuickDocumentation("acme-store", "common", "core")
            documentationHintEditorPane {
                waitAnyTextsContains(":common:core")
                waitAnyTextsContains(":server:core")
                waitAnyTextsContains(":server:billing:invoices")
            }
        }
    }
}
