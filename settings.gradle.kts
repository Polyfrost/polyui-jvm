pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.polyfrost.cc/releases")
    }
}

rootProject.name = "polyui"

include(":nanovg-impl")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
