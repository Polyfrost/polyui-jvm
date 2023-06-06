dependencies {
    // Depend on LWJGL3 and its NanoVG bindings, as well as OpenGL
    implementation(libs.bundles.lwjgl)
    implementation(libs.slf4j.api)

    // Add LWJGL modules' native bindings to the test runtime
    val nativePlatforms = listOf("windows", "linux", "macos", "macos-arm64")
    val modules = listOf("lwjgl", "lwjgl-nanovg", "lwjgl-opengl", "lwjgl-glfw", "lwjgl-stb")
    modules.forEach { module ->
        nativePlatforms.forEach { platform ->
            testRuntimeOnly("org.lwjgl:$module:3.3.1:natives-$platform")
        }
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "cc.polyfrost.polyui.TestKt"
    }
    from(sourceSets.test.get().output)
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().mapNotNull { if(!it.exists()) null else if (it.isDirectory) it else zipTree(it) })
    from(configurations.testRuntimeClasspath.get().mapNotNull { if(!it.exists()) null else if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

