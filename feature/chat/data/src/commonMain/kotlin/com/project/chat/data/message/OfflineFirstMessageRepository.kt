package com.project.chat.data.message

import com.project.chat.data.dto.websocket.OutgoingWebSocketDto
import com.project.chat.data.dto.websocket.WebSocketMessageDto
import com.project.chat.data.mappers.toDomain
import com.project.chat.data.mappers.toEntity
import com.project.chat.data.mappers.toWebSocketDto
import com.project.chat.data.network.KtorWebSocketConnector
import com.project.chat.database.ChirpChatDatabase
import com.project.chat.domain.message.ChatMessageConstants
import com.project.chat.domain.message.ChatMessageService
import com.project.chat.domain.message.MessageRepository
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.domain.models.MessageWithSender
import com.project.chat.domain.models.OutgoingNewMessage
import com.project.core.data.database.safeDatabaseUpdate
import com.project.core.domain.auth.SessionStorage
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Offline-first implementation of the [MessageRepository] that communicates directly with the local database.
 *
 * ## How It Works
 * 1. Implements `updateMessageDeliveryStatus`.
 * 2. Wraps the underlying DAO call using the `safeDatabaseUpdate` inline utility to ensure local SQL exceptions do not crash the app.
 * 3. Passes the `messageId` and converts the `DeliveryStatus` enum to a string to execute the DAO update.
 */
class OfflineFirstMessageRepository(
    private val database: ChirpChatDatabase,
    private val chatMessageService: ChatMessageService,
    private val sessionStorage: SessionStorage,
    private val json: Json,
    private val webSocketConnector: KtorWebSocketConnector,
    private val applicationScope: CoroutineScope,
) : MessageRepository {

    override suspend fun sendMessage(message: OutgoingNewMessage): EmptyResult<DataError> {
        return safeDatabaseUpdate {
            val dto = message.toWebSocketDto()

            val localUser = sessionStorage.observeAuthInfo().first()?.user
                ?: return Result.Failure(DataError.Local.NOT_FOUND)

            val entity = dto.toEntity(
                senderId = localUser.id,
                deliveryStatus = ChatMessageDeliveryStatus.SENDING,
            )
            database.chatMessageDao.upsertMessage(entity)

            return webSocketConnector
                .sendMessage(dto.toJsonPayload())
                .onFailure { error ->
                    applicationScope.launch {
                        database.chatMessageDao.updateDeliveryStatus(
                            messageId = entity.messageId,
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            status = ChatMessageDeliveryStatus.FAILED.name,
                        )
                    }.join()
                }
        }
    }

    override suspend fun retryMessage(messageId: String): EmptyResult<DataError> {
        return safeDatabaseUpdate {
            println("Message ID retry $messageId")
            val message = database.chatMessageDao.getMessageById(messageId)
                ?: return Result.Failure(DataError.Local.NOT_FOUND)

            database.chatMessageDao.updateDeliveryStatus(
                messageId = messageId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                status = ChatMessageDeliveryStatus.SENDING.name,
            )

            val outgoingNewMessage = OutgoingWebSocketDto.NewMessage(
                chatId = message.chatId,
                messageId = messageId,
                content = message.content,
            )
            return webSocketConnector
                .sendMessage(outgoingNewMessage.toJsonPayload())
                .onFailure {
                    applicationScope.launch {
                        database.chatMessageDao.upsertMessage(
                            message.copy(
                                deliveryStatus = ChatMessageDeliveryStatus.FAILED.name,
                                timestamp = Clock.System.now().toEpochMilliseconds(),
                            ),
                        )
                    }.join()
                }
        }
    }

    override suspend fun deleteMessage(messageId: String): EmptyResult<DataError.Remote> {
        return chatMessageService
            .deleteMessage(messageId)
            .onSuccess {
                applicationScope.launch {
                    database.chatMessageDao.deleteMessageById(messageId)
                }.join()
            }
    }

    override suspend fun updateMessageDeliveryStatus(
        messageId: String,
        status: ChatMessageDeliveryStatus,
    ): EmptyResult<DataError.Local> {
        return safeDatabaseUpdate {
            database.chatMessageDao.updateDeliveryStatus(
                messageId = messageId,
                status = status.name,
                timestamp = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    override suspend fun fetchMessages(
        chatId: String,
        before: String?,
    ): Result<List<ChatMessage>, DataError> {
        return chatMessageService
            .fetchMessages(chatId, before)
            .onSuccess { messages ->
                return safeDatabaseUpdate {
                    database.chatMessageDao.upsertMessagesAndSyncIfNecessary(
                        chatId = chatId,
                        serverMessages = messages.map { it.toEntity() },
                        pageSize = ChatMessageConstants.PAGE_SIZE,
                        shouldSync = before == null, // Only sync for most recent page
                    )
                    messages
                }
            }
    }

    override fun getMessagesForChat(chatId: String): Flow<List<MessageWithSender>> {
        return database
            .chatMessageDao
            .getMessagesByChatId(chatId)
            .map { messages ->
                messages.map { it.toDomain() }
            }
    }

    private fun OutgoingWebSocketDto.NewMessage.toJsonPayload(): String {
        val webSocketMessage = WebSocketMessageDto(
            type = type.name,
            payload = json.encodeToString(this),
        )
        return json.encodeToString(webSocketMessage)
    }
}
