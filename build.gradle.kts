// Root build. Plugins are declared here (not applied) so every module uses the
// same versions. AGP 9 has built-in Kotlin support; declaring kotlin-jvm here
// also pins the Kotlin version that AGP uses for the app module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless)
}

// Formatting: ktlint (rules configured in .editorconfig).
//   ./gradlew spotlessCheck   fails on badly formatted code
//   ./gradlew spotlessApply   fixes it
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

// Every check in one go: ./gradlew lintAll
tasks.register("lintAll") {
    group = "verification"
    description = "Runs every linter: ktlint (Spotless), detekt, Android Lint and the dead-code report."
    dependsOn("spotlessCheck", ":game:detekt", ":app:detekt", ":game:lint", ":app:lintDebug", ":app:deadCode")
}
