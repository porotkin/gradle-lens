rootProject.name = "server"

dependencyResolutionManagement {
    versionCatalogs {
        create("commonLibs") {
            from(files("../common/gradle/projects.versions.toml"))
        }
    }
}

includeBuild("../common")

include("app")
include("core")
