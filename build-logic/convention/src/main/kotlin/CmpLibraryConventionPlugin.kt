import com.project.chirp.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A convention plugin that configures Compose Multiplatform (CMP) support for general KMP libraries.
 *
 * ## Strategy / Decisions
 * This plugin serves as a base for any module that requires Compose capabilities but isn't
 * necessarily a full "feature" (e.g., UI utility modules or design systems). It builds
 * upon the standard KMP library configuration to ensure consistency across the project.
 *
 * ## How It Works
 * 1. **Inheritance:** Applies the base `KmpLibraryConventionPlugin` first.
 * 2. **Plugin Integration:** Applies the JetBrains Compose compiler and the multiplatform plugin.
 * 3. **Shared UI Dependencies:** Automatically adds core Compose libraries (UI, Foundation, Material 3)
 * to `commonMain` so they don't have to be manually declared in every UI module.
 *
 * ## Alternatives / Why Not
 * * **Material Icons Extended:** The instructor explicitly decided **not** to include the
 * Extended Icons set by default. This is because it is a "quite large dependency" that
 * could unnecessarily bloat the application size. Instead, only the Core icons are provided.
 */
class CmpLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.project.convention.kmp.library")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }

            dependencies {
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-ui").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-foundation").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material3").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material-icons-core").get())
            }
        }
    }
}
