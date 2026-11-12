import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(25)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_25) }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.testJunit5)
    // Gradle 9 no longer supplies test framework runtime dependencies implicitly.
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

compose.desktop {
    application {
        mainClass = "dev.jarful.desktop.MainKt"
        // Skiko and jSerialComm load native libraries on JDK 25.
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Jarful"
            packageVersion = "1.7.4"
            description = "Game-loop task manager for ADHD brains"
            vendor = "Jarful contributors"
            licenseFile.set(rootProject.file("LICENSE"))

            windows {
                menuGroup = "Jarful"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "7f1d7d3e-5b64-4c6a-9d2f-2b2f4d1e6c01"
                iconFile.set(project.file("icons/jarful.ico"))
            }
            linux {
                iconFile.set(project.file("icons/jarful.png"))
            }
        }
    }
}

// Compose 1.10.3 writes UTF-8 @argfiles, but the Windows JDK launcher reads
// them in the system code page. Pass jlink's short argument list directly so
// Japanese user-profile paths reach the tool without an encoding round trip.
if (System.getProperty("os.name").startsWith("Windows")) {
    tasks.withType<AbstractJLinkTask>().configureEach {
        val runtimeTask = this
        val process = providers
        actions.clear()
        doLast {
            val destination = runtimeTask.destinationDir.get().asFile
            check(destination.toPath().startsWith(layout.buildDirectory.get().asFile.toPath())) {
                "Runtime output must stay inside the desktop build directory"
            }
            destination.deleteRecursively()
            val selectedModules = if (runtimeTask.includeAllModules.getOrElse(false)) {
                "ALL-MODULE-PATH"
            } else {
                runtimeTask.modules.get().joinToString(",")
            }
            process.exec {
                executable = file("${runtimeTask.javaHome.get()}/bin/jlink.exe").absolutePath
                args("--add-modules", selectedModules,
                    "--strip-debug", "--no-header-files", "--no-man-pages",
                    "--strip-native-commands", "--output", destination.absolutePath)
                args(runtimeTask.freeArgs.getOrElse(emptyList()))
            }.result.get().assertNormalExitValue()
        }
    }
}
