package com.project.chat.domain.notification

import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult

/**
 * Provides API contract for registering and unregistering specific device tokens with the backend.
 *
 * ## Strategy / Decisions
 * This service is strategically placed in the Chat Feature (specifically Chat Domain) because the only
 * push notifications currently handled by the app are related to new chat messages.
 *
 * ## How It Works
 * 1. `registerToken` sends the unique token from the OS (FCM/APNS) and the platform type to the server.
 * 2. `unregisterToken` deletes the token from the backend, typically used during logout.
 *
 * ## Alternatives / Why Not
 * Creating a standalone "Notifications" module was considered and would be an architecturally sound alternative.
 * It was ultimately placed in the Chat module for simplicity, as chat is the sole driver for notifications right now.
 *
 * ## Technical Details
 * When unregistering a token, we must explicitly pass the token string rather than relying solely on the
 * authenticated User ID. A single user can be logged into multiple devices (e.g., tablet and mobile), each
 * with its own unique device token. Relying on User ID alone would inadvertently unregister all of the user's devices.
 *
 * @param token The unique device token provided by FCM (Android) or APNS (iOS).
 * @param platform The OS platform ("Android" or "iOS") required by the backend to determine which external service to use.
 * @return Empty Result wrapping a DataError.Remote.
 */
interface DeviceTokenService {

    suspend fun registerToken(
        token: String,
        platform: String,
    ): EmptyResult<DataError.Remote>

    suspend fun unregisterToken(
        token: String,
    ): EmptyResult<DataError.Remote>
}
