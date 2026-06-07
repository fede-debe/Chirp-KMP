package com.project.chat.data.di

import com.project.chat.data.lifecycle.AppLifecycleObserver
import com.project.chat.database.DatabaseFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Android-specific Koin module for chat data dependencies.
 * * ## How It Works
 * Provides the `DatabaseFactory` as a singleton by passing in the `androidContext()`
 * supplied by the `koin-android` library.
 */
actual val platformChatDataModule = module {
    single { DatabaseFactory(androidContext()) }
    singleOf(::AppLifecycleObserver)
}
