import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `kotlin-dsl`
}

val versionCatalogAccessors = files(LibrariesForLibs::class.java.protectionDomain.codeSource.location)

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.intellijPlatform.gradlePlugin)
    implementation(versionCatalogAccessors)
}
