package com.project.chat.data.dto.websocket

import com.project.chat.data.dto.request.AttachmentInput
import kotlinx.serialization.Serializable

/**
 * Serializable Data Transfer Object representing payloads sent from the client to the server over the WebSocket connection.
 *
 * ## Strategy / Decisions
 * Designed as a `sealed interface` to keep the architecture easily extendable. Even though "New Message" is currently the only outgoing action, using a sealed interface prepares the system for future outgoing event types (e.g., typing indicators) without needing to rewrite the serialization flow.
 *
 * ## How It Works
 * 1. Defines specific payload classes like `NewMessage`.
 * 2. Holds the client-generated `messageId` alongside the chat ID and string content.
 *
 * Technical Details:
 * - Explicitly omits `senderId` and `deliveryStatus`, as the server infers the sender from the authentication token and the delivery status is tracked locally.
 */
enum class OutgoingWebSocketType {
    NEW_MESSAGE,
}

@Serializable
sealed class OutgoingWebSocketDto(
    val type: OutgoingWebSocketType,
) {

    @Serializable
    data class NewMessage(
        val chatId: String,
        val messageId: String,
        val content: String,
        val attachments: List<AttachmentInput> = emptyList(),
    ) : OutgoingWebSocketDto(OutgoingWebSocketType.NEW_MESSAGE)
}
