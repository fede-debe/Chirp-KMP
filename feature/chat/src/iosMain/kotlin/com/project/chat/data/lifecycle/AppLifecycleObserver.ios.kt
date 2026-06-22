package com.project.chat.data.lifecycle

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

/**
 * iOS-specific implementation of the application lifecycle observer using UIKit APIs.
 *
 * ## Strategy / Decisions
 * Relies on `NSNotificationCenter` to observe system-level app state notifications. The initial state check
 * requires special handling for the `UIApplicationStateInactive` state, which intuitively sounds like a
 * background state, but actually indicates the app is still in the foreground.
 *
 * ## How It Works
 * 1. Checks the initial `UIApplication.sharedApplication.applicationState`.
 * 2. Emits `true` if the state is `UIApplicationStateActive` OR `UIApplicationStateInactive`.
 * (Inactive means the app is technically visible but not receiving touch events—e.g., an incoming phone call
 * or the notification center is pulled down).
 * 3. Bridges UIKit notification callbacks to a Kotlin Flow using `callbackFlow`.
 * 4. Registers four distinct observers on the `NSNotificationCenter.defaultCenter` main queue:
 * - `didBecomeActive` -> emits `true`
 * - `willEnterForeground` -> emits `true`
 * - `didEnterBackground` -> emits `false`
 * - `willResignActive` -> emits `false`
 * 5. In `awaitClose`, calls `removeObserver` for all four registered callbacks to cleanly tear down the listeners.
 *
 * Technical Details
 * - **Queue Handling:** Notification observers are explicitly queued on `NSOperationQueue.mainQueue` to
 * mirror Android's Main dispatcher behavior.
 */
actual class AppLifecycleObserver {
    actual val isInForeground: Flow<Boolean> = callbackFlow {
        val currentState = UIApplication.sharedApplication.applicationState
        val isCurrentlyInForeground = when (currentState) {
            UIApplicationState.UIApplicationStateActive -> true
            // App itself is active, but could be that notification center is dragged down
            // or there's an ongoing phone call
            UIApplicationState.UIApplicationStateInactive -> true
            else -> false
        }
        send(isCurrentlyInForeground)

        val notificationCenter = NSNotificationCenter.defaultCenter

        val foregroundObserver = notificationCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            trySend(true)
        }

        val willEnterForegroundObserver = notificationCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            trySend(true)
        }

        val backgroundObserver = notificationCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            trySend(false)
        }

        val willResignActiveObserver = notificationCenter.addObserverForName(
            name = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) {
            trySend(false)
        }

        awaitClose {
            notificationCenter.removeObserver(foregroundObserver)
            notificationCenter.removeObserver(willEnterForegroundObserver)
            notificationCenter.removeObserver(backgroundObserver)
            notificationCenter.removeObserver(willResignActiveObserver)
        }
    }
}
