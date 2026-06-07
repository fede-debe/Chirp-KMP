package com.project.chat.data.chat

import com.project.chat.data.dto.websocket.IncomingWebSocketDto
import com.project.chat.data.dto.websocket.IncomingWebSocketType
import com.project.chat.data.dto.websocket.WebSocketMessageDto
import com.project.chat.data.mappers.toDomain
import com.project.chat.data.mappers.toEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
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
    private val applicationScope: CoroutineScope,
) : ChatConnectionClient {

    override val chatMessages = webSocketConnector
        .messages
        .mapNotNull { parseIncomingMessage(it) }
        .onEach { handleIncomingMessage(it) }
        .filterIsInstance<IncomingWebSocketDto.NewMessageDto>()
        .mapNotNull {
            database.chatMessageDao.getMessageById(it.id)?.toDomain()
        }
        .shareIn(
            applicationScope,
            SharingStarted.WhileSubscribed(5000),
        )

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

    private fun parseIncomingMessage(message: WebSocketMessageDto): IncomingWebSocketDto? {
        return when (message.type) {
            IncomingWebSocketType.NEW_MESSAGE.name -> {
                json.decodeFromString<IncomingWebSocketDto.NewMessageDto>(message.payload)
            }
            IncomingWebSocketType.MESSAGE_DELETED.name -> {
                json.decodeFromString<IncomingWebSocketDto.MessageDeletedDto>(message.payload)
            }
            IncomingWebSocketType.PROFILE_PICTURE_UPDATED.name -> {
                json.decodeFromString<IncomingWebSocketDto.ProfilePictureUpdated>(message.payload)
            }
            IncomingWebSocketType.CHAT_PARTICIPANTS_CHANGED.name -> {
                json.decodeFromString<IncomingWebSocketDto.ChatParticipantsChangedDto>(message.payload)
            }
            else -> null
        }
    }

    private suspend fun handleIncomingMessage(message: IncomingWebSocketDto) {
        when (message) {
            is IncomingWebSocketDto.ChatParticipantsChangedDto -> refreshChat(message)
            is IncomingWebSocketDto.MessageDeletedDto -> deleteMessage(message)
            is IncomingWebSocketDto.NewMessageDto -> handleNewMessage(message)
            is IncomingWebSocketDto.ProfilePictureUpdated -> updateProfilePicture(message)
        }
    }

    private suspend fun refreshChat(message: IncomingWebSocketDto.ChatParticipantsChangedDto) {
        chatRepository.fetchChatById(message.chatId)
    }

    private suspend fun deleteMessage(message: IncomingWebSocketDto.MessageDeletedDto) {
        database.chatMessageDao.deleteMessageById(message.messageId)
    }

    private suspend fun handleNewMessage(message: IncomingWebSocketDto.NewMessageDto) {
        val chatExists = database.chatDao.getChatById(message.chatId) != null
        if (!chatExists) {
            chatRepository.fetchChatById(message.chatId)
        }

        val entity = message.toEntity()
        database.chatMessageDao.upsertMessage(entity)
    }

    private suspend fun updateProfilePicture(message: IncomingWebSocketDto.ProfilePictureUpdated) {
        database.chatParticipantDao.updateProfilePictureUrl(
            userId = message.userId,
            newUrl = message.newUrl,
        )

        val authInfo = sessionStorage.observeAuthInfo().firstOrNull()
        if (authInfo != null) {
            sessionStorage.set(
                info = authInfo.copy(
                    user = authInfo.user.copy(
                        profilePictureUrl = message.newUrl,
                    ),
                ),
            )
        }
    }
}
