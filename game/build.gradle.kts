// Pure Kotlin/JVM game logic: no Android or UI dependencies, so it can be unit
// tested on any machine and reused by a future iOS port.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint) // Android Lint for a plain Kotlin module
    alias(libs.plugins.detekt)
}

// Built with JDK 21, but emits Java 17 bytecode like the Android app, so the
// app can use (and inline) game code.
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // Compiler checks: every warning (plus the extra K2 checks, e.g. unused
        // variables and redundant code) fails the build.
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

lint {
    warningsAsErrors = true
    abortOnError = true
    checkAllWarnings = true
    checkTestSources = true
    // Online "newer version available" checks: see app/lint.xml.
    disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Renders the real game art to PNG files with Java2D (no phone needed):
//   ./gradlew :game:renderPreviews   ->  game/build/previews/*.png
tasks.register<JavaExec>("renderPreviews") {
    group = "verification"
    description = "Renders PNG previews of the game art into build/previews."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.quissicutdeus.snake.game.preview.RenderPreviewsKt")
    args(
        layout.buildDirectory
            .dir("previews")
            .get()
            .asFile.absolutePath,
    )
    jvmArgs("-Djava.awt.headless=true")
}

// Plays a few games with a bot player and prints survival time and size mix:
//   ./gradlew :game:balanceReport
tasks.register<JavaExec>("balanceReport") {
    group = "verification"
    description = "Prints how bot-played games go with the current GameConfig."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.quissicutdeus.snake.game.preview.BalanceReportKt")
}

// Times the simulation step and the renderer's own work, and prints a checksum
// of the final world (unchanged by a pure optimisation):
//   ./gradlew :game:benchmark
tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Times GameSim.step() and SceneRenderer.draw() on the JVM."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.quissicutdeus.snake.game.preview.BenchmarkKt")
}
