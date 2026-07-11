package com.project.chat.domain.models

/**
 * A real-time, ephemeral signal that a participant is (or has stopped) typing in a chat. Carried in-memory
 * only — typing presence is never persisted, unlike [ChatMessage].
 *
 * The `username` is supplied by the server's broadcast, so the presentation layer does not need to resolve
 * it from the chat participants.
 */
data class TypingUser(
    val chatId: String,
    val userId: String,
    val username: String,
    val isTyping: Boolean,
)
