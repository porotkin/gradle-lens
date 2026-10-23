package gradlelens.dependencies

import gradlelens.BuildFileTestCase

class DependencyDeclarationTest : BuildFileTestCase() {
    fun testFindsDeclaration() {
        val script = """
            dependencies {
                // implementation(project(":core:foo"))
                implementation(projects.core.fooBar)
                implementation(project(":core:foo"))
                api(project(path = ":core"))
            }
        """.trimIndent()
        val file = buildScript(script)
        fun offset(gradlePath: String) = findDeclaration(file, ProjectNotation(gradlePath))?.textOffset

        assertEquals(script.indexOf("project(", script.indexOf("fooBar")), offset(":core:foo"))
        assertEquals(script.indexOf("projects.core.fooBar"), offset(":core:foo-bar"))
        assertEquals(script.indexOf("project(path"), offset(":core"))
        assertNull(offset(":missing"))
    }

    fun testFindsEveryNotation() {
        val script = """
            dependencies {
                jsMainImplementation(project(":unused"))
                jsMainImplementation("org.example:lib:1.0")
                jsMainImplementation(libs.kotlinx.coroutines.core.test)
                jsMainImplementation(libs.kotlinx.coroutines.core)
            }
        """.trimIndent()
        val file = buildScript(script)
        fun offset(notation: DependencyNotation) = findDeclaration(file, notation)?.textOffset
        val coroutines = AccessorNotation(listOf("libs", "kotlinx", "coroutines", "core"))

        assertEquals(script.indexOf("project("), offset(ProjectNotation(":unused")))
        assertEquals(script.indexOf("\"org.example"), offset(CoordinatesNotation("org.example:lib")))
        assertEquals(script.indexOf("libs.kotlinx.coroutines.core)"), offset(coroutines))
        assertNull(offset(CoordinatesNotation("org.example:li")))
    }
}
