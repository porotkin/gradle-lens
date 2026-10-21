package gradlelens.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GradleModulesTest {
    private val core = module(":core", dependencies = setOf("/repo/core"))
    private val lib = module(":lib", dependencies = setOf("/repo/core"))
    private val app = module(":app", dependencies = setOf("/repo/lib", "/repo/core"))
    private val modules = GradleModules(listOf(lib, core, app))

    @Test
    fun listsOtherModulesDependingOnModuleSortedByPath() {
        assertEquals(listOf(app, lib), modules.dependentsOf(core))
        assertEquals(listOf(app), modules.dependentsOf(lib))
        assertEquals(emptyList<GradleModule>(), modules.dependentsOf(app))
    }

    @Test
    fun updatesOnlyTheChangedModule() {
        val updated = modules.updated("/repo/core") { it.copy(description = "Core module") }

        assertEquals("Core module", updated["/repo/core"]?.description)
        assertEquals(app, updated["/repo/app"])
        assertEquals(listOf(app, lib), updated.dependentsOf(core))
    }

    @Test
    fun ignoresUpdatesOfUnknownModules() =
        assertEquals(modules, modules.updated("/repo/missing") { it.copy(description = "Missing") })
}

private fun module(path: String, dependencies: Set<String>) =
    GradleModule(
        directory = "/repo" + path.replace(':', '/'),
        identityPath = path,
        path = path,
        description = null,
        buildScript = null,
        dependencies = dependencies,
    )
