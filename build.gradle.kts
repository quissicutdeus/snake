// Root build. Plugins are declared here (not applied) so every module uses the
// same versions. AGP 9 has built-in Kotlin support; declaring kotlin-jvm here
// also pins the Kotlin version that AGP uses for the app module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
