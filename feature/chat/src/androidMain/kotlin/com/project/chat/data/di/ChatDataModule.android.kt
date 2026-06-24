package com.project.chat.data.di

import com.project.chat.data.attachment.AndroidAudioPlayer
import com.project.chat.data.attachment.AndroidAudioRecorder
import com.project.chat.data.attachment.AndroidImageCompressor
import com.project.chat.data.attachment.AndroidImageSaver
import com.project.chat.data.lifecycle.AppLifecycleObserver
import com.project.chat.data.network.ConnectionErrorHandler
import com.project.chat.data.network.ConnectivityObserver
import com.project.chat.data.notification.FirebasePushNotificationService
import com.project.chat.database.DatabaseFactory
import com.project.chat.domain.attachment.AudioPlayer
import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.ImageCompressor
import com.project.chat.domain.attachment.ImageSaver
import com.project.chat.domain.notification.PushNotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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
    singleOf(::ConnectivityObserver)
    singleOf(::ConnectionErrorHandler)

    singleOf(::FirebasePushNotificationService) bind PushNotificationService::class
    singleOf(::AndroidImageCompressor) bind ImageCompressor::class
    single { AndroidImageSaver(androidContext()) } bind ImageSaver::class
    single { AndroidAudioRecorder(androidContext()) } bind AudioRecorder::class
    singleOf(::AndroidAudioPlayer) bind AudioPlayer::class
}
