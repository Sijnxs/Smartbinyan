pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // ✅ Required for hannesa2 MQTT fork
        maven { url = uri("https://jitpack.io") }
        // ✅ Optional: Legacy Paho releases
        maven { url = uri("https://repo.eclipse.org/content/repositories/paho-releases/") }
    }
}

dependencyResolutionManagement {
    // ✅ Disallow declaring repos in other Gradle files
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ hannesa2 MQTT (JitPack)
        maven { url = uri("https://jitpack.io") }
        // ✅ Eclipse Paho (optional)
        maven { url = uri("https://repo.eclipse.org/content/repositories/paho-releases/") }
    }
}

rootProject.name = "Smartbinyan"
include(":app")
