import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Registers and exposes the convention plugins to the overarching build logic.
 *
 * ## Strategy / Decisions
 * - **Strict Validation:** Configures the `validatePlugins` task to fail on warnings, ensuring zero silent issues in our build logic layer.
 * - **Alias Registration:** Defines the unique plugin ID (`com.project.android.application`) mapped to its implementation class so the root project can consume it natively.
 */

plugins {
    `kotlin-dsl`
}

group = "com.project.convention.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.project.convention.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
