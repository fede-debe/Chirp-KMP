@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Desktop has no camera-capture path. The launcher reports itself unavailable so the attachment sheet
 * hides the "Take photo" option, and launching is a no-op.
 */
@Composable
actual fun rememberCameraLauncher(
    onResult: (PickedAttachment) -> Unit,
): CameraLauncher {
    return remember { CameraLauncher(isAvailable = false, onLaunch = {}) }
}
