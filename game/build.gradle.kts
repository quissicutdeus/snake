// Pure Kotlin/JVM game logic: no Android or UI dependencies, so it can be unit
// tested on any machine and reused by a future iOS port.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
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
    mainClass.set("com.snakearena.game.preview.RenderPreviewsKt")
    args(layout.buildDirectory.dir("previews").get().asFile.absolutePath)
    jvmArgs("-Djava.awt.headless=true")
}

// Plays a few games with a bot player and prints survival time and size mix:
//   ./gradlew :game:balanceReport
tasks.register<JavaExec>("balanceReport") {
    group = "verification"
    description = "Prints how bot-played games go with the current GameConfig."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.snakearena.game.preview.BalanceReportKt")
}
