package com.project.auth.presentation.social

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelNormal

/**
 * Resolves the app's main, normal-level window to use as the presentation anchor for the Apple and
 * Google (web) sign-in sheets. Same rationale as the chat module's `topMostViewController` — avoid the
 * deprecated `keyWindow` and the keyboard/text-effects window, which refuse to present.
 */
internal fun topMostWindow(): UIWindow? {
    val windows = UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>()
    return windows.firstOrNull { it.isKeyWindow() && it.windowLevel == UIWindowLevelNormal }
        ?: windows.firstOrNull { it.windowLevel == UIWindowLevelNormal }
        ?: windows.firstOrNull { it.isKeyWindow() }
        ?: windows.firstOrNull()
}

/** Presentation anchor for the auth sheets; falls back to a fresh window if none is found. */
internal fun presentationAnchor(): ASPresentationAnchor = topMostWindow() ?: UIWindow()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val pointer = this.bytes ?: return ByteArray(0)
    return pointer.readBytes(length)
}
