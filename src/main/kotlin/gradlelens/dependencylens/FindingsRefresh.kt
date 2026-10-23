package gradlelens.dependencylens

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.Project
import gradlelens.model.GradleModulesListener

class RehighlightOnModulesChanged(private val project: Project) : GradleModulesListener {
    override fun modulesChanged() =
        DaemonCodeAnalyzer.getInstance(project).restart("Gradle modules changed")
}
