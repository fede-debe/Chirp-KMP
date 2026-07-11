@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher {
    return remember {
        AudioPermissionLauncher(
            isAvailable = false,
            onRequest = { onResult(false) },
        )
    }
}
