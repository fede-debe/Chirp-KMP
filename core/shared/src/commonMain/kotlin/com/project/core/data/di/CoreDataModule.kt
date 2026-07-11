package com.project.core.data.di

import com.project.core.data.auth.KtorAuthService
import com.project.core.data.logging.KermitLogger
import com.project.core.data.networking.HttpClientFactory
import com.project.core.data.util.NonceFactory
import com.project.core.domain.auth.AuthService
import com.project.core.domain.logging.ChirpLogger
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Expected declaration for platform-specific dependency definitions.
 *
 * ## Strategy / Decisions
 * * **Expect/Actual Pattern:** Used to provide platform-specific networking engines (OkHttp for Android,
 *   Darwin for iOS) while keeping the core logic in common code.
 */
expect val platformCoreDataModule: Module

/**
 * Dependency injection module for the core data layer.
 * Provides singleton instances for networking, logging, and authentication services.
 *
 * ## Strategy / Decisions
 * * **Decentralized DI Modules:** Instead of defining all dependencies in the main app module,
 *   modules are defined within their own layers (e.g., core-data). This favors "Separation of
 *   Concerns" and improves build times by isolating components.
 * * **Singleton Pattern:** Networking clients (HTTPClient) and services are marked as `single`
 *   to ensure a consistent state and resource efficiency across the application lifecycle.
 * * **Interface Binding:** Uses `bind` to link implementations (e.g., KtorAuthService) to
 *   abstractions (AuthService). This facilitates easier mocking during unit testing.
 *
 * ## How It Works
 * 1. **Logger Setup:** Provides a Kermit logger implementation wrapped in the project's Logger interface.
 * 2. **Client Factory:** Invokes the `HttpClientFactory` while injecting the logger and
 *    the platform-specific `HTTPClientEngine`.
 * 3. **Includes:** Automatically includes [platformCoreDataModule] using the `includes`
 *    keyword to ensure platform-specific engines are available to the common factory.
 */
val coreDataModule = module {
    includes(platformCoreDataModule)
    single<ChirpLogger> { KermitLogger }
    single {
        HttpClientFactory(get(), get()).create(get())
    }
    singleOf(::KtorAuthService) bind AuthService::class
    singleOf(::NonceFactory)
    // SessionStorage is bound per-platform: encrypted (Keystore/Keychain) on mobile,
    // DataStore on desktop. See each platformCoreDataModule.
}
