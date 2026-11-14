plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.compose)
    id("sniddle.quality") // compiler checks, detekt, lint, JDK 21 (build-logic)
}

kotlin {
    android {
        namespace = "com.quissicutdeus.sniddle.shared"
        // SDK levels: gradle/libs.versions.toml, the same as the app's.
        compileSdk {
            version =
                release(
                    libs.versions.androidCompileSdk
                        .get()
                        .toInt(),
                ) {
                    minorApiLevel =
                        libs.versions.androidCompileSdkMinor
                            .get()
                            .toInt()
                }
        }
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
        // Lint for the Android source sets (the module's `lint {}` below is
        // the JVM-side run): the app's settings, with the same Compose checks.
        lint {
            lintConfig = rootProject.file("app/lint.xml")
        }
        // The shared code's own tests run on the JVM: ./gradlew :composeApp:testAndroidHostTest
        withHostTestBuilder {}
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":game"))
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

// Lint for the JVM-side run (commonMain as plain Kotlin); the Android source
// sets have their own, in kotlin { android { lint } } above. Same settings.
lint {
    lintConfig = rootProject.file("app/lint.xml")
}

dependencies {
    lintChecks(libs.compose.lint.checks)
}
