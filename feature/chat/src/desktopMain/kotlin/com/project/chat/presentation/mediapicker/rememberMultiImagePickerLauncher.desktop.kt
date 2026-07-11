@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Desktop is out of scope for the mobile image-attachments task. A no-op launcher keeps the shared
 * composer compiling and running on desktop without picking anything.
 */
@Composable
actual fun rememberMultiImagePickerLauncher(
    selectionLimit: Int,
    onResult: (List<PickedAttachment>) -> Unit,
): MultiImagePickerLauncher {
    return remember { MultiImagePickerLauncher(onLaunch = {}) }
}
