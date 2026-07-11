@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Launches the system camera via [ActivityResultContracts.TakePicture], which writes the full-size
 * photo to a [FileProvider] URI in the app cache. Because the app does not declare the `CAMERA`
 * permission, the system camera app handles capture and no runtime permission prompt is required.
 */
@Composable
actual fun rememberCameraLauncher(
    onResult: (PickedAttachment) -> Unit,
): CameraLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Holds the URI we asked the camera to write to, so the result callback can read it back.
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = captureUri
        if (success && uri != null) {
            scope.launch {
                val attachment = withContext(Dispatchers.IO) { context.readCapturedImage(uri) }
                if (attachment != null) {
                    onResult(attachment)
                }
            }
        }
    }

    return remember {
        CameraLauncher(
            isAvailable = true,
            onLaunch = {
                val uri = context.createCaptureUri()
                captureUri = uri
                launcher.launch(uri)
            },
        )
    }
}

private fun Context.createCaptureUri(): Uri {
    val file = File.createTempFile("camera_${System.currentTimeMillis()}", ".jpg", cacheDir)
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

private fun Context.readCapturedImage(uri: Uri): PickedAttachment? {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedAttachment(
        bytes = bytes,
        mimeType = "image/jpeg",
        fileName = "camera_${System.currentTimeMillis()}.jpg",
    )
}
