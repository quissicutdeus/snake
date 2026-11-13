// Kotlin Multiplatform game logic: no Android or UI dependencies, so it can be
// unit tested on any machine and shared by the Android app (the jvm target) and
// an iOS port (the ios targets).
//   src/commonMain   the game itself: everything but the code below
//   src/jvmMain      what each platform does its own way (see the expect
//   src/iosMain        declarations in commonMain)
//   src/commonTest   tests run on every target
//   src/jvmTest      the Java2D previews, sound previews, balance report and
//                    benchmark (desktop tools, so JVM only)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lint) // Android Lint for a plain Kotlin module
    alias(libs.plugins.detekt)
}

kotlin {
    // Built with JDK 21, but emits Java 17 bytecode like the Android app, so the
    // app can use (and inline) game code.
    jvmToolchain(21)
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iPhones and iPads, and the simulator on Apple-silicon Macs.
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        // Compiler checks: every warning (plus the extra K2 checks, e.g. unused
        // variables and redundant code) fails the build, on every target.
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    // detekt's defaults are src/main and src/test, which don't exist here:
    // without this it would check nothing and pass. Every source set instead.
    source.setFrom("src")
}

lint {
    warningsAsErrors = true
    abortOnError = true
    checkAllWarnings = true
    checkTestSources = true
    // Online "newer version available" checks: see app/lint.xml.
    disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// CI and the README run the game tests as `:game:test`: on the JVM, like the
// Android app. (The iOS tests run on a Mac: see iosSimulatorArm64Test.)
tasks.register("test") {
    group = "verification"
    description = "Runs the game tests on the JVM (jvmTest)."
    dependsOn("jvmTest")
}

// Kotlin/Native can compile the iOS code anywhere, but only a Mac can link it
// and run its tests; elsewhere those tasks are skipped and the build still
// passes. Asking for them by name off a Mac is a mistake (a CI job on the wrong
// runner), so it fails instead of passing with nothing tested.
val iosOnlyOnMac = Regex("""(^|:)(ios\w*Test|ios\w*Binaries|link\w*Ios\w*)$""")
val askedForIos = gradle.startParameter.taskNames.filter { iosOnlyOnMac.containsMatchIn(it) }
if (askedForIos.isNotEmpty() && !System.getProperty("os.name").startsWith("Mac")) {
    throw GradleException(
        "$askedForIos link or run iOS code, which needs a Mac. " +
            "Here, :game:compileTestKotlinIosArm64 checks that the iOS code compiles.",
    )
}

// The previews, balance report and benchmark below run from the JVM test code,
// on the same JDK 21 toolchain the code is built with.
val jvmTestCompilation = kotlin.jvm().compilations["test"]
val jvmTestClasspath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
val jdk21 = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }

// Renders the real game art to PNG files with Java2D (no phone needed):
//   ./gradlew :game:renderPreviews   ->  game/build/previews/*.png
tasks.register<JavaExec>("renderPreviews") {
    group = "verification"
    description = "Renders PNG previews of the game art into build/previews."
    classpath = jvmTestClasspath
    javaLauncher.set(jdk21)
    mainClass.set("com.quissicutdeus.sniddle.game.preview.RenderPreviewsKt")
    args(
        layout.buildDirectory
            .dir("previews")
            .get()
            .asFile.absolutePath,
    )
    jvmArgs("-Djava.awt.headless=true")
}

// Writes every sound effect as a WAV file, to listen to on a computer:
//   ./gradlew :game:soundPreviews   ->  game/build/sounds/*.wav
tasks.register<JavaExec>("soundPreviews") {
    group = "verification"
    description = "Writes the synthesized sound effects as WAV files into build/sounds."
    classpath = jvmTestClasspath
    javaLauncher.set(jdk21)
    mainClass.set("com.quissicutdeus.sniddle.game.preview.SoundPreviewsKt")
    args(
        layout.buildDirectory
            .dir("sounds")
            .get()
            .asFile.absolutePath,
    )
}

// Plays a few games with a bot player and prints survival time and size mix:
//   ./gradlew :game:balanceReport
tasks.register<JavaExec>("balanceReport") {
    group = "verification"
    description = "Prints how bot-played games go with the current GameConfig."
    classpath = jvmTestClasspath
    javaLauncher.set(jdk21)
    mainClass.set("com.quissicutdeus.sniddle.game.preview.BalanceReportKt")
}

// Times the simulation step and the renderer's own work, and prints a checksum
// of the final world (unchanged by a pure optimisation):
//   ./gradlew :game:benchmark
tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Times GameSim.step() and SceneRenderer.draw() on the JVM."
    classpath = jvmTestClasspath
    javaLauncher.set(jdk21)
    mainClass.set("com.quissicutdeus.sniddle.game.preview.BenchmarkKt")
}
