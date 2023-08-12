plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/skija/maven")
}

dependencies {
    implementation("org.jetbrains.skija:skija-linux:0.93.1")
    implementation(libs.bundles.lwjgl)
    implementation(libs.bundles.slf4j)
    val nativePlatforms = listOf("windows", "linux", "macos", "macos-arm64")
    val modules = listOf("lwjgl", "lwjgl-opengl", "lwjgl-glfw", "lwjgl-stb")
    modules.forEach { module ->
        nativePlatforms.forEach { platform ->
            testRuntimeOnly("org.lwjgl:$module:3.3.1:natives-$platform")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}