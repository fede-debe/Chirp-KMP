package com.project.chat.presentation.mappers

import com.project.chat.domain.models.Chat
import com.project.chat.presentation.models.ChatUi

fun Chat.toUi(localParticipantId: String): ChatUi {
    val (local, other) = participants.partition { it.userId == localParticipantId }
    return ChatUi(
        id = id,
        localParticipant = local.first().toUi(),
        otherParticipants = other.map { it.toUi() },
        lastMessage = lastMessage,
        lastMessageSenderUsername = lastMessageSenderUsername,
        creatorId = creatorId,
    )
}
