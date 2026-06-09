package com.project.chat.data.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.project.chat.domain.notification.DeviceTokenService
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Android-specific background service to observe and intercept Firebase Cloud Messaging (FCM) token updates.
 *
 * ## Strategy / Decisions
 * Because this service can be awakened by the Android OS in the background at any time (even when the app UI is dead),
 * we cannot blindly send new tokens to the server. We must proactively verify the user's authentication state first.
 * * ## How It Works
 * 1. Overrides FCM's `onNewToken` callback.
 * 2. Observes the `SessionStorage` to fetch the first available `AuthInfo` element.
 * 3. If `AuthInfo` is not null (user is authenticated), it invokes `DeviceTokenService.registerToken`.
 *
 * ## Technical Details
 * - Relies on Koin property injection (`by inject()`) rather than constructor injection, as Android Services are
 * instantiated directly by the Android framework.
 * - Requires registration in the `AndroidManifest.xml` with `exported=false` (to prevent other apps from triggering it)
 * and an intent filter for `com.google.firebase.MESSAGING_EVENT`.
 */
class ChirpFirebaseMessagingService : FirebaseMessagingService() {

    private val deviceTokenService by inject<DeviceTokenService>()
    private val sessionStorage by inject<SessionStorage>()
    private val applicationScope by inject<CoroutineScope>()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        applicationScope.launch {
            val authInfo = sessionStorage.observeAuthInfo().first()
            if (authInfo != null) {
                deviceTokenService.registerToken(
                    token = token,
                    platform = "ANDROID",
                )
            }
        }
    }
}
