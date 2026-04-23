import com.android.build.api.dsl.ApplicationExtension
import com.project.chirp.convention.configureKotlinAndroid
import com.project.chirp.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Includes the Application module-specific configuration to keep plugins modular and flexible.
 *
 * ## Strategy / Decisions
 * - **Modular Configuration:** Extracts project-specific Android configuration (SDK levels, packaging, build types) into a single reusable plugin to avoid bloating module-level Gradle files.
 * - **Automatic Plugin Application:** Applies `com.android.application` under the hood. The moment a module applies this convention, it is automatically recognized as an Android application.
 * - **Dynamic Versioning:** Pulls dynamic values (SDK versions, version codes) directly from the Version Catalog (`libs`) rather than hardcoding them, ensuring a single source of truth.
 *
 * ## How It Works
 * 1. Overrides the `apply` function from the `Plugin<Project>` interface.
 * 2. Uses the `pluginManager` to apply the underlying `com.android.application` plugin.
 * 3. Accesses the `ApplicationExtension` via the module's extensions block.
 * 4. Resolves version catalog string values, casting them to integers for `targetSdk` and `versionCode`.
 * 5. Calls the shared `configureKotlinAndroid()` utility to apply baseline SDK and Java version settings.
 * 6. Configures application-specific build types (e.g., release minification).
 *
 * ## Alternatives / Why Not
 * - **Why not put all Android config (like compileSdk) directly in here?** Shared properties like `compileSdk` and `minSdk` are also required by KMP library modules. Placing them strictly inside the `ApplicationExtension` would cause code duplication across different convention plugins.
 *
 * Technical Details: Relies on `com.android.build.api.dsl.ApplicationExtension`. Version catalog references require `.get()` and explicit string-to-int parsing.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                namespace = "com.project.chirp"

                defaultConfig {
                    applicationId = libs.findVersion("projectApplicationId").get().toString()
                    targetSdk = libs.findVersion("projectTargetSdkVersion").get().toString().toInt()
                    versionCode = libs.findVersion("projectVersionCode").get().toString().toInt()
                    versionName = libs.findVersion("projectVersionName").get().toString()
                }
                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
                // make it true for release builds to shrink the APK size
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = false
                    }
                }

                configureKotlinAndroid(this)
            }
        }
    }
}
