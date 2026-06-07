package com.project.chat.data.chat

import com.project.chat.data.dto.websocket.WebSocketMessageDto
import com.project.chat.data.mappers.toNewMessage
import com.project.chat.data.network.KtorWebSocketConnector
import com.project.chat.database.ChirpChatDatabase
import com.project.chat.domain.chat.ChatConnectionClient
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.domain.error.ConnectionError
import com.project.chat.domain.message.MessageRepository
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.core.domain.auth.SessionStorage
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * WebSocket-specific implementation of the real-time chat client that manages JSON serialization, transmission, and local fallback on network failure.
 *
 * ## Strategy / Decisions
 * Acts as the bridge between domain models and the raw `WebsocketConnector`. It is responsible for the two-step JSON serialization process required by the API (first serializing the specific outgoing DTO, then packaging it into a generic WebSocket payload with an explicit type identifier).
 *
 * ## How It Works
 * 1. Receives a [ChatMessage] domain model and maps it to an [OutgoingWebsocketDTO.NewMessage].
 * 2. Serializes the mapped DTO into a raw JSON string.
 * 3. Wraps the serialized string into a generic `WebsocketMessageDTO`, applying the `OutgoingWebsocketType.NEW_MESSAGE` type.
 * 4. Serializes the entire wrapper object into a final JSON payload and sends it via the `WebsocketConnector`.
 * 5. Uses an `onFailure` block to catch transmission errors. If sending fails, it updates the local database via `MessageRepository` to mark the message's delivery status as `FAILED`.
 *
 * ## Alternatives / Why Not
 * Standard HTTP communication was rejected for this specific flow because WebSockets do not provide an immediate, clear response code upon success. Instead, success is verified when the server broadcasts the message back to the client.
 *
 * Technical Details:
 * - Message IDs are generated on the client-side, not server-side, to ensure the client can uniquely identify and confirm the successful broadcast of the message when the server echoes it back.
 * - Thread Safety / Scope: Depends on `WebsocketConnector`, `MessageRepository`, database, and JSON parser injected via the constructor.
 */
class WebSocketChatConnectionClient(
    private val webSocketConnector: KtorWebSocketConnector,
    private val chatRepository: ChatRepository,
    private val database: ChirpChatDatabase,
    private val sessionStorage: SessionStorage,
    private val json: Json,
    private val messageRepository: MessageRepository,
) : ChatConnectionClient {

    override val chatMessages: Flow<ChatMessage>
        get() = TODO("Not yet implemented")

    override val connectionState = webSocketConnector.connectionState

    override suspend fun sendChatMessage(message: ChatMessage): EmptyResult<ConnectionError> {
        val outgoingDto = message.toNewMessage()
        val webSocketMessage = WebSocketMessageDto(
            type = outgoingDto.type.name,
            payload = json.encodeToString(outgoingDto),
        )
        val rawJsonPayload = json.encodeToString(webSocketMessage)

        return webSocketConnector
            .sendMessage(rawJsonPayload)
            .onFailure { error ->
                messageRepository.updateMessageDeliveryStatus(
                    messageId = message.id,
                    status = ChatMessageDeliveryStatus.FAILED,
                )
            }
    }
}
