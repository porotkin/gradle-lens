import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellijPlatform)
}

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
    }

    testImplementation(libs.junit)
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

tasks.wrapper {
    gradleVersion = "9.8.0"
}
