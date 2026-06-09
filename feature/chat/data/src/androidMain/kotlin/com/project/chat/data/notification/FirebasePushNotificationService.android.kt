package com.project.chat.data.notification

import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.project.chat.domain.notification.PushNotificationService
import com.project.core.domain.logging.ChirpLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Android actual implementation of the PushNotificationService using the native Firebase Messaging SDK.
 *
 * ## Strategy / Decisions
 * Instead of waiting for a broadcast emission from the Firebase service, this implementation proactively
 * pulls the token from the Firebase SDK on demand.
 *
 * ## How It Works
 * 1. Accesses `FirebaseMessaging.getInstance().token`.
 * 2. Uses `await()` to bridge the Firebase asynchronous `Task` into Kotlin Coroutines.
 * 3. Emits the resulting FCM token into the flow.
 *
 * ## Technical Details
 * Wrapped in a `try-catch` block handling cancellation and standard exceptions natively. If fetching the token fails
 * (e.g., missing Google Services configuration), it safely emits `null` and logs the exception.
 */
actual class FirebasePushNotificationService(
    private val logger: ChirpLogger,
) : PushNotificationService {

    actual override fun observeDeviceToken(): Flow<String?> = flow {
        try {
            val fcmToken = Firebase.messaging.token.await()
            logger.info("Initial FCM token received: $fcmToken")
            emit(fcmToken)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.error("Failed to get FCM token", e)
            emit(null)
        }
    }
}
