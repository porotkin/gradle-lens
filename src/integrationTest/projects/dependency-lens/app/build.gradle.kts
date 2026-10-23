plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dependencyLens)
}

kotlin {
    js()

    sourceSets {
        jsMain.dependencies {
            implementation(project(":unused"))
        }
    }
}
