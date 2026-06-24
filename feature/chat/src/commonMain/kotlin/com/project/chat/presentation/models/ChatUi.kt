package com.project.chat.presentation.models

import com.project.chat.domain.models.ChatMessage
import com.project.core.designsystem.components.avatar.ChatParticipantUi

data class ChatUi(
    val id: String,
    val localParticipant: ChatParticipantUi,
    val otherParticipants: List<ChatParticipantUi>,
    val lastMessage: ChatMessage?,
    val lastMessageSenderUsername: String?,
    /** User id of the chat's creator. The local user manages members only when this equals their own id. */
    val creatorId: String,
) {
    val isLocalUserCreator: Boolean
        get() = localParticipant.id == creatorId
}
