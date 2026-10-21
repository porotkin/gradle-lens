package gradlelens.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.CollectionFactory
import java.io.File

data class GradleModule(
    val directory: String,
    val identityPath: String,
    val path: String,
    val description: String?,
    val buildScript: File?,
    val dependencies: Set<String>,
) {
    val isBuildRoot: Boolean
        get() = path == ":"
}

@JvmInline
value class GradleModules private constructor(
    private val byDirectory: Map<String, GradleModule>,
) {
    constructor(modules: Iterable<GradleModule>) :
            this(
                modules.associateByTo(
                    CollectionFactory.createFilePathMap(),
                    GradleModule::directory,
                )
            )

    operator fun get(directory: String): GradleModule? =
        byDirectory[directory]

    fun find(directory: VirtualFile): GradleModule? =
        directory.canonicalPath?.let(::get)

    fun dependentsOf(module: GradleModule): List<GradleModule> =
        byDirectory.values
            .filter { it.directory != module.directory && module.directory in it.dependencies }
            .sortedBy(GradleModule::identityPath)

    fun updated(directory: String, change: (GradleModule) -> GradleModule): GradleModules {
        val module = this[directory] ?: return this
        return GradleModules(byDirectory.values.map { if (it === module) change(it) else it })
    }

    companion object {
        val EMPTY = GradleModules(emptyList())
    }
}
