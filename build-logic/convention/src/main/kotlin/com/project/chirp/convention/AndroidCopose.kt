package com.project.chirp.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configures Jetpack Compose specifically for the Android side of the project.
 *
 * ## Strategy / Decisions
 * - **Module Agnosticism:** Operates on the `CommonExtension` interface rather than a specific application extension. This allows the configuration to be reused across both Android Application and Android Library modules.
 * - **BOM Dependency Management:** Utilizes a Bill of Materials (BOM) to ensure all Compose libraries use compatible versions without explicitly stating versions for each artifact.
 * - **Compose Multiplatform Compatibility:** Intentionally omits the standard Android Compose Gradle plugin. Because this is a Compose Multiplatform (CMP) project, the special JetBrains Compose Gradle plugin must be handled at a different level.
 *
 * ## How It Works
 * 1. Enables the Compose build feature via `buildFeatures.compose = true`.
 * 2. Retrieves the Compose BOM (`androidx-compose-bom`) from the Version Catalog.
 * 3. Applies the BOM as a `platform` dependency to both `implementation` and `androidTestImplementation`.
 * 4. Retrieves and applies `ui-tooling` and `ui-tooling-preview` dependencies as `debugImplementation` to enable Compose Previews specifically on the Android side (helpful if creating Android-specific UI components outside common code).
 *
 * ## Alternatives / Why Not
 * - **Applying Compose Compiler Plugin Here:** Rejected at this level. Applying the JetBrains Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`) makes more sense at the root plugin level/helper function to ensure consistency across the CMP project,
 * rather than locking it solely to Android.
 *
 * Technical Details:
 * - Requires a Version Catalog named `libs` containing `androidx-compose-bom`, `androidx-compose-ui-tooling-preview`, and `androidx-compose-ui-tooling`.
 *
 * @param commonExtension The base Android extension (either Application or Library) to apply the Compose features to.
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>
) {
    with(commonExtension) {
        buildFeatures {
            compose = true
        }

        dependencies {
            val bom = libs.findLibrary("androidx-compose-bom").get()
            "implementation"(platform(bom))
            "testImplementation"(platform(bom))
            "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
