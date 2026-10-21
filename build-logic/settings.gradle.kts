dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))

            val kotlinVersion = providers.gradleProperty("kotlin.version").orNull
            if (kotlinVersion != null) {
                version("kotlin", kotlinVersion)
            }
        }
    }
}

rootProject.name = "build-logic"
