@file:Suppress("ktlint:standard:filename")

package com.project.chat.data.di

import com.project.chat.data.attachment.DesktopAudioPlayer
import com.project.chat.data.attachment.DesktopAudioRecorder
import com.project.chat.data.attachment.DesktopImageCompressor
import com.project.chat.data.attachment.DesktopImageSaver
import com.project.chat.data.lifecycle.AppLifecycleObserver
import com.project.chat.data.network.ConnectionErrorHandler
import com.project.chat.data.network.ConnectivityObserver
import com.project.chat.data.notification.DesktopNotifier
import com.project.chat.data.notification.FirebasePushNotificationService
import com.project.chat.database.DatabaseFactory
import com.project.chat.domain.attachment.AudioPlayer
import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.ImageCompressor
import com.project.chat.domain.attachment.ImageSaver
import com.project.chat.domain.notification.PushNotificationService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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
actual val platformChatDataModule = module {
    singleOf(::DatabaseFactory)
    singleOf(::ConnectionErrorHandler)
    singleOf(::ConnectivityObserver)
    singleOf(::AppLifecycleObserver)
    singleOf(::DesktopNotifier)
    singleOf(::FirebasePushNotificationService) bind PushNotificationService::class
    singleOf(::DesktopImageCompressor) bind ImageCompressor::class
    singleOf(::DesktopImageSaver) bind ImageSaver::class
    singleOf(::DesktopAudioRecorder) bind AudioRecorder::class
    singleOf(::DesktopAudioPlayer) bind AudioPlayer::class
}
