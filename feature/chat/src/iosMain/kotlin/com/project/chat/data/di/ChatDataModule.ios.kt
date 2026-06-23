package com.project.chat.data.di

import com.project.chat.data.attachment.IosImageCompressor
import com.project.chat.data.attachment.IosImageSaver
import com.project.chat.data.lifecycle.AppLifecycleObserver
import com.project.chat.data.network.ConnectionErrorHandler
import com.project.chat.data.network.ConnectivityObserver
import com.project.chat.data.notification.FirebasePushNotificationService
import com.project.chat.database.DatabaseFactory
import com.project.chat.domain.attachment.ImageCompressor
import com.project.chat.domain.attachment.ImageSaver
import com.project.chat.domain.notification.PushNotificationService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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
    singleOf(::ConnectionErrorHandler)
    singleOf(::FirebasePushNotificationService) bind PushNotificationService::class
    singleOf(::IosImageCompressor) bind ImageCompressor::class
    singleOf(::IosImageSaver) bind ImageSaver::class
}
