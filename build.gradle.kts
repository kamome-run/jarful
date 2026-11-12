plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

require(JavaVersion.current() == JavaVersion.VERSION_25) {
    "Jarful builds require JDK 25. Set JAVA_HOME to a JDK 25 installation."
}

subprojects {
    // The Gradle process uses the native charset for Windows launcher argfiles.
    // Application source files and test data continue to use UTF-8.
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    tasks.withType<Test>().configureEach {
        defaultCharacterEncoding = "UTF-8"
    }
}
