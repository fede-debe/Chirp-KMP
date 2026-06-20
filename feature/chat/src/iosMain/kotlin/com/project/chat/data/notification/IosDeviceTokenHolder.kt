package com.project.chat.data.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A Kotlin object acting as a bridge to allow the iOS Swift environment to pass FCM device tokens to the Kotlin Multiplatform shared code.
 *
 * ## Strategy / Decisions
 * Because there is no native way to define push notification listener logic entirely within the Kotlin iOS target, this singleton serves as an observable bridge. It allows Swift to push token updates into a flow that the shared Kotlin `FirebasePushNotificationService` can observe and react to.
 *
 * ## How It Works
 * 1. Initializes a private `MutableStateFlow` holding a nullable token string.
 * 2. Exposes an `updateToken` function that Swift calls to push a newly received FCM token into the state flow.
 * 3. The `FirebasePushNotificationService` returns this flow. It uses the `onStart` operator so that the moment collection begins, it checks if the current token is null.
 * 4. If null, it queries iOS `NSUserDefaults.standardUserDefaults` using the local FCM token key.
 * 5. If the token is found in user defaults, it updates the flow. If it does not exist, it triggers `UIApplication.sharedApplication.registerForRemoteNotifications()` to proactively request one from iOS.
 *
 * ## Technical Details
 * - Relies on `NSUserDefaults` for native iOS local storage to persist the token across app launches.
 * - Thread-safety/Lifecycle: Tied to the application lifecycle via `onStart` collection.
 * * @param token The FCM device token string provided by the native iOS environment.
 */
object IosDeviceTokenHolder {

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    fun updateToken(token: String?) {
        _token.value = token
    }
}
