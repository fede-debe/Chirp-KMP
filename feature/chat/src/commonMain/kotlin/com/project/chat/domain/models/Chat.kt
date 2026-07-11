package com.project.chat.domain.models

import kotlin.time.Instant

data class Chat(
    val id: String,
    val participants: List<ChatParticipant>,
    val lastActivityAt: Instant,
    val lastMessage: ChatMessage?,
    val lastMessageSenderUsername: String? = null,
    /** User id of the chat's creator — the only participant allowed to remove others. */
    val creatorId: String,
)
