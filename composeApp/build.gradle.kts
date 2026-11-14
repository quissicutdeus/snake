plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.quissicutdeus.sniddle.shared"
        compileSdk = 37
        minSdk = 30
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
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
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

// Linking the framework or running iOS tests needs a Mac (Xcode does it there,
// through embedAndSignAppleFrameworkForXcode). Here, compileKotlinIosArm64 checks the iOS code compiles.
val iosOnlyOnMac = Regex("""(^|:)(ios\w*Test|ios\w*Binaries|link\w*Ios\w*|embedAndSign\w*)$""")
val askedForIos = gradle.startParameter.taskNames.filter { iosOnlyOnMac.containsMatchIn(it) }
if (askedForIos.isNotEmpty() && !System.getProperty("os.name").startsWith("Mac")) {
    throw GradleException(
        "$askedForIos link or run iOS code, which needs a Mac. " +
            "Here, :composeApp:compileKotlinIosArm64 checks that the iOS code compiles.",
    )
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src")
}

// The shared UI's Android lint, set up like the app's (app/lint.xml), with the same Compose checks.
lint {
    warningsAsErrors = true
    abortOnError = true
    checkAllWarnings = true
    checkTestSources = true
    lintConfig = rootProject.file("app/lint.xml")
}

dependencies {
    lintChecks(libs.compose.lint.checks)
}
