package com.project.chirp.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures the Android-specific target settings for a Kotlin multi-platform module.
 *
 * ## Strategy / Decisions
 * Extracted into a standalone internal helper function to easily apply standard Android target logic
 * across both application and library CMP modules. This provides a single source of truth
 * for base Android KMP configuration and prevents repetition.
 *
 * ## How It Works
 * 1. Accesses the `KotlinMultiplatformExtension` via the Project extensions `configure` block.
 * 2. Initializes the `androidTarget` block.
 * 3. Sets the compiler options to target JVM 17.
 *
 * Technical Details:
 * - Extension Configured: `KotlinMultiplatformExtension`.
 * - Compiler Target: JVM 17.
 */
/**
 * Configures Android target for APPLICATION modules using com.android.application plugin.
 * Uses the traditional androidTarget {} DSL.
 */
internal fun Project.configureAndroidTarget() {
    extensions.configure<KotlinMultiplatformExtension> {
        androidTarget {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }
}

/**
 * Configures Android target for LIBRARY modules using com.android.kotlin.multiplatform.library plugin.
 * Android library settings (namespace, compileSdk, minSdk) must be configured in each module's
 * build.gradle.kts using kotlin { androidLibrary { ... } }
 */
internal fun Project.configureAndroidLibraryTarget() {
    dependencies {
        "coreLibraryDesugaring"(libs.findLibrary("android-desugarJdkLibs").get())
    }
}
