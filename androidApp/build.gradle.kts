import java.util.Properties

plugins {
    alias(libs.plugins.convention.android.application.compose)
    alias(libs.plugins.google.services)
}

// Deep-link host is per-project. Driven by DEEP_LINK_HOST in local.properties; falls back to a
// placeholder so the build still succeeds (deep links just won't resolve until you set your domain).
val deepLinkHost: String = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) propsFile.inputStream().use { load(it) }
}.getProperty("DEEP_LINK_HOST") ?: "example.com"

android {
    defaultConfig {
        manifestPlaceholders["deepLinkHost"] = deepLinkHost
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.koin.android)
    implementation(libs.core.splashscreen)
    implementation(libs.core.ktx)
    implementation(libs.androidx.activity.compose)
}
