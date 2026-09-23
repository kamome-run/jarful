import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

compose.desktop {
    application {
        mainClass = "dev.jarful.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Jarful"
            packageVersion = "1.7.2"
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
