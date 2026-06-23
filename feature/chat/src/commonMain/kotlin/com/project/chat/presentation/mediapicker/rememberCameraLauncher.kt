@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable

/**
 * Captures a single photo with the device camera and returns it as a [PickedAttachment], so it flows
 * through the same compress → stage → send pipeline as a gallery pick.
 *
 * @param onResult invoked once with the captured photo (bytes already read, JPEG mime type).
 */
@Composable
expect fun rememberCameraLauncher(
    onResult: (PickedAttachment) -> Unit,
): CameraLauncher

/**
 * @param isAvailable whether this device has a usable camera (false on desktop). Callers hide the
 * "Take photo" option when this is false.
 */
class CameraLauncher(
    val isAvailable: Boolean,
    private val onLaunch: () -> Unit,
) {
    fun launch() {
        onLaunch()
    }
}
