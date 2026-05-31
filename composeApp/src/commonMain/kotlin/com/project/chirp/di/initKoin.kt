@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chirp.di

import com.project.auth.presentation.di.authPresentationModule
import com.project.core.data.di.coreDataModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Shared entry point for initializing Koin across all platforms.
 *
 * ## Strategy / Decisions
 * * **Cross-Platform Initialization:** Provides a helper function that can be called from
 *   native entry points (Swift for iOS, Application class for Android).
 * * **Configuration Injection:** Accepts `KoinAppDeclaration` (a lambda) to allow
 *   platforms to inject their own specific configurations, such as the Android Context.
 *
 * ## How It Works
 * 1. Calls `startKoin`.
 * 2. Invokes the platform-specific configuration lambda.
 * 3. Loads the shared modules ([coreDataModule], [authPresentationModule]).
 */

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            coreDataModule,
            authPresentationModule,
            appModule,
        )
    }
}
