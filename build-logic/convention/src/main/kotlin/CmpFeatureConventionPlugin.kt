import com.project.chirp.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A convention plugin specifically for feature-specific presentation layers (e.g., Auth, Chat).
 *
 * ## Strategy / Decisions
 * Feature modules require more than just UI components; they need navigation, dependency
 * injection (Koin), and Lifecycle management. This plugin centralizes those dependencies
 * to ensure all features follow the same architectural pattern.
 *
 * ## How It Works
 * 1. **Base Layer:** Applies the `CmpLibraryConventionPlugin` to get standard Compose support.
 * 2. **Cross-Module Linking:** Automatically adds dependencies on `core:presentation` and
 * `core:design-system` since every feature needs shared UI utilities and the design system.
 * 3. **Architectural Stack:** Adds Koin for DI, JetBrains Lifecycle for ViewModels, and
 * Compose Navigation for screen transitions.
 * 4. **Android Specifics:** Includes `AndroidMain` specific Koin dependencies for Android-specific
 * context handling and ViewModel injection.
 *
 * ## Technical Details
 * * **Koin BOM:** Uses the Koin Bill of Materials (BOM) to manage versions across both
 * `commonMain` and `androidMain` source sets, ensuring version compatibility.
 * * **SavedState:** Includes `saved-state` dependencies to allow ViewModels to retrieve
 * navigation arguments effectively.
 */
class CmpFeatureConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.project.convention.cmp.library")
            }

            dependencies {
                "commonMainImplementation"(project(":core:presentation"))
                "commonMainImplementation"(project(":core:designsystem"))

                "commonMainImplementation"(platform(libs.findLibrary("koin-bom").get()))
                "androidMainImplementation"(platform(libs.findLibrary("koin-bom").get()))

                "commonMainImplementation"(libs.findLibrary("koin-compose").get())
                "commonMainImplementation"(libs.findLibrary("koin-compose-viewmodel").get())

                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-runtime").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-viewmodel").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-lifecycle-viewmodel").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-lifecycle-compose").get())

                "commonMainImplementation"(libs.findLibrary("jetbrains-lifecycle-viewmodel-savedstate").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-savedstate").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-bundle").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-navigation").get())

                "androidMainImplementation"(libs.findLibrary("koin-android").get())
                "androidMainImplementation"(libs.findLibrary("koin-androidx-compose").get())
                "androidMainImplementation"(libs.findLibrary("koin-androidx-navigation").get())
                "androidMainImplementation"(libs.findLibrary("koin-core-viewmodel").get())
            }
        }
    }
}
