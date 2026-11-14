// The build's own Gradle code: convention plugins every module applies, so a
// new module gets the same compiler checks, detekt, Android Lint and Mac-only
// guard without copying them. Uses the main build's version catalog.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
