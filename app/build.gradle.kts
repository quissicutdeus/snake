// Android app (Jetpack Compose). AGP 9 compiles Kotlin itself, so only the
// Compose compiler plugin is applied here.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.quissicutdeus.snake"
    // Newest stable platform (Android 17, minor release 2).
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 2
            }
    }

    defaultConfig {
        applicationId = "com.quissicutdeus.snake"
        // Android 16+: every Pixel from the Pixel 6 onwards.
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // A shared debug key, committed on purpose: every build, local or CI,
        // has the same signature, so a new APK installs over the old one and
        // keeps the saved high scores. Not secret and not for the Play Store.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Optimized build for playing at home (sideloaded). Swap in a real
            // upload key before ever publishing to the Play Store.
            signingConfig = signingConfigs.getByName("debug")
        }
        // Not for installing: R8 shrinks without optimizing so ./gradlew deadCode
        // can list code the app never reaches.
        create("deadCode") {
            initWith(getByName("release"))
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "deadcode-rules.pro")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Android Lint (+ Slack's Compose rules): every check on, warnings fail the build.
    lint {
        warningsAsErrors = true
        abortOnError = true
        checkAllWarnings = true
        checkTestSources = true
        checkDependencies = true
        lintConfig = file("lint.xml")
    }

    testOptions {
        unitTests {
            // Robolectric screenshot tests need the app's resources.
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Every compiler warning (plus the extra K2 checks) fails the build.
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(project(":game"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    lintChecks(libs.compose.lint.checks)

    // Screenshot tests that render the real screens on the JVM (no phone needed).
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Robolectric screenshots are written to app/build/screenshots.
tasks.withType<Test>().configureEach {
    systemProperty(
        "screenshots.dir",
        layout.buildDirectory
            .dir("screenshots")
            .get()
            .asFile.absolutePath,
    )
    // Robolectric downloads its Android jars at test time; use Google's Maven
    // Central mirror, which doesn't rate-limit these large files.
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
    // Robolectric's Android 16 runtime reaches into a JDK-internal class.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
}

// Dead-code report: everything R8 could remove from the app (minus compiler-
// generated bits and the allowlist in config/deadcode-allowlist.txt).
//   ./gradlew deadCode
val deadCodeUsage = layout.buildDirectory.file("outputs/mapping/deadCode/usage.txt")
val deadCodeAllowlist = rootProject.layout.projectDirectory.file("config/deadcode-allowlist.txt")
tasks.register("deadCode") {
    group = "verification"
    description = "Fails if the app contains code nothing uses (via R8's shrinker)."
    dependsOn("minifyDeadCodeWithR8")
    inputs.file(deadCodeAllowlist)
    val usageFile = deadCodeUsage
    val allowFile = deadCodeAllowlist
    doLast {
        val ours = "com.quissicutdeus.snake."
        // Compiler-generated members that are never "dead code" you could delete.
        val generated =
            listOf(
                "\$stable",
                "\$annotations",
                "\$default(",
                "access\$",
                "\$ENTRIES",
                "\$VALUES",
                "<clinit>",
                "DefaultConstructorMarker",
                " copy(",
                " equals(java.lang.Object)",
                " hashCode()",
                " toString()",
                " values()",
                " valueOf(java.lang.String)",
                " getEntries()",
                // No-argument constructors Kotlin adds when every parameter has a default,
                // and the helper class for interface default arguments.
                ":: public void <init>()",
                "\$DefaultImpls",
            )
        val generatedPattern =
            Regex(
                // componentN() of data classes, and the Companion field;
                " component\\d+\\(| Companion$|" +
                    // Kotlin `const val`s are copied into the code that uses them.
                    "static final (int|float|long|double|boolean|java\\.lang\\.String) [A-Z0-9_]+$",
            )
        val allow =
            allowFile.asFile
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
        var owner = ""
        val dead = mutableListOf<String>()
        for (line in usageFile.get().asFile.readLines()) {
            if (line.isBlank()) continue
            if (!line.startsWith(" ")) {
                owner = line.trim().removeSuffix(":")
                // A line without ':' is a whole class nobody uses.
                if (!line.trimEnd().endsWith(":")) dead += owner
                continue
            }
            dead += "$owner :: ${line.trim()}"
        }
        val found =
            dead
                .filter { it.startsWith(ours) }
                .filterNot { e ->
                    e.contains(".R$") || e.endsWith(".R") || e.contains("ComposableSingletons") ||
                        generated.any { e.contains(it) } ||
                        generatedPattern.containsMatchIn(e)
                }.filterNot { e -> allow.any { e.contains(it) } }
        if (found.isNotEmpty()) {
            throw GradleException(
                "Unused code found (remove it, or explain it in config/deadcode-allowlist.txt):\n" + found.joinToString("\n"),
            )
        }
        println("Dead-code check: nothing unused.")
    }
}
