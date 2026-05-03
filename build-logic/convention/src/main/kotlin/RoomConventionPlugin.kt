import androidx.room.gradle.RoomExtension
import com.project.chirp.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for configuring Room database and its required KSP code generation across Kotlin Multiplatform targets.
 *
 * ## Strategy / Decisions
 * - **KSP Integration:** Room relies heavily on annotations and code generation. We integrate Kotlin Symbol Processing (KSP) (`com.google.devtools.ksp`) alongside the Room Gradle plugin (`androidx.room`) to facilitate this multiplatform code generation.
 * - **Transitive Dependency Exposure (`api` vs `implementation`):** The Room runtime (`androidx.room:room-runtime`) and bundled SQLite libraries (`sqlite-bundled`) are deliberately added using `api` (e.g., `commonMainApi`) instead of `implementation`. This ensures that any module depending on the database module automatically inherits visibility to Room's classes.
 * - **Schema Export:** The Room extension is configured to export generated database schemas to a localized `schemas` directory (`projectDir/schemas`) within the applied module for structured version tracking.
 * - **Target-Specific KSP Configuration:** A generic `ksp(...)` dependency declaration is deprecated in modern Kotlin Multiplatform projects. To avoid warnings and ensure correct compilation, KSP processor dependencies are explicitly attached to specific targets (e.g., `kspAndroid`, `kspIosSimulatorArm64`, `kspIosArm64`, `kspIosX64`).
 *
 * ## How It Works
 * 1. Resolves and applies both the KSP and Room Gradle plugins via the `pluginManager`.
 * 2. Accesses the `RoomExtension` to configure the `schemaDirectory` output path.
 * 3. Injects the required Room runtime and Kotlin multiplatform SQLite bundle as `api` dependencies to the applying module.
 * 4. Injects the Room compiler (`androidx.room:room-compiler`) using explicit, target-specific KSP dependency configurations for Android and all defined iOS CPU architectures.
 *
 * ## Alternatives / Why Not
 * - **Why not use `implementation` for Room dependencies?** If `implementation` were used, consumer modules depending on our central database module would fail to compile due to missing Room class visibility. `api` safely bridges this multi-module boundary.
 * - **Why not use a blanket `ksp(...)` dependency?** In Kotlin Multiplatform, a generic KSP configuration assumes a single target and triggers deprecation warnings. Specifying target-mapped configurations accurately routes the symbol processor to the exact native and JVM compilations.
 *
 * Technical Details:
 * - Build Environment: Requires `androidx.room:room-gradle-plugin` as a `compileOnly` dependency in the convention module's own `build.gradle.kts` to expose the `RoomExtension` class.
 * - Classpath requirement: Relies on the KSP and Room plugins being declared in the root project's build script with `apply false` to populate the plugin classpath.
 */
class RoomConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("androidx.room")
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "commonMainApi"(libs.findLibrary("androidx-room-runtime").get())
                "commonMainApi"(libs.findLibrary("sqlite-bundled").get())
                "kspAndroid"(libs.findLibrary("androidx-room-compiler").get())
                "kspIosSimulatorArm64"(libs.findLibrary("androidx-room-compiler").get())
                "kspIosArm64"(libs.findLibrary("androidx-room-compiler").get())
                "kspIosX64"(libs.findLibrary("androidx-room-compiler").get())
            }
        }
    }
}
