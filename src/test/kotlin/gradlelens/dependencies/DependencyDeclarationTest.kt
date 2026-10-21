package gradlelens.dependencies

import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile

class DependencyDeclarationTest : BasePlatformTestCase() {
    override fun setUp() =
        LoggedErrorProcessor.executeWith<Throwable>(object : LoggedErrorProcessor() {
            override fun processError(category: String, message: String, details: Array<String>, t: Throwable?) =
                if ("com.intellij.modules.ultimate" in message) Action.NONE
                else super.processError(category, message, details, t)
        }) { super.setUp() }

    fun testFindsDeclaration() {
        val script = """
            dependencies {
                // implementation(project(":core:foo"))
                implementation(projects.core.fooBar)
                implementation(project(":core:foo"))
                api(project(path = ":core"))
            }
        """.trimIndent()
        val file = myFixture.configureByText("build.gradle.kts", script) as KtFile
        fun offset(gradlePath: String) = findDependencyDeclaration(file, gradlePath)?.textOffset

        assertEquals(script.indexOf("project(", script.indexOf("fooBar")), offset(":core:foo"))
        assertEquals(script.indexOf("projects.core.fooBar"), offset(":core:foo-bar"))
        assertEquals(script.indexOf("project(path"), offset(":core"))
        assertNull(offset(":missing"))
    }
}
