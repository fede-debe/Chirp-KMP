plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    androidLibrary {
        namespace = "com.project.core.ui"
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

                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                implementation(libs.material3.adaptive)
                implementation(libs.jetbrains.lifecycle.compose)
                implementation(libs.bundles.koin.common)
            }
        }

        val mobileMain by getting {
            dependencies {
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.notifications)
            }
        }

        androidMain {
            dependsOn(mobileMain)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.project.core.ui"
}
