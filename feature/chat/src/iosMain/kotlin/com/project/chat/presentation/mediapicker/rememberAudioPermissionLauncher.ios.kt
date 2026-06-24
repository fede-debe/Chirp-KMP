@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession

@Composable
actual fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher {
    return remember {
        AudioPermissionLauncher(
            isAvailable = true,
            onRequest = {
                // iOS shows its own one-time prompt; the callback may run off the main thread, but the
                // ViewModel actions it dispatches only touch StateFlow/Channel, which are thread-safe.
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    onResult(granted)
                }
            },
        )
    }
}
