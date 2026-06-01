package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.chat.presentation.models.ChatMessageUi
import com.project.core.designSystem.components.avatar.ChirpAvatarPhoto
import com.project.core.designSystem.components.chat.ChirpChatBubble
import com.project.core.designSystem.components.chat.TrianglePosition

@Composable
fun OtherUserMessage(
    message: ChatMessageUi.OtherUserMessage,
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
        ChirpChatBubble(
            messageContent = message.content,
            sender = message.sender.username,
            trianglePosition = TrianglePosition.LEFT,
            formattedDateTime = message.formattedSentTime.asString(),
        )
    }
}
