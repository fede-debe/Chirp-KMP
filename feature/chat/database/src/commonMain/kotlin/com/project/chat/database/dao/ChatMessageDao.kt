package com.project.chat.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.project.chat.database.entities.ChatMessageEntity
import com.project.chat.database.entities.MessageWithSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Interface for the ChatMessage Data Access Object (DAO).
 * A lightweight DAO providing standard CRUD functionality to locally cache chat messages.
 *
 * ## Strategy / Decisions
 * - **Local Caching:** Designed to locally cache messages in the database immediately upon sending or receiving, ensuring data is available and persisted.
 * - **Reactive UI:** Uses `Flow` for retrieving messages for a specific chat. This allows the UI to actively observe database changes and immediately reflect newly inserted messages without manual polling.
 * - **Sorting:** Automatically orders chat messages by timestamp descending directly within the query so the most recent messages are always surfaced first.
 *
 * ## How It Works
 * 1. Provides `suspend` functions for one-off database writes (inserting a single message upon sending, updating multiple messages).
 * 2. Exposes query functions to delete single or multiple messages based on their IDs.
 * 3. Exposes an observable `Flow` for reading all messages tied to a specific `chatId`, handling the real-time UI updates.
 * 4. Retrieves one-off single messages by their ID when needed.
 *
 * ## Alternatives / Why Not
 * - **Deleting Multiple Items:** To delete multiple messages by their IDs, this DAO utilizes a single SQL query checking if the message ID is contained in the provided list (`messageId IN (:messageIds)`). The alternative (used previously in the `ChatDao`) is iterating over the list manually inside a `@Transaction`. The `IN` query approach is chosen here to demonstrate an equivalent, standard SQL-level alternative.
 *
 * Technical Details:
 * - `getMessagesByChatId` returns a `Flow` and therefore must *not* be a `suspend` function.
 * - `insertSingleMessage` operates as an "upsert" to safely handle the insertion of a single chat message entity.
 */
@Dao
interface ChatMessageDao {

    @Upsert
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Upsert
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chatmessageentity WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM chatmessageentity WHERE messageId IN (:messageIds)")
    suspend fun deleteMessagesById(messageIds: List<String>)

    @Query("SELECT * FROM chatmessageentity WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getMessagesByChatId(chatId: String): Flow<List<MessageWithSender>>

    @Query(
        """
        SELECT *
        FROM chatmessageentity
        WHERE chatId = :chatId
        ORDER BY timestamp DESC
        LIMIT :limit
    """,
    )
    fun getMessagesByChatIdLimited(chatId: String, limit: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chatmessageentity WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Query(
        """
        UPDATE chatmessageentity
        SET deliveryStatus = :status, deliveryStatusTimestamp = :timestamp
        WHERE messageId = :messageId
    """,
    )
    suspend fun updateDeliveryStatus(messageId: String, status: String, timestamp: Long)

    @Transaction
    suspend fun upsertMessagesAndSyncIfNecessary(
        chatId: String,
        serverMessages: List<ChatMessageEntity>,
        pageSize: Int,
        shouldSync: Boolean = false,
    ) {
        val localMessages = getMessagesByChatIdLimited(
            chatId = chatId,
            limit = pageSize,
        ).first()

        upsertMessages(serverMessages)

        if (!shouldSync) {
            return
        }

        val serverIds = serverMessages.map { it.messageId }.toSet()

        val messagesToDelete = localMessages.filter { localMessage ->
            val missingOnServer = localMessage.messageId !in serverIds
            val isSent = localMessage.deliveryStatus == "SENT"

            missingOnServer && isSent
        }

        val messageIds = messagesToDelete.map { it.messageId }
        deleteMessagesById(messageIds)
    }
}
