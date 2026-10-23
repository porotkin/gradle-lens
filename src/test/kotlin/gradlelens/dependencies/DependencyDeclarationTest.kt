package gradlelens.dependencies

import gradlelens.BuildFileTestCase
import gradlelens.model.GradleModule
import org.toml.lang.psi.TomlFile

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
        fun offset(gradlePath: String) =
            findDeclaration(file, ProjectNotation(gradlePath))?.textOffset

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
        fun offset(notation: DependencyNotation) =
            findDeclaration(file, notation)?.textOffset
        val coroutines = AccessorNotation(listOf("libs", "kotlinx", "coroutines", "core"))

        assertEquals(script.indexOf("project("), offset(ProjectNotation(":unused")))
        assertEquals(
            script.indexOf("\"org.example"),
            offset(CoordinatesNotation("org.example:lib")),
        )
        assertEquals(script.indexOf("libs.kotlinx.coroutines.core)"), offset(coroutines))
        assertNull(offset(CoordinatesNotation("org.example:li")))
    }

    fun testFindsDeclarationOfModuleFromSameOrOtherBuild() {
        val catalog = myFixture.addFileToProject(
            "gradle/projects.versions.toml",
            """
            [libraries]
            core = { module = "common:core", version.ref = "common" }
            log-core = "common:log-core:1.0"
            flags = { group = "common", name = "flags" }
            """.trimIndent(),
        ) as TomlFile
        val script = """
            dependencies {
                implementation(projects.core)
                implementation(commonLibs.core)
                implementation(commonLibs.log.core)
                implementation(commonLibs.flags)
                implementation("common:graphs:1.0")
            }
        """.trimIndent()
        val file = buildScript(script)
        val app = module(":server:app")
        fun offset(identityPath: String) =
            findDeclaration(
                file,
                declarationNotations(module(identityPath), app, mapOf("commonLibs" to catalog)),
            )?.textOffset

        assertEquals(script.indexOf("projects.core"), offset(":server:core"))
        assertEquals(script.indexOf("commonLibs.core"), offset(":common:core"))
        assertEquals(script.indexOf("commonLibs.log.core"), offset(":common:log-core"))
        assertEquals(script.indexOf("commonLibs.flags"), offset(":common:flags"))
        assertEquals(script.indexOf("\"common:graphs"), offset(":common:graphs"))
        assertNull(offset(":common:missing"))
    }
}

private fun module(identityPath: String) =
    GradleModule(
        directory = "/repo" + identityPath.replace(':', '/'),
        identityPath = identityPath,
        path = ":" + identityPath.substringAfterLast(':'),
        coordinates = identityPath.removePrefix(":"),
        description = null,
        buildScript = null,
        dependencies = emptySet(),
    )
