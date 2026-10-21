package gradlelens.model

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class LocalGradleModel(
    private val project: Project,
    private val scope: CoroutineScope,
) {
    @Volatile
    var modules: GradleModules = GradleModules.EMPTY
        private set

    private val reloads = Dispatchers.Default.limitedParallelism(1)

    fun reload() {
        scope.launch(reloads) {
            val synced = readGradleModules(project)
            replace { synced }
        }
    }

    fun update(directory: String, change: (GradleModule) -> GradleModule) =
        replace { it.updated(directory, change) }

    private fun replace(transform: (GradleModules) -> GradleModules) {
        val changed = synchronized(this) {
            val previous = modules
            modules = transform(previous)
            modules != previous
        }
        if (changed) project.messageBus.syncPublisher(GradleModulesListener.TOPIC).modulesChanged()
    }
}

interface GradleModulesListener {
    fun modulesChanged()

    companion object {
        @Topic.ProjectLevel
        val TOPIC = Topic(GradleModulesListener::class.java)
    }
}
