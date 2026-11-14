pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Google's mirror of Maven Central, only for Robolectric: its large
        // native-graphics jar is often rate-limited (HTTP 429) on Maven Central.
        maven("https://maven-central.storage-download.googleapis.com/maven2/") {
            content { includeGroup("org.robolectric") }
        }
        mavenCentral()
    }
}

rootProject.name = "Sniddle"

include(":game")
include(":composeApp")
include(":app")
