import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.time.Year

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.kotlin.abi)
//    alias(libs.plugins.git.hooks)
    alias(libs.plugins.dokka)
    `maven-publish`
}

buildscript {
    dependencies {
        classpath(libs.dokka.base)
    }
}

group = "org.polyfrost"
version = properties["version"] as String
val targetKotlinVersion = KotlinVersion.fromVersion(properties["kotlin.target"] as? String ?: "1.9")
val jvmToolchainVersion = (properties["jvm.toolchain"] as? String ?: "8").toInt()

allprojects {
    apply(plugin = "java-library")
    apply(plugin = rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
    apply(plugin = rootProject.libs.plugins.kotlinter.get().pluginId)
    apply(plugin = rootProject.libs.plugins.dokka.get().pluginId)
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.polyfrost.org/releases")
    }

    dependencies {
        implementation(rootProject.libs.bundles.kotlin)
        implementation(rootProject.libs.logging.api)
        testRuntimeOnly(rootProject.libs.logging.impl)
    }

    java.withSourcesJar()

    kotlin {
        compilerOptions {
            languageVersion = targetKotlinVersion
            apiVersion = targetKotlinVersion
            optIn.add("kotlin.experimental.ExperimentalTypeInference")
            freeCompilerArgs = listOf(
                "-Xcontext-receivers",
                "-Xjvm-default=all-compatibility",
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Xno-param-assertions",
            )
        }
        jvmToolchain(jvmToolchainVersion)
    }

    kotlinter {
        ignoreFailures = false
        reporters = arrayOf("checkstyle", "plain")
    }

    license {
        rule("${rootProject.projectDir}/HEADER")
        include("**/*.kt")
    }

    tasks {
        withType<AbstractDokkaTask> {
            pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
                val rootPath = "${rootProject.projectDir.absolutePath}/format/dokka"
                customStyleSheets = file("$rootPath/styles").listFiles()?.toList() ?: throw IllegalStateException("Please add git submodule https://github.com/Polyfrost/BuildFormat to /format")
                customAssets = file("$rootPath/assets").listFiles()!!.toList()
                templatesDir = file("$rootPath/templates")

                footerMessage = "© ${Year.now().value} PolyUI"
            }

            doLast {
                val scriptsOut = outputDirectory.get().asFile.resolve("scripts")
                val scriptsIn = file("${rootProject.projectDir}/format/dokka/scripts")
                if (project != rootProject) return@doLast
                scriptsIn.listFiles()!!.forEach {
                    it.copyTo(scriptsOut.resolve(it.name), overwrite = true)
                }
            }
        }

        build {
            dependsOn("format")
        }

        register("format") {
            group = "formatting"
            description = "Formats source code according to project style"
            dependsOn(applyLicenses, formatKotlin)
        }

        create("dokkaJavadocJar", Jar::class.java) {
            group = "documentation"
            archiveClassifier = "javadoc"
            from(dokkaJavadoc)
        }

        create("dokkaHtmlJar", Jar::class.java) {
            group = "documentation"
            archiveBaseName = rootProject.name
            archiveClassifier = "dokka"
            from(dokkaHtml.get().outputDirectory)
            duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    }
}

apiValidation {
    ignoredProjects += rootProject.subprojects.map { it.name }
}

subprojects {
    dependencies {
        api(project.rootProject)
    }

    tasks.jar {
        archiveBaseName = "${rootProject.name}-${project.name}"
    }

    tasks.named<Jar>("dokkaJavadocJar") {
        archiveBaseName = "${rootProject.name}-${project.name}"
    }
}

publishing {
    publications {
        create<MavenPublication>("Maven") {
            artifactId = project.name
            version = rootProject.version.toString()

            artifact(tasks.jar.get().archiveFile)

            artifact(tasks.getByName<Jar>("sourcesJar").archiveFile) {
                this.classifier = "sources"
            }

            artifact(tasks.getByName<Jar>("dokkaJavadocJar").archiveFile) {
                builtBy(tasks.getByName("dokkaJavadocJar"))
                this.classifier = "javadoc"
            }

            val dokka = rootProject.tasks.getByName<Jar>("dokkaHtmlJar")
            artifact(dokka.archiveFile) {
                builtBy(dokka)
                classifier = "dokka"
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            url = uri("https://repo.polyfrost.org/releases")
            name = "releases"
            credentials(PasswordCredentials::class)
        }
        maven {
            url = uri("https://repo.polyfrost.org/snapshots")
            name = "snapshots"
            credentials(PasswordCredentials::class)
        }
        maven {
            url = uri("https://repo.polyfrost.org/private")
            name = "private"
            credentials(PasswordCredentials::class)
        }
    }
}

//gitHooks {
//    setHooks(mapOf("pre-commit" to "format"))
//}
