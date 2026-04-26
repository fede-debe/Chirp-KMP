package com.project.chirp.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures the various iOS CPU architecture targets for a Kotlin multi-platform module.
 *
 * ## Strategy / Decisions
 * iOS requires multiple distinct target definitions depending on the CPU architecture
 * (e.g., simulators vs. physical devices). Extracting this block into a single helper function
 * prevents massive boilerplate duplication across shared library modules (like core-data, core-designsystem)
 * and the main application.
 *
 * ## How It Works
 * 1. Accesses the `KotlinMultiplatformExtension` via the Project extensions.
 * 2. Applies the respective iOS targets (e.g., arm64, x64, simulator arm64) inside the extension block.
 *
 * Technical Details:
 * - Extension Configured: `KotlinMultiplatformExtension`.
 */
internal fun Project.configureIosTargets() {
    extensions.configure<KotlinMultiplatformExtension> {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }
}
