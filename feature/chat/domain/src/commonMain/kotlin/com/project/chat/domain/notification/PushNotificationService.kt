package com.project.chat.domain.notification

import kotlinx.coroutines.flow.Flow

/**
 * Multiplatform interface for reactively observing device token emissions from the native OS.
 *
 * ## Strategy / Decisions
 * Separates token observation from token network registration. While Android has a background service that emits
 * tokens, we also need a proactive way to fetch/observe tokens within the app's lifecycle (e.g., exactly when a user logs in).
 *
 * ## How It Works
 * Provides a single entry point to observe token changes.
 *
 * @return A Flow emitting the device token string, or null if unavailable.
 */
interface PushNotificationService {
    fun observeDeviceToken(): Flow<String?>
}
