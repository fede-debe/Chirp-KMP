package com.project.chat.presentation.mappers

import com.project.chat.domain.models.MessageWithSender
import com.project.chat.presentation.models.ChatMessageUi
import com.project.chat.presentation.util.DateUtils

fun MessageWithSender.toUi(
    localUserId: String,
): ChatMessageUi {
    val isFromLocalUser = this.sender.userId == localUserId
    return if (isFromLocalUser) {
        ChatMessageUi.LocalUserMessage(
            id = message.id,
            content = message.content,
            deliveryStatus = message.deliveryStatus,
            isMenuOpen = false,
            formattedSentTime = DateUtils.formatMessageTime(instant = message.createdAt),
        )
    } else {
        ChatMessageUi.OtherUserMessage(
            id = message.id,
            content = message.content,
            formattedSentTime = DateUtils.formatMessageTime(instant = message.createdAt),
            sender = sender.toUi(),
        )
    }
}
