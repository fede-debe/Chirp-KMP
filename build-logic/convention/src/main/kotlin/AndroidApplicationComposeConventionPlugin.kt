import com.android.build.api.dsl.ApplicationExtension
import com.project.chirp.convention.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Gradle convention plugin that configures an Android application module to use Jetpack Compose.
 *
 * ## Strategy / Decisions
 * - **Separation of Concerns:** Maintains Compose-specific logic in a separate plugin from pure Android application logic. This allows modules to opt-in to Compose only when needed.
 * - **Plugin Composition:** Automatically applies the base custom Android Application convention plugin. This guarantees that any module needing Compose automatically inherits the project's standard Android configuration without requiring the developer to apply both manually.
 *
 * ## How It Works
 * 1. Automatically applies the base custom Android application convention plugin (e.g., `com.project.convention.android.application`).
 * 2. Extracts the standard Android `ApplicationExtension` from the project via `extensions.getByType()`.
 * 3. Delegates the actual Compose configuration to the internal `configureAndroidCompose` utility function.
 *
 * ## Alternatives / Why Not
 * - **Applying `com.android.application` directly:** Rejected. Instead of declaring the raw Android application plugin, we reference our *custom* convention plugin. If we used the raw plugin, we would bypass all the shared configuration previously defined for Android application modules.
 *
 * Technical Details:
 * - Applies to: `Project`
 * - Target Extension: `ApplicationExtension`
 */
class AndroidApplicationComposeConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.project.convention.android.application")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidCompose(extension)
        }
    }
}
