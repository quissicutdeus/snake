plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

// compileOnly: the plugins themselves come from the main build (its root
// build.gradle.kts declares them), so every module runs the same copy.
dependencies {
    compileOnly(libs.android.gradle.api)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("quality") {
            id = "sniddle.quality"
            implementationClass = "SniddleQualityPlugin"
        }
    }
}
