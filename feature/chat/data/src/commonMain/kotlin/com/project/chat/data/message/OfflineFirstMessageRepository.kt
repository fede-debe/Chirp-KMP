package com.project.chat.data.message

import com.project.chat.database.ChirpChatDatabase
import com.project.chat.domain.message.MessageRepository
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.core.data.database.safeDatabaseUpdate
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
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
}
