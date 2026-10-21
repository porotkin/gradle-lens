package gradlelens

import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.util.GradleConstants

fun findGradleModule(project: Project, directory: VirtualFile): ModuleData? =
    directory.canonicalPath?.let {
        ExternalSystemApiUtil.findModuleNode(
            project,
            GradleConstants.SYSTEM_ID,
            it,
        )?.data
    }
