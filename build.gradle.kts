import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // TODO: remove when we update to gradle 8.1

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlinter)
}

group = "cc.polyfrost"
version = "1.0.2"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "kotlin")
    apply(plugin = "org.quiltmc.gradle.licenser")
    apply(plugin = "org.jmailen.kotlinter")

    group = rootProject.group
    version = rootProject.version

    dependencies {
        api(project.rootProject)
    }
}

// TODO: cleanup with some simple build logic/integrate into textile
allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.polyfrost.cc/releases")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        sourceSets.all {
            languageSettings.apply {
                optIn("kotlin.experimental.ExperimentalTypeInference")
                languageVersion = "2.0"
            }
        }
    }
    tasks {
        withType(KotlinCompile::class).all {
            // todo migrate this when it is in the kotlin {} block
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = listOf(
                    "-Xjvm-default=all-compatibility",
                    "-Xno-call-assertions",
                    "-Xno-receiver-assertions",
                    "-Xno-param-assertions",
                )
            }
        }

        register("format") {
            group = "formatting"
            description = "Formats source code according to project style"
            dependsOn(checkLicenses, formatKotlin)
        }
    }

    kotlinter {
        ignoreFailures = false
        reporters = arrayOf("checkstyle", "plain")
        experimentalRules = true
        disabledRules = arrayOf("no-wildcard-imports", "filename", "max-line-length")
    }

    license {
        rule(rootProject.file("FILEHEADER"))
        include("**/*.kt")
    }
}

dependencies {
    implementation(libs.bundles.slf4j)
    implementation(libs.bundles.kotlin)
}