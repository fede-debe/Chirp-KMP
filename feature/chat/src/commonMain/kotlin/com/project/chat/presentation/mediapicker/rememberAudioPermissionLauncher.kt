@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable

/**
 * Requests microphone permission for voice recording. Mirrors [rememberCameraLauncher]: Android shows a
 * runtime prompt (RECORD_AUDIO) via an activity-result launcher; iOS uses AVAudioSession's prompt;
 * desktop reports unavailable. [onResult] is invoked with whether recording may proceed.
 */
@Composable
expect fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher

/** @param isAvailable whether mic recording is supported on this platform (false on desktop). */
class AudioPermissionLauncher(
    val isAvailable: Boolean,
    private val onRequest: () -> Unit,
) {
    fun request() {
        onRequest()
    }
}
