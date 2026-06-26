plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    androidLibrary {
        namespace = "com.project.feature.auth"
        compileSdk = 36
        minSdk = 26

        androidResources {
            enable = true
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.core.shared)
            }
        }
        androidMain {
            dependencies {
                // Native Google sign-in via Credential Manager + Google ID token helper.
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.google.identity.googleid)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.project.auth.presentation"
}
