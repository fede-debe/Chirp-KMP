plugins {
    alias(libs.plugins.convention.android.application.compose)
    alias(libs.plugins.kotlin.android)
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
}
