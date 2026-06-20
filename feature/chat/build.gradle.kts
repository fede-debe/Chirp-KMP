import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.convention.cmp.feature)
    alias(libs.plugins.convention.room)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {
    androidLibrary {
        namespace = "com.project.feature.chat"
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
                implementation(projects.core.data)
                // core.designsystem + core.presentation auto-added by CmpFeatureConventionPlugin
                // koin BOM, koin-compose, koin-compose-viewmodel auto-added by CmpFeatureConventionPlugin

                implementation(libs.koin.core)
                implementation(libs.bundles.ktor.common)

                implementation(libs.material3.adaptive)
                implementation(libs.material3.adaptive.layout)
                implementation(libs.material3.adaptive.navigation)
                implementation(libs.jetbrains.compose.backhandler)
                implementation(libs.kotlinx.datetime)
            }
        }

        val mobileMain by getting
        androidMain {
            dependsOn(mobileMain)
            dependencies {
                // koin.android auto-added by CmpFeatureConventionPlugin
                implementation(libs.androidx.lifecycle.process)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging)
            }
        }
    }

    targets.withType<KotlinNativeTarget> {
        compilations.getByName("main") {
            cinterops {
                create("network") {
                    defFile(file("src/nativeInterop/cinterop/network.def"))
                }
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.project.chat.presentation"
}
