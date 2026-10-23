import gradlelens.buildlogic.GenerateIntegrationTestIde
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("gradlelens.intellij-plugin")
}

val libs = the<LibrariesForLibs>()

val integrationTest: SourceSet = sourceSets.create("integrationTest")
val integrationTestImplementation: Configuration =
    configurations[integrationTest.implementationConfigurationName]

dependencies {
    intellijPlatform {
        testFramework(
            TestFrameworkType.Starter,
            configurationName = integrationTestImplementation.name,
        )
    }
    integrationTestImplementation(libs.kotlin.stdlib)
    integrationTestImplementation(libs.kotlinx.coroutines.core)
    integrationTestImplementation(libs.teamcity.serviceMessages)
}

val generateIntegrationTestIde =
    tasks.register<GenerateIntegrationTestIde>("generateIntegrationTestIde") {
        description = "Generates the IDE version constant the integration tests start the IDE with."
        ideVersion = libs.versions.intellijIdea
        outputDirectory = layout.buildDirectory.dir("generated/integrationTestIde")
    }

kotlin.sourceSets.named(integrationTest.name) {
    kotlin.srcDir(generateIntegrationTestIde.flatMap(GenerateIntegrationTestIde::outputDirectory))
}

intellijPlatformTesting.testIdeUi.register(integrationTest.name) {
    task {
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.runtimeClasspath
        useJUnitPlatform { excludeEngines("junit-vintage") }
        testLogging { events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED) }
    }
}
