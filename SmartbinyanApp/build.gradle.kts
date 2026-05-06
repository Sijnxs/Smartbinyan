// Root-level build.gradle.kts

plugins {
    kotlin("android") version "1.9.0" apply false
    id("com.android.application") version "8.3.1" apply false
    id("com.android.library") version "8.3.1" apply false
    // ✅ Updated Google Services plugin version
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// ✅ No repository blocks here (repositories are handled in settings.gradle.kts)

buildscript {
    dependencies {
        // ✅ Ensure Kotlin Gradle plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")

        // ✅ Added / Updated Firebase Google Services Gradle classpath
        classpath("com.google.gms:google-services:4.4.2")
    }
}

// ✅ Optional cleanup task
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
