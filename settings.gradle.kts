pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val kotlinVersion = providers.gradleProperty("kotlin.version").orNull
            if (kotlinVersion != null) {
                version("kotlin", kotlinVersion)
            }
        }
    }
}

rootProject.name = "gradle-lens"
