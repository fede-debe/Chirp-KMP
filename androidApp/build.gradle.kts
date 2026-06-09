plugins {
    alias(libs.plugins.convention.android.application.compose)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.koin.android)
}
