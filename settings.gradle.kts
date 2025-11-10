pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ===================================
        // ¡AQUÍ ES DONDE SÍ DEBE ESTAR JITPACK!
        // ¡Esta es la sintaxis .kts correcta!
        maven { url = uri("https://jitpack.io") }
        // ===================================
    }
}

rootProject.name = "RiegoSostenible"
include(":app")