package com.project.chat.data.di

import com.project.chat.data.lifecycle.AppLifecycleObserver
import com.project.chat.data.network.ConnectivityObserver
import com.project.chat.database.DatabaseFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * iOS-specific Koin module for chat data dependencies.
 * * ## How It Works
 * Provides the `DatabaseFactory` as a singleton. Requires no parameters since the iOS
 * file manager implementation does not rely on a context object.
 */
actual val platformChatDataModule = module {
    single { DatabaseFactory() }
    singleOf(::AppLifecycleObserver)
    singleOf(::ConnectivityObserver)
}
