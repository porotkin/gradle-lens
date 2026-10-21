package gradlelens.description

import com.intellij.openapi.command.WriteCommandAction
import gradlelens.BuildFileTestCase

class GradlePropertiesDescriptionTest : BuildFileTestCase() {
    fun testReplacesExistingDescriptionWithAsciiEscapedValue() =
        assertWritten(
            """
                org.gradle.caching=true
                description=Old
                kotlin.code.style=official
            """,
            "Модуль \\ core",
            """
                org.gradle.caching=true
                description=${"\\u041c\\u043e\\u0434\\u0443\\u043b\\u044c"} \\ core
                kotlin.code.style=official
            """,
        )

    fun testAddsMissingDescriptionKeepingSortedOrder() =
        assertWritten(
            """
                org.gradle.caching=true
            """,
            "Core module",
            """
                description=Core module
                org.gradle.caching=true
            """,
        )

    fun testRemovesClearedDescription() =
        assertWritten(
            """
                description=Old
                org.gradle.caching=true
            """,
            null,
            """
                org.gradle.caching=true
            """,
        )

    fun testPrefersTheFileThatDeclaresTheDescription() {
        val script = KotlinScriptDescriptionFile(buildScript("plugins {}"))
        val declaringProperties = GradlePropertiesDescriptionFile(gradleProperties("description=Core module"))

        assertSame(declaringProperties, chooseDescriptionFile(listOf(script, declaringProperties)))
    }

    fun testPrefersTheBuildScriptWhenBothOrNeitherDeclare() {
        val declaringScript = KotlinScriptDescriptionFile(buildScript("description = \"Script\""))
        val declaringProperties = GradlePropertiesDescriptionFile(gradleProperties("description=Properties"))
        val emptyScript = KotlinScriptDescriptionFile(buildScript("plugins {}"))
        val emptyProperties = GradlePropertiesDescriptionFile(gradleProperties("org.gradle.caching=true"))

        assertSame(declaringScript, chooseDescriptionFile(listOf(declaringScript, declaringProperties)))
        assertSame(emptyScript, chooseDescriptionFile(listOf(emptyScript, emptyProperties)))
    }

    private fun assertWritten(
        properties: String,
        description: String?,
        expected: String,
    ) {
        val file = gradleProperties(properties.trimIndent())
        WriteCommandAction.runWriteCommandAction(project) { GradlePropertiesDescriptionFile(file).write(description) }
        assertEquals(expected.trimIndent(), file.containingFile.text)
    }
}
