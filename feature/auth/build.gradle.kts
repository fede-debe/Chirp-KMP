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
                implementation(projects.core.domain)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.project.auth.presentation"
}
