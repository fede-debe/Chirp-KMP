import com.android.build.api.dsl.LibraryExtension
import com.project.chirp.convention.configureKotlinAndroid
import com.project.chirp.convention.configureKotlinMultiplatform
import com.project.chirp.convention.libs
import com.project.chirp.convention.pathToResourcePrefix
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * **Header:** Configures the baseline Kotlin Multiplatform (KMP) settings for all standard library modules (e.g., data, domain, design system).
 *
 * ## Strategy / Decisions
 * - **Separation of Concerns:** Applies generic KMP, Android, and Serialization plugins, but explicitly excludes Compose Multiplatform. Compose is kept in a separate convention plugin so non-UI modules (like domain/data) don't carry unnecessary UI dependencies.
 * - **Serialization Requirement:** Applies the Serialization plugin by default because it is globally required across the app for type-safe navigation routes (presentation) and JSON parsing (data).
 * - **Android Plugin Choice:** We use the standard `com.android.library` plugin instead of `com.android.multiplatform.library`. The standard library plugin exposes the `LibraryExtension`, which is strictly necessary to dynamically configure the Android namespace.
 * - **iOS Simulator Support:** Explicitly sets the experimental property `android.experimental.kmp.enableAndroidResources=true` because it is required to allow debug builds of the application to run successfully on the iOS simulator.
 *
 * ## How It Works
 * 1. Applies the core plugin manager dependencies (`com.android.library`, `org.jetbrains.kotlin.multiplatform`, `org.jetbrains.kotlin.plugin.serialization`).
 * 2. Configures the `LibraryExtension` to dynamically set the Android namespace and resource prefixes based on the module path.
 * 3. Configures common Kotlin Android settings.
 * 4. Injects foundational KMP library dependencies into `commonMain` (serialization) and `commonTest` (kotlin.test).
 *
 * ## Alternatives / Why Not
 * - **Rejected `com.android.multiplatform.library`:** Initially attempted, but it threw a conflict error when applied alongside `com.android.library`. The latter was retained because the multiplatform variant lacks the standard `LibraryExtension` needed for dynamic namespace configurations.
 *
 * Technical Details
 * - Constraints: Requires the serialization plugin (`org.jetbrains.kotlin.plugin.serialization`) to be defined with `apply false` in the root `build.gradle.kts` to be accessible on the convention plugin's classpath.
 */
class KmpLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            configureKotlinMultiplatform()

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)

                resourcePrefix = this@with.pathToResourcePrefix()

                // Required to make debug build of app run in iOS simulator
                experimentalProperties["android.experimental.kmp.enableAndroidResources"] = "true"
            }

            dependencies {
                "commonMainImplementation"(libs.findLibrary("kotlinx-serialization-json").get())
                "commonTestImplementation"(libs.findLibrary("kotlin-test").get())
            }
        }
    }
}
