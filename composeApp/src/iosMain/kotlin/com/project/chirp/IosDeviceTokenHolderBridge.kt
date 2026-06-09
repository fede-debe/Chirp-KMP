package com.project.chirp

import com.project.chat.data.notification.IosDeviceTokenHolder

/**
 * A proxy class to expose the internal `IOSDeviceTokenHolder` to the Xcode Swift project.
 *
 * ## Strategy / Decisions
 * The primary `IOSDeviceTokenHolder` resides within the `chat-data` module, which is not exported or directly accessible to the Xcode Swift project (Xcode only sees the `composeApp` application module). Instead of fundamentally restructuring module visibilities, this bridge is placed inside `composeApp` to forward calls down to the data layer.
 *
 * ## How It Works
 * 1. Declares a public function `updateToken(token)`.
 * 2. Takes the passed token and directly delegates the call to `IOSDeviceTokenHolder.updateToken(token)` in the underlying data module.
 *
 * ## Technical Details
 * - This file exists strictly as a compiler/visibility workaround for Kotlin Multiplatform module boundaries in Xcode.
 * * @param token The FCM device token string retrieved from Firebase in Swift.
 */
object IosDeviceTokenHolderBridge {
    fun updateToken(token: String) {
        IosDeviceTokenHolder.updateToken(token)
    }
}
