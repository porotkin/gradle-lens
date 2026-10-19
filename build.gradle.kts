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
        intellijIdea("2026.2.3")
        bundledPlugin("com.intellij.gradle")
    }
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
