rootProject.name = "JaninoLoader"
include("annotationProcessor")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}
