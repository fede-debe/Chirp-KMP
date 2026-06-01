package com.project.chat.data.di

import com.project.chat.data.chat.KtorChatParticipantService
import com.project.chat.domain.chat.ChatParticipantService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

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
    singleOf(::KtorChatParticipantService) bind ChatParticipantService::class
}
