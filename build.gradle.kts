plugins {
    alias(libs.plugins.gradlelens.intellijPlugin)
    alias(libs.plugins.gradlelens.integrationTest)
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellijIdea)

        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.plugins.gradle")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.toml.lang")
    }
}

intellijPlatform {
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
