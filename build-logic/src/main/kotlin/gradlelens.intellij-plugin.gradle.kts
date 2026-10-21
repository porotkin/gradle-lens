import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

val libs = the<LibrariesForLibs>()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation(libs.junit)
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
}

intellijPlatform {
    buildSearchableOptions = false
}
