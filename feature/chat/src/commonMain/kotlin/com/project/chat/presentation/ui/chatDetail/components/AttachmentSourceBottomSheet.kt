package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.project.chat.presentation.Res
import com.project.chat.presentation.add_attachment
import com.project.chat.presentation.camera_icon
import com.project.chat.presentation.choose_from_gallery
import com.project.chat.presentation.take_photo
import com.project.chat.presentation.upload_icon
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * Bottom sheet shown when the user taps the composer's attach button: choose to take a photo with the
 * camera or pick from the gallery. The camera row is hidden when [isCameraAvailable] is false (desktop).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSourceBottomSheet(
    isCameraAvailable: Boolean,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.add_attachment),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            if (isCameraAvailable) {
                AttachmentSourceRow(
                    icon = vectorResource(Res.drawable.camera_icon),
                    label = stringResource(Res.string.take_photo),
                    onClick = onTakePhoto,
                )
            }
            AttachmentSourceRow(
                icon = vectorResource(Res.drawable.upload_icon),
                label = stringResource(Res.string.choose_from_gallery),
                onClick = onChooseFromGallery,
            )
        }
    }
}

@Composable
private fun AttachmentSourceRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.extended.textSecondary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
