package gradlelens.description

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.jetbrains.plugins.gradle.util.GradleConstants

class ModuleDescriptionDecorator : ProjectViewNodeDecorator {
    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        if (!moduleDescriptionsEnabled) return
        val project = node.project ?: return
        val directory = (node as? PsiDirectoryNode)?.virtualFile ?: return

        val description = ExternalSystemApiUtil.findModuleNode(
            project,
            GradleConstants.SYSTEM_ID,
            directory.path
        )?.data?.description ?: return

        data.locationString = description
    }
}
