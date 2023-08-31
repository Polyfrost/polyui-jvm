pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.polyfrost.org/releases")
    }
}

rootProject.name = "polyui"

include(
    ":nanovg-impl",
    //"skija-impl"
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
