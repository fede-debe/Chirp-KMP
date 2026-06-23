@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable

/**
 * Multi-select image picker for chat attachments (gallery, up to [selectionLimit] images). Kept separate
 * from the profile single-image picker so neither flow constrains the other.
 *
 * @param onResult invoked with every picked image (bytes already read, plus mime type and original file
 * name for the upload + the file-icon fallback chip).
 */
@Composable
expect fun rememberMultiImagePickerLauncher(
    selectionLimit: Int,
    onResult: (List<PickedAttachment>) -> Unit,
): MultiImagePickerLauncher

class MultiImagePickerLauncher(
    private val onLaunch: () -> Unit,
) {
    fun launch() {
        onLaunch()
    }
}

class PickedAttachment(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String,
)
