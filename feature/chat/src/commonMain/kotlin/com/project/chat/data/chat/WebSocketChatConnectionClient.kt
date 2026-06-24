package com.project.chat.data.chat

import com.project.chat.data.dto.websocket.IncomingWebSocketDto
import com.project.chat.data.dto.websocket.IncomingWebSocketType
import com.project.chat.data.dto.websocket.OutgoingWebSocketDto
import com.project.chat.data.dto.websocket.OutgoingWebSocketType
import com.project.chat.data.dto.websocket.WebSocketMessageDto
import com.project.chat.data.mappers.toDomain
import com.project.chat.data.mappers.toEntity
import com.project.chat.data.network.KtorWebSocketConnector
import com.project.chat.database.ChirpChatDatabase
import com.project.chat.domain.chat.ChatConnectionClient
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ChatRemoval
import com.project.chat.domain.models.ChatRemovalReason
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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
    private val applicationScope: CoroutineScope,
) : ChatConnectionClient {

    /**
     * One shared subscription to the raw socket: parse each frame and apply its DB side effect exactly once.
     * Both [chatMessages] and [typingUsers] derive from this — `webSocketConnector.messages` is a cold flow,
     * so collecting it twice would open a second WebSocket session (and persist every message twice).
     */
    private val incomingMessages = webSocketConnector
        .messages
        .mapNotNull { parseIncomingMessage(it) }
        .onEach { handleIncomingMessage(it) }
        .shareIn(
            applicationScope,
            SharingStarted.WhileSubscribed(5000),
        )

    override val chatMessages = incomingMessages
        .filterIsInstance<IncomingWebSocketDto.NewMessageDto>()
        .mapNotNull {
            database.chatMessageDao.getMessageById(it.id)?.toDomain()
        }

    override val connectionState = webSocketConnector.connectionState

    /**
     * Ephemeral typing presence. Exposed in-memory — typing is a live signal, not chat content, so (unlike
     * [chatMessages]) it is never written to the database. The server already supplies the username and only
     * broadcasts to *other* participants.
     */
    override val typingUsers = incomingMessages
        .filterIsInstance<IncomingWebSocketDto.TypingIndicatorDto>()
        .map { it.toDomain() }

    /**
     * Chats the local user just lost access to. Emitted from [handleIncomingMessage] (which also deletes the
     * local chat row, so the list updates on its own) — emitting there lets us read the chat's creator before
     * the row is gone, to tell "I deleted this" apart from "an admin deleted this". Lets an open chat-detail
     * screen navigate away too.
     */
    private val _chatRemovals = MutableSharedFlow<ChatRemoval>(extraBufferCapacity = 8)
    override val chatRemovals = _chatRemovals.asSharedFlow()

    override suspend fun sendTypingStarted(chatId: String) {
        val dto = OutgoingWebSocketDto.TypingStarted(chatId)
        sendOutgoing(dto.type, json.encodeToString(dto))
    }

    override suspend fun sendTypingStopped(chatId: String) {
        val dto = OutgoingWebSocketDto.TypingStopped(chatId)
        sendOutgoing(dto.type, json.encodeToString(dto))
    }

    /**
     * Wraps an already-serialized payload in the generic [WebSocketMessageDto] envelope and transmits it.
     * Fire-and-forget: a dropped typing frame is harmless (the server's auto-stop and re-sends recover it).
     */
    private suspend fun sendOutgoing(type: OutgoingWebSocketType, payload: String) {
        val envelope = WebSocketMessageDto(
            type = type.name,
            payload = payload,
        )
        webSocketConnector.sendMessage(json.encodeToString(envelope))
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
            IncomingWebSocketType.TYPING_INDICATOR.name -> {
                json.decodeFromString<IncomingWebSocketDto.TypingIndicatorDto>(message.payload)
            }
            IncomingWebSocketType.REMOVED_FROM_CHAT.name -> {
                json.decodeFromString<IncomingWebSocketDto.RemovedFromChatDto>(message.payload)
            }
            IncomingWebSocketType.CHAT_DELETED.name -> {
                json.decodeFromString<IncomingWebSocketDto.ChatDeletedDto>(message.payload)
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
            // Typing presence is ephemeral and never persisted — surfaced via the typingUsers flow instead.
            is IncomingWebSocketDto.TypingIndicatorDto -> Unit
            // Lost access to the chat (removed by admin, or chat deleted): drop it locally; cascade cleans up.
            is IncomingWebSocketDto.RemovedFromChatDto ->
                handleChatRemoval(message.chatId, ChatRemovalReason.REMOVED_BY_ADMIN)
            is IncomingWebSocketDto.ChatDeletedDto -> handleChatDeleted(message.chatId)
        }
    }

    private suspend fun refreshChat(message: IncomingWebSocketDto.ChatParticipantsChangedDto) {
        chatRepository.fetchChatById(message.chatId)
    }

    /**
     * A chat was deleted. Decide whether the local user is the one who deleted it (they're the creator) so the
     * UI can show a "deleted successfully" confirmation rather than a "deleted by the admin" notice. The
     * creator check reads the local chat before it is removed; if the row is already gone, the local user
     * removed it via their own delete, which is also "by me".
     */
    private suspend fun handleChatDeleted(chatId: String) {
        val creatorId = database.chatDao.getChatById(chatId)?.chat?.creatorId
        val localUserId = sessionStorage.observeAuthInfo().firstOrNull()?.user?.id
        val reason = if (creatorId == null || creatorId == localUserId) {
            ChatRemovalReason.CHAT_DELETED_BY_ME
        } else {
            ChatRemovalReason.CHAT_DELETED
        }
        handleChatRemoval(chatId, reason)
    }

    private suspend fun handleChatRemoval(chatId: String, reason: ChatRemovalReason) {
        database.chatDao.deleteChatById(chatId)
        _chatRemovals.emit(ChatRemoval(chatId, reason))
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
        database.chatDao.updateLastActivity(entity.chatId, entity.timestamp)
        database.chatMessageDao.upsertMessage(entity)
    }

    private suspend fun updateProfilePicture(message: IncomingWebSocketDto.ProfilePictureUpdated) {
        database.chatParticipantDao.updateProfilePictureUrl(
            userId = message.userId,
            newUrl = message.newUrl,
        )

        val authInfo = sessionStorage.observeAuthInfo().firstOrNull()
        if (authInfo != null && authInfo.user.id == message.userId) {
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
