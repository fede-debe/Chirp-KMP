@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMultiImagePickerLauncher(
    selectionLimit: Int,
    onResult: (List<PickedAttachment>) -> Unit,
): MultiImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // PickMultipleVisualMedia requires a max of at least 2.
    val maxItems = selectionLimit.coerceAtLeast(2)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val attachments = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri -> context.readAttachment(uri) }
                }
                onResult(attachments)
            }
        }
    }

    return remember(maxItems) {
        MultiImagePickerLauncher(
            onLaunch = {
                launcher.launch(
                    PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            },
        )
    }
}

private fun Context.readAttachment(uri: Uri): PickedAttachment? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val mimeType = contentResolver.getType(uri) ?: "image/*"
    val fileName = queryDisplayName(uri) ?: "image"
    return PickedAttachment(
        bytes = bytes,
        mimeType = mimeType,
        fileName = fileName,
    )
}

private fun Context.queryDisplayName(uri: Uri): String? {
    return contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}
