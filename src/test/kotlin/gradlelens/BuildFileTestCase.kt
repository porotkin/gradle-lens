package gradlelens

import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile

abstract class BuildFileTestCase : BasePlatformTestCase() {
    override fun setUp() =
        LoggedErrorProcessor.executeWith<Throwable>(object : LoggedErrorProcessor() {
            override fun processError(
                category: String,
                message: String,
                details: Array<String>,
                t: Throwable?,
            ) =
                if ("com.intellij.modules.ultimate" in message) {
                    Action.NONE
                } else {
                    super.processError(category, message, details, t)
                }
        }) {
            super.setUp()
        }

    protected fun buildScript(text: String): KtFile =
        myFixture.configureByText("build.gradle.kts", text) as KtFile

    protected fun gradleProperties(text: String): PropertiesFile =
        myFixture.configureByText("gradle.properties", text) as PropertiesFile
}
