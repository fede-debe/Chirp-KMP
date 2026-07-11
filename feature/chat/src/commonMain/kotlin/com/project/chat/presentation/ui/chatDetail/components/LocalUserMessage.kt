package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.presentation.Res
import com.project.chat.presentation.delete_for_everyone
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.models.MessageUi
import com.project.chat.presentation.reload_icon
import com.project.chat.presentation.retry
import com.project.chat.presentation.you
import com.project.core.designsystem.components.chat.ChirpChatBubble
import com.project.core.designsystem.components.chat.TrianglePosition
import com.project.core.designsystem.components.dropdown.ChirpDropDownMenu
import com.project.core.designsystem.components.dropdown.DropDownItem
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun LocalUserMessage(
    message: MessageUi.LocalUserMessage,
    messageWithOpenMenu: MessageUi.LocalUserMessage?,
    onMessageLongClick: () -> Unit,
    onDismissMessageMenu: () -> Unit,
    onDeleteClick: () -> Unit,
    onRetryClick: () -> Unit,
    onAttachmentClick: (MessageAttachmentUi) -> Unit,
    onPlayAttachment: (MessageAttachmentUi) -> Unit,
    onPauseAttachment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val audioAttachments = message.attachments.filter { it.mimeType.startsWith("audio/") }
                val imageAttachments = message.attachments
                    .filterNot { it.mimeType.startsWith("audio/") }

                if (message.content.isNotBlank() || audioAttachments.isNotEmpty()) {
                    ChirpChatBubble(
                        messageContent = message.content,
                        sender = stringResource(Res.string.you),
                        formattedDateTime = message.formattedSentTime.asString(),
                        trianglePosition = TrianglePosition.RIGHT,
                        messageStatus = {
                            MessageStatus(
                                status = message.deliveryStatus,
                            )
                        },
                        onLongClick = {
                            onMessageLongClick()
                        },
                        content = if (audioAttachments.isNotEmpty()) {
                            {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    audioAttachments.forEach { audio ->
                                        VoiceMessagePlayer(
                                            attachment = audio,
                                            onPlay = { onPlayAttachment(audio) },
                                            onPause = onPauseAttachment,
                                        )
                                    }
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
                if (imageAttachments.isNotEmpty()) {
                    BubbleAttachmentsRow(
                        attachments = imageAttachments,
                        onAttachmentClick = onAttachmentClick,
                    )
                }
            }

            ChirpDropDownMenu(
                isOpen = message.id == messageWithOpenMenu?.id,
                onDismiss = onDismissMessageMenu,
                items = listOf(
                    DropDownItem(
                        title = stringResource(Res.string.delete_for_everyone),
                        icon = Icons.Default.Delete,
                        contentColor = MaterialTheme.colorScheme.extended.destructiveHover,
                        onClick = onDeleteClick,
                    ),
                ),
            )
        }

        if (message.deliveryStatus == ChatMessageDeliveryStatus.FAILED) {
            IconButton(
                onClick = onRetryClick,
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.reload_icon),
                    contentDescription = stringResource(Res.string.retry),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
