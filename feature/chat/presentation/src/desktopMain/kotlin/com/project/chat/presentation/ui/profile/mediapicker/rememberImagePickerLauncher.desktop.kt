@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.ui.profile.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePickerLauncher(onResult: (PickedImageData) -> Unit): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher(
            onLaunch = {},
        )
    }
}
