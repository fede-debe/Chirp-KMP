@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onResult(granted)
    }

    return remember {
        AudioPermissionLauncher(
            isAvailable = true,
            onRequest = {
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (alreadyGranted) {
                    onResult(true)
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )
    }
}
