plugins {
    alias(libs.plugins.convention.cmp.application)
    alias(libs.plugins.conveyor)
}

version = "1.0.0"

kotlin {
    androidLibrary {
        namespace = "com.project.chirp.shared"
        compileSdk = 36
        minSdk = 26

        androidResources {
            enable = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(projects.core.shared)
            implementation(projects.core.ui)

            implementation(projects.feature.auth)

            implementation(projects.feature.chat)

            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.bundles.koin.common)
            implementation(libs.jetbrains.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.compose)
        }
        /**
         * Desktop dependency and application entry point configuration.
         *
         * ## Strategy / Decisions
         * This file configures the execution environment for the desktop application. Unlike mobile apps, desktop apps require a specific class entry point. Furthermore, we explicitly integrate `JSystemThemeDetector` because detecting system theme preferences (Dark/Light mode) requires interfacing with vastly different OS-level APIs (Windows Registry, macOS Plists, etc.), making an existing Java-based library the most robust solution.
         *
         * ## How It Works
         * 1. Inside the `compose.desktop.application` block, `mainClass` is mapped to `com.plcoding.chirp.main.kt`.
         * 2. `compose.desktop.currentOs` is added to `desktopMain` dependencies to bundle the correct OS-specific UI binaries.
         * 3. `JSystemThemeDetector` is linked from the version catalog to handle cross-platform theme state detection.
         *
         * ## Alternatives / Why Not
         * - **Native Kotlin Theme Detection:** We rejected writing custom `expect`/`actual` logic for Windows/Mac/Linux theme parsing because it is overly complex and fragile. Using `JSystemThemeDetector` abstracts these OS intricacies out of the box.
         *
         * Technical Details
         * - `JSystemThemeDetector` requires the JitPack repository (`maven { url = uri("https://jitpack.io") }`) to be added to `settings.gradle.kts` for dependency resolution.
         */
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlin.stdlib)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.jsystemthemedetector)

            implementation(compose.desktop.linux_x64)
            implementation(compose.desktop.linux_arm64)
            implementation(compose.desktop.macos_x64)
            implementation(compose.desktop.macos_arm64)
            implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.windows_arm64)
        }
    }
}

compose.resources {
    packageOfResClass = "chirp.composeapp.generated.resources"
}

compose.desktop {
    application {
        mainClass = "com.project.chirp.MainKt"

        nativeDistributions {
            packageName = "com.project.chirp"
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        },
    )
}
