package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.models.MessageUi
import com.project.core.designsystem.components.avatar.ChirpAvatarPhoto
import com.project.core.designsystem.components.chat.ChirpChatBubble
import com.project.core.designsystem.components.chat.TrianglePosition

@Composable
fun OtherUserMessage(
    message: MessageUi.OtherUserMessage,
    color: Color,
    onAttachmentClick: (MessageAttachmentUi) -> Unit,
    onPlayAttachment: (MessageAttachmentUi) -> Unit,
    onPauseAttachment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChirpAvatarPhoto(
            displayText = message.sender.initials,
            imageUrl = message.sender.imageUrl,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val audioAttachments = message.attachments.filter { it.mimeType.startsWith("audio/") }
            val imageAttachments = message.attachments
                .filterNot { it.mimeType.startsWith("audio/") }

            if (message.content.isNotBlank() || audioAttachments.isNotEmpty()) {
                ChirpChatBubble(
                    messageContent = message.content,
                    sender = message.sender.username,
                    trianglePosition = TrianglePosition.LEFT,
                    color = color,
                    formattedDateTime = message.formattedSentTime.asString(),
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
    }
}
