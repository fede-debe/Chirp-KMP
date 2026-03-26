@file:Suppress("DEPRECATION")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    androidLibrary {
        namespace = "com.project.chirp"
        compileSdk = libs.versions.projectCompileSdkVersion.get().toInt()
        minSdk = libs.versions.projectMinSdkVersion.get().toInt()
    }

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

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.tooling.preview)
        }
        commonMain.dependencies {
            // wire everything together - Overview of the app
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.presentation)

            implementation(projects.feature.auth.domain)
            implementation(projects.feature.auth.presentation)

            implementation(projects.feature.chat.data)
            implementation(projects.feature.chat.domain)
            implementation(projects.feature.chat.presentation)
            implementation(projects.feature.chat.database)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.preview)
            implementation(libs.jetbrains.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

// Workaround: android.kotlin.multiplatform.library does not register an assets
// directory with the CMP resources plugin, so CopyResourcesToAndroidAssetsTask's
// outputDirectory is never set. We set it via reflection (the class is internal).
val composeAndroidAssetsDir = layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets")
afterEvaluate {
    tasks.findByName("copyAndroidMainComposeResourcesToAndroidAssets")?.let { task ->
        val outputDirProp = task::class.java.getMethod("getOutputDirectory").invoke(task)
        (outputDirProp as org.gradle.api.file.DirectoryProperty).set(composeAndroidAssetsDir)
    }
}

compose.desktop {
    application {
        mainClass = "com.project.chirp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.project.chirp"
            packageVersion = libs.versions.desktopPackageVersion.get()
        }
    }
}
