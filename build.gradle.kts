import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellijPlatform)
}

val integrationTest: SourceSet = sourceSets.create("integrationTest")
val integrationTestImplementation: Configuration = configurations[integrationTest.implementationConfigurationName]

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellijIdea)

        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Starter, configurationName = integrationTestImplementation.name)
    }

    testImplementation(libs.junit)
    integrationTestImplementation(libs.kotlin.stdlib)
    integrationTestImplementation(libs.kotlinx.coroutines.core)
    integrationTestImplementation(libs.teamcity.serviceMessages)
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmDefault = org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }
    publishing {
        token = providers.gradleProperty("intellijPublishToken")
    }
}

val generateIntegrationTestIde = tasks.register("generateIntegrationTestIde") {
    val version = libs.versions.intellijIdea
    val sources = layout.buildDirectory.dir("generated/integrationTestIde")
    inputs.property("version", version)
    outputs.dir(sources)
    doLast {
        sources.get().file("gradlelens/IntegrationTestIde.kt").asFile.run {
            parentFile.mkdirs()
            writeText("package gradlelens\n\nobject IntegrationTestIde {\n    const val VERSION = \"${version.get()}\"\n}\n")
        }
    }
}

kotlin.sourceSets.named(integrationTest.name) {
    kotlin.srcDir(generateIntegrationTestIde)
}

intellijPlatformTesting.testIdeUi.register(integrationTest.name) {
    task {
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.runtimeClasspath
        javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
        useJUnitPlatform { excludeEngines("junit-vintage") }
    }
}

tasks.wrapper {
    gradleVersion = "9.8.0"
}
