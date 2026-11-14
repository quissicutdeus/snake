import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.Lint
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * The checks every Sniddle module gets. Apply it last in a module's `plugins`
 * block (after the Kotlin and Android plugins, whose extensions it configures):
 *
 * - Kotlin on the JDK 21 toolchain, with every compiler warning (plus the
 *   extra K2 checks: unused variables, redundant code...) failing the build,
 *   on every target.
 * - detekt with config/detekt/detekt.yml, over the whole src directory.
 *   detekt's defaults are src/main and src/test, which a multiplatform module
 *   doesn't have: it would check nothing and pass.
 * - Android Lint, every check on and warnings failing the build, for every
 *   lint run the module has. A multiplatform module with an Android target
 *   has two: the module's own `lint {}` covers only the JVM-side run, and the
 *   Android target's `kotlin { android { lint {} } }` its Android source sets.
 *   Missing the second let warnings in androidMain pass. Which checks are
 *   switched off stays in each module (lint.xml or `disable`).
 * - Asking off a Mac for a task that links or runs iOS code fails, rather than
 *   being skipped and passing with nothing tested.
 */
class SniddleQualityPlugin : Plugin<Project> {
    override fun apply(project: Project) =
        with(project) {
            val kotlin =
                extensions.findByType(KotlinBaseExtension::class.java)
                    ?: throw GradleException("$path: apply sniddle.quality after the Kotlin plugin")
            kotlin.jvmToolchain(JDK)
            tasks.withType(KotlinCompilationTask::class.java).configureEach {
                compilerOptions {
                    allWarningsAsErrors.set(true)
                    extraWarnings.set(true)
                }
            }

            pluginManager.apply("dev.detekt")
            extensions.configure(DetektExtension::class.java) {
                buildUponDefaultConfig.set(true)
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                source.setFrom("src")
            }

            val lints =
                listOfNotNull(
                    extensions.findByType(Lint::class.java),
                    extensions.findByType(CommonExtension::class.java)?.lint,
                )
            if (lints.isEmpty()) {
                throw GradleException("$path: apply sniddle.quality after an Android or Android Lint plugin")
            }
            lints.forEach { it.strict() }
            pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
                extensions
                    .getByType(KotlinMultiplatformExtension::class.java)
                    .targets
                    .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                    .configureEach { lint.strict() }
            }

            failIosTasksOffAMac()
        }

    private fun Lint.strict() {
        warningsAsErrors = true
        abortOnError = true
        checkAllWarnings = true
        checkTestSources = true
    }

    private fun Project.failIosTasksOffAMac() {
        val asked = gradle.startParameter.taskNames.filter { IOS_ONLY_ON_MAC.containsMatchIn(it) }
        if (asked.isNotEmpty() && !System.getProperty("os.name").startsWith("Mac")) {
            throw GradleException(
                "$asked link or run iOS code, which needs a Mac. " +
                    "Here, compileKotlinIosArm64 and compileTestKotlinIosArm64 check that the iOS code compiles.",
            )
        }
    }

    private companion object {
        const val JDK = 21

        // Kotlin/Native compiles iOS code anywhere, but only a Mac links it,
        // runs its tests or hands Xcode a framework.
        val IOS_ONLY_ON_MAC = Regex("""(^|:)(ios\w*Test|ios\w*Binaries|link\w*Ios\w*|embedAndSign\w*)$""")
    }
}
