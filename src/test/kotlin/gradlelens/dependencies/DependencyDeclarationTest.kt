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
        fun offset(gradlePath: String) = findDependencyDeclaration(file, gradlePath)?.textOffset

        assertEquals(script.indexOf("project(", script.indexOf("fooBar")), offset(":core:foo"))
        assertEquals(script.indexOf("projects.core.fooBar"), offset(":core:foo-bar"))
        assertEquals(script.indexOf("project(path"), offset(":core"))
        assertNull(offset(":missing"))
    }
}
