plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {
    androidLibrary {
        namespace = "com.project.core.shared"
        compileSdk = 36
        minSdk = 26
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)
                implementation(libs.touchlab.kermit)
                implementation(libs.sqlite.bundled)
            }
        }

        // Explicit opt-in for Android+Desktop JVM sharing — see HierarchyTemplate.kt: jvmCommon was
        // removed from the global hierarchy for AGP 9 path conflict reasons; we define it locally here.
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
        }

        desktopMain {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        androidMain {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.koin.android)
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

// Keep the generated BuildKonfig class in com.project.core.data so existing
// source files (e.g. HttpClientFactory.kt) need no package-name changes.
buildkonfig {
    packageName = "com.project.core.data"
}
