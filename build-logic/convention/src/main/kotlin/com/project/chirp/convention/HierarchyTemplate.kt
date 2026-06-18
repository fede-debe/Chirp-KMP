@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package com.project.chirp.convention

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

/**
 * Defines the custom Kotlin Multiplatform source set hierarchy template for the project.
 * This structure explicitly groups targets to maximize code sharing and organize module dependencies.
 *
 * ## Strategy / Decisions
 * We utilize the `KotlinHierarchyTemplate` API to define our source set groupings centrally at the root level (in the convention plugin) rather than configuring them per module. A custom `jvmCommon` group was introduced to specifically bundle the `androidTarget` and the desktop target (`jvm`). This allows us to share Java/JVM-specific library code (like OkHttp classes) between Android and Desktop seamlessly, bypassing iOS which cannot access JVM classes.
 *
 * ## How It Works
 * 1. Opts into the experimental `KotlinHierarchyTemplate` API.
 * 2. Defines `sourceSetTree` for both main (`KotlinSourceSetTree.main`) and test (`KotlinSourceSetTree.test`) variants so Gradle auto-generates test folders for custom groups.
 * 3. Inside the `common` wrapper, `withCompilations(true)` is applied to include custom executable bundles.
 * 4. A `mobile` group is created containing the `androidTarget` and an `ios` subgroup (which properly bundles the various iOS CPU architectures).
 * 5. A `jvmCommon` group is created combining `withAndroidTarget()` and `withJvm()`.
 * 6. The template is finalized and then passed to the KMP extension using `applyHierarchyTemplate()` in configuring modules (e.g., `core-presentation` and `app`).
 *
 * ## Alternatives / Why Not
 * The previous approach involved manually defining source sets and dependencies (e.g., manually linking `iosMain` or `mobileMain`) directly in individual `build.gradle.kts` files. This was rejected because it was deemed "quite hacky," difficult to maintain, required redefining default targets, and caused Gradle sync failures when source sets already existed.
 *
 * Technical Details: Relies on experimental Kotlin Multiplatform APIs. The resulting template forms a strictly structured tree where child leaves inherit dependencies from their parent groups.
 */
private val hierarchyTemplate = KotlinHierarchyTemplate {
    withSourceSetTree(
        KotlinSourceSetTree.main,
        KotlinSourceSetTree.test,
    )

    common {
        withCompilations { true }

        /*
            AGP 9.0 Prep:
            jvmCommon was removed because Android belonged to both mobile and jvmCommon,
            creating overlapping source-set paths. With the new AGP KMP plugin, this ambiguity
            forces actuals to exist in all intermediate source sets, leading to compilation
            errors. Removing jvmCommon globally avoids this conflict, while modules that truly
            need Android + Desktop JVM sharing can opt in explicitly using dependsOn().
         */
        group("mobile") {
            withAndroidTarget()
            group("ios") {
                withIos()
            }
        }

        /*
            Android no longer automatically depends on intermediate source sets such as
            mobileMain or jvmCommonMain, and jvmCommonMain has been removed from the global
            hierarchy. As of Gradle 9.0, any module that needs to share code between Android
            and Desktop JVM must explicitly configure this dependency, for example by
            making androidMain depend on jvmCommonMain.
        */
        group("native") {
            withNative()

            group("apple") {
                withApple()

                group("ios") {
                    withIos()
                }

                group("macos") {
                    withMacos()
                }
            }
        }
    }
}

fun KotlinMultiplatformExtension.applyHierarchyTemplate() {
    applyHierarchyTemplate(hierarchyTemplate)
}
