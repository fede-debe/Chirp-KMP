@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.convention.cmp.application)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.conveyor)
}

version = "1.0.0"

kotlin {
    androidLibrary {
        compileSdk = 36
        minSdk = 26
        namespace = "com.project.chirp.composeapp"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.core.splashscreen)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.presentation)

            implementation(projects.feature.auth.domain)
            implementation(projects.feature.auth.presentation)

            implementation(projects.feature.chat.data)
            implementation(projects.feature.chat.database)
            implementation(projects.feature.chat.domain)
            implementation(projects.feature.chat.presentation)

            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.bundles.koin.common)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
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
            implementation(projects.core.presentation)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlin.stdlib)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
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
