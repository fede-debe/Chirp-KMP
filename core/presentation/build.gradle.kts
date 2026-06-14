plugins {
    alias(libs.plugins.convention.cmp.library)
}

/**
 * Configures dependencies and custom source sets for the core presentation module.
 *
 * ## Strategy / Decisions
 * Implements a custom KMP source set named `mobileMain` to act as a bridge for the Moco Permissions library.
 * Moco is utilized to simplify complex native permission requests across platforms.
 *
 * ## How It Works
 * 1. Creates a new `mobileMain` source set using `by creating`.
 * 2. Applies the Moco permissions dependencies (`permissions`, `permissions-compose`, `permissions-notifications`) exclusively to `mobileMain`.
 * 3. Binds `mobileMain` to depend on `commonMain` so domain classes are accessible.
 * 4. Modifies the default `androidMain` to depend on `mobileMain`.
 * 5. Recreates the `iosMain` source set, iterates through the required iOS targets (iosArm64, iosX64, iosSimulatorArm64), and sets them to depend on the new `iosMain` which in turn depends on `mobileMain`.
 *
 * ## Alternatives / Why Not
 * - **Declaring Moco in `commonMain`:** Rejected because Moco does not support Desktop targets. Doing so would break compilation when a Desktop target is added later.
 * - **Declaring Moco separately in `androidMain` and `iosMain`:** Rejected because the mobile implementation for permission handling is completely identical. This approach would result in unnecessary code duplication.
 */
kotlin {
    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here

                implementation(projects.core.domain)

                implementation(libs.material3.adaptive)
                implementation(libs.jetbrains.lifecycle.compose)
                implementation(libs.bundles.koin.common)

                implementation(libs.components.resources)
            }
        }

        val mobileMain by creating {
            dependencies {
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.notifications)
            }
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)

        val iosMain by creating {
            dependsOn(mobileMain)
        }

        listOf(
            iosArm64(),
            iosX64(),
            iosSimulatorArm64(),
        ).forEach { target ->
            getByName("${target.name}Main") {
                dependsOn(iosMain)
            }
        }
    }
}
