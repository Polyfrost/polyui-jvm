
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    // Depend on LWJGL3 and its NanoVG bindings, as well as OpenGL
    implementation("org.lwjgl:lwjgl:3.3.1")
    implementation("org.lwjgl:lwjgl-nanovg:3.3.1")
    implementation("org.lwjgl:lwjgl-opengl:3.3.1")
    implementation("org.lwjgl:lwjgl-stb:3.3.1")

    // Use glfw for test window runner
    implementation("org.lwjgl:lwjgl-glfw:3.3.1")

    // Add LWJGL modules' native bindings to the test runtime
    val nativePlatforms = listOf("windows", "linux", "macos", "macos-arm64")
    val modules = listOf("lwjgl", "lwjgl-nanovg", "lwjgl-opengl", "lwjgl-glfw", "lwjgl-stb")
    modules.forEach { module ->
        nativePlatforms.forEach { platform ->
            testRuntimeOnly("org.lwjgl:$module:3.3.1:natives-$platform")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
            "-Xno-param-assertions"
        )
    }
}