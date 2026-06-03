package com.project.chat.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.project.chat.data.chat.KtorChatParticipantService
import com.project.chat.data.chat.KtorChatService
import com.project.chat.database.DatabaseFactory
import com.project.chat.domain.chat.ChatParticipantService
import com.project.chat.domain.chat.ChatService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Core Koin module for the chat feature's data layer.
 * * ## Strategy / Decisions
 * The database injection is kept here in the `ChatDataModule` rather than a generic
 * `DatabaseModule` to treat the database schema as a reusable library component. This prevents
 * tight coupling to Koin inside the raw database package itself.
 * * ## How It Works
 * 1. Includes a platform-specific `PlatformChatDataModule`.
 * 2. Retrieves the `DatabaseFactory` instance from the Koin graph.
 * 3. Calls `create()` on the factory.
 * 4. Sets the underlying SQLite driver to `BundledSQLiteDriver()`.
 * 5. Calls `build()` to finalize and provide the `ChirpChatDatabase` singleton.
 */
expect val platformChatDataModule: Module

/**
 * Koin dependency injection module for the Chat data layer.
 *
 * ## Strategy / Decisions
 * - **Singleton Lifetime:** Declares the `KtorChatParticipantService` as a `singleOf` to be instantiated exactly once per application lifecycle.
 * - **Interface Binding:** Uses the `bind` operator to map the concrete Ktor implementation to the `ChatParticipantService` interface. This ensures the ViewModel receives the implementation automatically when requesting the interface.
 *
 * Technical Details:
 * - Resolves the "No definition found" Koin crash encountered when launching the feature.
 */
val chatDataModule = module {
    includes(platformChatDataModule)

    singleOf(::KtorChatParticipantService) bind ChatParticipantService::class
    singleOf(::KtorChatService) bind ChatService::class
    single {
        get<DatabaseFactory>()
            .create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}
