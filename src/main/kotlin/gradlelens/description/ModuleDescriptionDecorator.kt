package gradlelens.description

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.components.service
import gradlelens.model.LocalGradleModel

class ModuleDescriptionDecorator : ProjectViewNodeDecorator {
    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val mode = moduleDescriptionsMode
        if (mode == ModuleDescriptionsMode.NONE) return
        val project = node.project ?: return
        val directory = (node as? PsiDirectoryNode)?.virtualFile ?: return
        if (mode == ModuleDescriptionsMode.SELECTED && directory !in project.getUserData(SELECTED_FILES)
                .orEmpty()
        ) return

        val description = project.service<LocalGradleModel>().modules.find(directory)?.description ?: return

        data.locationString = description
    }
}
