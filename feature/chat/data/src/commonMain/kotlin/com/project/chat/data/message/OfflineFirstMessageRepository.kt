package com.project.chat.data.message

import com.project.chat.data.mappers.toDomain
import com.project.chat.data.mappers.toEntity
import com.project.chat.database.ChirpChatDatabase
import com.project.chat.domain.message.ChatMessageConstants
import com.project.chat.domain.message.ChatMessageService
import com.project.chat.domain.message.MessageRepository
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.domain.models.MessageWithSender
import com.project.core.data.database.safeDatabaseUpdate
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import com.project.core.domain.util.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
) : MessageRepository {

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
}
