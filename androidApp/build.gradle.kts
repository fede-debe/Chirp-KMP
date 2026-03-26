plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.project.chirp"
    compileSdk = libs.versions.projectCompileSdkVersion.get().toInt()

    defaultConfig {
        applicationId = "com.project.chirp"
        minSdk = libs.versions.projectMinSdkVersion.get().toInt()
        targetSdk = libs.versions.projectTargetSdkVersion.get().toInt()
        versionCode = libs.versions.projectVersionCode.get().toInt()
        versionName = libs.versions.projectVersionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Workaround: include Compose Resources from :composeApp as assets.
// android.kotlin.multiplatform.library does not package composeResources into
// the AAR, so we pull them directly from composeApp's build output.
val composeAppAssetsDir = project(":composeApp").layout.buildDirectory
    .dir("generated/compose/resourceGenerator/androidAssets")

android.sourceSets["main"].assets.srcDir(composeAppAssetsDir)

afterEvaluate {
    listOf("mergeDebugAssets", "mergeReleaseAssets").forEach { taskName ->
        tasks.findByName(taskName)
            ?.dependsOn(":composeApp:copyAndroidMainComposeResourcesToAndroidAssets")
    }
}
