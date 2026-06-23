package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.project.chat.presentation.Res
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.models.PendingAttachmentStatus
import com.project.chat.presentation.models.PendingAttachmentUi
import com.project.chat.presentation.upload_icon
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.vectorResource

/**
 * Image thumbnail loaded by Coil. [model] may be a URL [String] (sent/received) or a [ByteArray] (a
 * staged-but-not-yet-uploaded image). On load error — or while still processing ([model] == null) — it
 * falls back to a file icon, satisfying the "alternative UI element" requirement.
 */
@Composable
fun AttachmentImageThumbnail(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var isError by remember(model) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.extended.surfaceLower),
        contentAlignment = Alignment.Center,
    ) {
        if (model == null || isError) {
            Icon(
                imageVector = vectorResource(Res.drawable.upload_icon),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.extended.textPlaceholder,
                modifier = Modifier.size(24.dp),
            )
        } else {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { isError = true },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** A staged attachment in the composer: file icon while processing, thumbnail once ready, removable. */
@Composable
fun ComposerAttachmentChip(
    item: PendingAttachmentUi,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showThumbnail = item.status == PendingAttachmentStatus.READY ||
        item.status == PendingAttachmentStatus.UPLOADING
    val showSpinner = item.status == PendingAttachmentStatus.PROCESSING ||
        item.status == PendingAttachmentStatus.UPLOADING

    Box(
        modifier = modifier.size(CHIP_SIZE),
    ) {
        AttachmentImageThumbnail(
            model = if (showThumbnail) item.bytes else null,
            contentDescription = item.fileName,
            modifier = Modifier.matchParentSize(),
        )

        if (showSpinner) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            }
        }

        // No removing mid-upload.
        if (item.status != PendingAttachmentStatus.UPLOADING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint = MaterialTheme.colorScheme.extended.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposerAttachmentsRow(
    attachments: List<PendingAttachmentUi>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            ComposerAttachmentChip(
                item = attachment,
                onRemove = { onRemove(attachment.id) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BubbleAttachmentsRow(
    attachments: List<MessageAttachmentUi>,
    onAttachmentClick: (MessageAttachmentUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        attachments.forEach { attachment ->
            AttachmentImageThumbnail(
                model = attachment.url,
                contentDescription = attachment.fileName,
                modifier = Modifier
                    .size(BUBBLE_THUMBNAIL_SIZE)
                    .clickable { onAttachmentClick(attachment) },
            )
        }
    }
}

private val CHIP_SIZE: Dp = 64.dp
private val BUBBLE_THUMBNAIL_SIZE: Dp = 72.dp
