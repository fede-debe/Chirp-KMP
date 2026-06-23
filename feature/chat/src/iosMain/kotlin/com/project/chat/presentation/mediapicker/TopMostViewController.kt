package com.project.chat.presentation.mediapicker

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelNormal

/**
 * Resolves the view controller to present pickers / the camera from.
 *
 * `UIApplication.keyWindow` is deprecated (nil on iPad / multi-scene), and the key window can be the
 * keyboard's text-effects window — which refuses to present view controllers ("Keyboard cannot present
 * view controllers"). So we pick the app's main, normal-level window and walk to its top-most presented
 * controller (which also avoids "already presenting" failures when another modal is on screen).
 */
internal fun topMostViewController(): UIViewController? {
    val windows = UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>()
    val mainWindow = windows.firstOrNull { it.isKeyWindow() && it.windowLevel == UIWindowLevelNormal }
        ?: windows.firstOrNull { it.windowLevel == UIWindowLevelNormal }
        ?: windows.firstOrNull { it.isKeyWindow() }
        ?: windows.firstOrNull()

    var controller = mainWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
