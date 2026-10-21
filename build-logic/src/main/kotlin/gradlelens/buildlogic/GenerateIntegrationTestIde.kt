package gradlelens.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateIntegrationTestIde : DefaultTask() {
    @get:Input
    abstract val ideVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        outputDirectory.file("gradlelens/IntegrationTestIde.kt").get().asFile.run {
            parentFile.mkdirs()
            writeText(
                """
                |package gradlelens
                |
                |object IntegrationTestIde {
                |    const val VERSION = "${ideVersion.get()}"
                |}
                |""".trimMargin(),
            )
        }
    }
}
