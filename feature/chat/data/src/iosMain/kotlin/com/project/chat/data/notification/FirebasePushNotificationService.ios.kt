package com.project.chat.data.notification

import com.project.chat.domain.notification.PushNotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications

/**
 * The iOS-specific actual implementation of the push notification service, responsible for observing and retrieving the FCM device token on Apple devices.
 *
 * ## Strategy / Decisions
 * Because iOS push notification delegates must be implemented natively in Swift/Objective-C, this Kotlin service cannot listen to Firebase directly. Instead, it observes a reactive Kotlin bridge (`IosDeviceTokenHolder`). It uses iOS native local preferences (`NSUserDefaults`) to persist and quickly reload the token across app launches, ensuring the shared KMP code can authenticate immediately without waiting for a fresh network registration cycle.
 *
 * ## How It Works
 * 1. Returns a flow observing the `token` state from `IosDeviceTokenHolder`.
 * 2. Utilizes the `onStart` flow operator to trigger fetching logic the exact moment collection begins (e.g., when the shared UI/ViewModel initializes).
 * 3. Checks if the bridge's current token value is `null`.
 * 4. If `null`, it accesses native iOS storage via `NSUserDefaults.standardUserDefaults` to check for an already persisted token.
 * 5. If an existing token is found locally, it populates the reactive bridge by calling `updateToken`.
 * 6. If no token exists locally, it actively calls `UIApplication.sharedApplication.registerForRemoteNotifications()` to instruct the iOS system to request a new remote token (which will subsequently be caught and routed back by the Swift `AppDelegate`).
 *
 * ## Technical Details
 * - **Storage Key:** Interacts with native iOS storage using the `"FCM_TOKEN"` key.
 * - **Native API Bridging:** Direct invocation of `UIApplication.sharedApplication` from Kotlin/Native to trigger system-level iOS registration flows.
 *
 * @return A reactive `Flow` emitting the nullable FCM device token string as it is loaded from disk or updated by the system.
 */
actual class FirebasePushNotificationService : PushNotificationService {
    actual override fun observeDeviceToken(): Flow<String?> {
        return IosDeviceTokenHolder
            .token
            .onStart {
                if (IosDeviceTokenHolder.token.value == null) {
                    val userDefaults = NSUserDefaults.standardUserDefaults
                    val fcmToken = userDefaults.stringForKey("FCM_TOKEN")

                    if (fcmToken != null) {
                        IosDeviceTokenHolder.updateToken(fcmToken)
                    } else {
                        UIApplication.sharedApplication.registerForRemoteNotifications()
                    }
                }
            }
    }
}
