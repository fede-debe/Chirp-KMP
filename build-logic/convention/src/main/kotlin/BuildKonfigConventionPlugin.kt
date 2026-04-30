import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import com.project.chirp.convention.pathToPackageName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.configure

/**
 * Configures the BuildConfig Gradle plugin to generate module-specific build constants, primarily used for safely injecting sensitive data like API keys into the app.
 *
 * ## Strategy / Decisions
 * - **Security via `local.properties`:** Sensitive credentials (like the backend API key) are read exclusively from `local.properties`. This guarantees that private keys are never committed to version control (e.g., GitHub), keeping the repository secure.
 * - **Fail-Fast Developer Experience:** If the API key is missing from `local.properties` (a common occurrence when a fresh repo is cloned), the plugin explicitly throws an `IllegalStateException` during Gradle sync. This immediately informs the developer what property is missing and where to put it, preventing silent runtime crashes later.
 * - **Dynamic Package Naming:** The generated `BuildConfig` Kotlin object uses a dynamically resolved package name via `target.pathToPackageName()`. This maps the generated file directly to the applying module's namespace (e.g., `core.data` vs `chat.data`), preventing package name collisions across a multi-module architecture.
 * - **Environment Scaling:** While currently used for an API key, this strategy is chosen because it easily scales to swapping base URLs (e.g., routing debug builds to UAT/Testing servers and release builds to Production).
 *
 * ## How It Works
 * 1. Applies the `buildconfig` Gradle plugin to the target module.
 * 2. Accesses the root project's file system via `rootProject.providers` to read `local.properties`.
 * 3. Retrieves the specific property named `API_KEY`.
 * 4. Checks the property for nullability; if null, halts the build with an `IllegalStateException`.
 * 5. Uses the plugin's `buildConfig` extension to define a new `String` field (`API_KEY`) and assigns the local property value to it.
 * 6. Sets the `packageName` attribute dynamically based on the current module's path.
 * 7. On build, generates a Kotlin singleton (`BuildConfig`) inside the `build/generated` directory containing the constants.
 *
 * ## Alternatives / Why Not
 * - **Hardcoding in source code / `gradle.properties`:** Rejected because it exposes sensitive data to anyone with repository access.
 * - **Full security obfuscation:** This setup was chosen purely for version control hygiene, not extreme reverse-engineering prevention. While a backend restriction is the ultimate alternative, this client-side injection is still required to make the initial secure handshake.
 *
 * Technical Details:
 * - **Reverse Engineering:** This implementation does not encrypt the API key in the final binary. It prevents version control leaks, but a determined user could still reverse-engineer the compiled app to extract the key.
 * - **Module Target:** This convention is explicitly designed to be applied only to specific modules that require the key (e.g., `core:data` and `chat:data`), rather than polluting the entire project hierarchy.
 * - **Sync Dependency:** The `BuildConfig` object will not be recognizable by the IDE until an initial build or a successful Gradle sync completes and the file is written to the `build/` directory.
 */
class BuildKonfigConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.codingfeline.buildkonfig")
            }

            extensions.configure<BuildKonfigExtension> {
                packageName = target.pathToPackageName()
                defaultConfigs {
                    val apiKey = gradleLocalProperties(rootDir, rootProject.providers)
                        .getProperty("API_KEY")
                        ?: "missing-api-key"
                    buildConfigField(FieldSpec.Type.STRING, "API_KEY", apiKey)
                }
            }
        }
    }
}
