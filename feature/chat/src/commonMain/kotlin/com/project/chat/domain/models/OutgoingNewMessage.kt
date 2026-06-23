package com.project.chat.domain.models

data class OutgoingNewMessage(
    val chatId: String,
    val messageId: String,
    val content: String,
    val attachments: List<MessageAttachment> = emptyList(),
)
