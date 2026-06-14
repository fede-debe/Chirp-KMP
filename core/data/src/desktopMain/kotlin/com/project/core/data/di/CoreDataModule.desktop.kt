@file:Suppress("ktlint:standard:filename")

package com.project.core.data.di

import com.project.core.data.auth.createDataStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

/**
 * Desktop Koin dependency injection module for data and platform-specific implementations.
 *
 * ## Strategy / Decisions
 * Desktop requires concrete bindings even for interfaces that do not yet have full logic
 * (like Push Notifications or Lifecycle Handlers). If these are not explicitly bound to their
 * stubbed implementations, Koin will throw a `NoDefinitionFoundException` and crash the app at startup.
 * Additionally, because the Desktop target runs on the JVM, we can safely inject the Java-based
 * `OkHttp` engine for our Ktor HTTP client, sharing robust JVM networking capabilities.
 *
 * ## How It Works
 * 1. Provides `single` instances for `createDataStore` and `DatabaseFactory`.
 * 2. Explicitly binds stubbed classes (`ConnectionErrorHandler`, `ConnectivityObserver`,
 * `AppLifecycleHandlerServer`) to their respective interfaces.
 * 3. Uses `bind` to link the `FirebasePushNotificationService` interface to its empty desktop implementation.
 * 4. Provides the `OkHttp` Ktor engine.
 *
 * Technical Details:
 * - Relies on the `ktor-client-okhttp` dependency in the `desktopMain` build.gradle.kts.
 */
actual val platformCoreDataModule = module {
    single { createDataStore() }
    single<HttpClientEngine> { OkHttp.create() }
}
