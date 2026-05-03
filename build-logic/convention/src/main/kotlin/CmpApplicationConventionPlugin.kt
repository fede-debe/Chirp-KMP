import com.project.chirp.convention.configureAndroidTarget
import com.project.chirp.convention.configureIosTargets
import com.project.chirp.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configures a Compose Multi-platform (CMP) application module by applying required plugins and shared dependencies.
 *
 * ## Strategy / Decisions
 * Extracted from the primary `composeApp` module to centralize multi-platform configuration for the application.
 * We explicitly apply the Compose Compiler plugin here rather than relying on the Android application compose
 * convention plugin to apply it. This makes the CMP plugin fully self-contained; if we ever drop
 * the Android target, the multi-platform application build will not break. "Hot Reload" was intentionally
 * left out of this shared convention because it is currently only relevant for desktop targets.
 *
 * ## How It Works
 * 1. Uses the Plugin Manager to apply core plugins: Android Application Compose, Kotlin Multiplatform, Compose Multiplatform, and Compose Compiler.
 * 2. Injects the `compose-ui-tooling` debug implementation dependency globally to enable composable previews in shared code.
 * 3. Delegates target-specific setups by calling internal helper functions `configureAndroidTarget()` and `configureIosTargets()`.
 *
 * ## Alternatives / Why Not
 * - **Hot Reload Plugin:** Rejected for inclusion in the shared application convention since it is currently desktop-exclusive and doesn't belong in shared targets.
 * - **Target Declarations in App Module:** Rejected keeping target blocks inside `composeApp/build.gradle.kts` to maintain a lightweight application module and promote reusability across potential future CMP modules.
 *
 * Technical Details:
 * - Extension Configured: `Project` (via Plugin Manager).
 * - Sync Issues: Gradle may occasionally throw "Plugin Not Found" errors during initial convention plugin registration due to sync ordering issues. Re-syncing typically resolves this.
 */
class CmpApplicationConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.project.convention.android.library")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            configureAndroidTarget()
            configureIosTargets()

            dependencies {
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}
