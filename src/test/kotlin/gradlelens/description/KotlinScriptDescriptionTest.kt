package gradlelens.description

import com.intellij.openapi.command.WriteCommandAction
import gradlelens.BuildFileTestCase

class KotlinScriptDescriptionTest : BuildFileTestCase() {
    fun testReplacesExistingDescriptionWithEscapedLiteral() =
        assertWritten(
            """
                plugins {
                    kotlin("jvm")
                }

                description = "Old"
            """,
            "Says \"hi\" for \$5 \\o/",
            """
                plugins {
                    kotlin("jvm")
                }

                description = "Says \"hi\" for \${'$'}5 \\o/"
            """,
        )

    fun testAddsDescriptionAfterPluginsBlock() =
        assertWritten(
            """
                plugins {
                    kotlin("jvm")
                }

                dependencies {}
            """,
            "Core module",
            """
                plugins {
                    kotlin("jvm")
                }

                description = "Core module"

                dependencies {}
            """,
        )

    fun testAddsTopLevelDescriptionIgnoringNestedOnes() =
        assertWritten(
            """
                subprojects {
                    description = "Shared"
                }
            """,
            "Root",
            """
                description = "Root"

                subprojects {
                    description = "Shared"
                }
            """,
        )

    fun testAddsDescriptionToEmptyScript() =
        assertWritten("", "Core module", """description = "Core module"""")

    fun testRemovesClearedDescription() =
        assertWritten(
            """
                description = "Old"

                dependencies {}
            """,
            null,
            """
                dependencies {}
            """,
        )

    private fun assertWritten(
        script: String,
        description: String?,
        expected: String,
    ) {
        val file = buildScript(script.trimIndent())
        WriteCommandAction.runWriteCommandAction(project) { KotlinScriptDescriptionFile(file).write(description) }
        assertEquals(expected.trimIndent(), file.text)
    }
}
