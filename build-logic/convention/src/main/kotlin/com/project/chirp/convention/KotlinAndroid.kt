package com.project.chirp.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Centralizes standard Android SDK limits, Java compatibility, and Kotlin compiler settings across the project, it can be used for both Application and Library projects.
 *
 * ## Strategy / Decisions
 * - **Unified Configuration:** Compiler options are highly error-prone if mixed (e.g., compiling some modules in JVM 11 and others in JVM 17). Centralizing this logic enforces a single source of truth across all modules.
 * - **API Backwards Compatibility:** Explicitly enables `coreLibraryDesugaring`. This guarantees that modern Java APIs (such as the `java.time` API introduced in API 26) can be safely utilized on older devices down to API 24.
 *
 * ## How It Works
 * 1. Accepts a generic `CommonExtension` parameter from the applying module because it can be used for both Application and Library projects.
 * 2. Applies `compileSdk` and `minSdk` retrieved from the Version Catalog.
 * 3. Enforces Java 17 compatibility for both source and target compilation.
 * 4. Automatically injects the `coreLibraryDesugaring` dependency directly into the module's dependency block.
 * 5. Calls `configureKotlin()` to align the Kotlin compiler's JVM target to JVM 17 and attach free compiler arguments (e.g., enabling experimental APIs).
 *
 * ## Alternatives / Why Not
 * - **Relying on `ApplicationExtension`:** Rejected. If this function required an `ApplicationExtension`, it would immediately break when applied to Kotlin Multiplatform library modules (which rely on `LibraryExtension`).
 * Injecting `CommonExtension` ensures the logic remains perfectly reusable across completely distinct target types.
 *
 * ## Technical Details
 * - Resolves the desugaring library dynamically via the Version Catalog using the `coreLibraryDesugaring` configuration keyword rather than standard `implementation`.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>
) {
    with(commonExtension) {
        compileSdk = libs.findVersion("projectCompileSdkVersion").get().toString().toInt()

        defaultConfig.minSdk = libs.findVersion("projectMinSdkVersion").get().toString().toInt()

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        configureKotlin()

        dependencies {
            "coreLibraryDesugaring"(libs.findLibrary("android-desugarJdkLibs").get())
        }
    }
}

/**
 * Configures base Kotlin compiler parameters.
 *
 * ## Strategy / Decisions
 * - **Standalone Kotlin Config:** Maintained as a separate function because plain Kotlin library modules (which lack Android-specific blocks) might still need strict JVM target alignment and experimental API flags.
 */
internal fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)

            freeCompilerArgs.add(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
        }
    }
}
