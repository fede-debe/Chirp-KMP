package com.project.chirp.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Gradle convention plugin logic to configure the Kotlin Multiplatform Desktop target.
 *
 * ## Strategy / Decisions
 * To support desktop, we utilize the JVM to act as our primary desktop target. To maintain compilation consistency across all platforms in the project, the JVM target is explicitly pinned to version 17 to match the Android and iOS targets. We also explicitly override the target name to "desktop" so the generated source sets are cleanly labeled `desktopMain` instead of the default `jvmMain`.
 *
 * ## How It Works
 * 1. Extends the KMP Gradle project extension.
 * 2. Invokes the Kotlin JVM target configuration function, passing the explicit name "desktop".
 * 3. Accesses `compilerOptions` via `compilations.all` to set the `jvmTarget` strictly to JVM 17.
 * 4. This extension is then applied to both the Application module and all Library modules to ensure desktop variants are available across the entire project graph.
 *
 * ## Alternatives / Why Not
 * - **Default JVM Naming:** We could have omitted the explicit naming argument, but this was rejected because it would default to generating a `jvmMain` source set. This breaks our consistent `[platform]Main` naming convention.
 *
 * Technical Details
 * - Enforces JDK 17 constraints.
 * - Must be applied globally to avoid "no matching variant found" dependency resolution errors between the Compose App and core presentation/data modules.
 */
internal fun Project.configureDesktopTarget() {
    extensions.configure<KotlinMultiplatformExtension> {
        jvm("desktop") {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
            }
        }
    }
}
