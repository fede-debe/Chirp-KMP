package com.project.chat.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.project.chat.database.entities.ChatParticipantCrossRef
import com.project.chat.database.entities.ChatParticipantEntity

/**
 * Manages the cross-reference table associating chats with their respective participants.
 * It tracks and updates the active or inactive status of users within specific chats.
 *
 * ## Strategy / Decisions
 * - **Single Source of Truth:** The server response is treated as the absolute truth for participant status.
 * - **Encapsulation:** Complex database synchronization logic is kept entirely inside the DAO. This prevents UI components, ViewModels, or Repositories from having to manage raw database state and cross-table referential integrity.
 *
 * ## How It Works
 * 1. Checks if the provided server participants list is empty; returns early if there is nothing to sync.
 * 2. Extracts and converts server participant IDs into a `Set` to eliminate duplicates.
 * 3. Queries the database for all local participant IDs, as well as specifically active local participant IDs for the chat.
 * 4. Identifies participants to **reactivate** by intersecting server IDs with inactive local IDs (updating `isActive` to true).
 * 5. Identifies participants to **deactivate** by subtracting server IDs from active local IDs (updating `isActive` to false).
 * 6. Identifies **completely new** participants by subtracting all local IDs from server IDs, inserting them as brand-new cross-reference rows.
 *
 * ## Technical Details
 * - **Transactions:** Custom synchronization functions are annotated with Room's `@Transaction` to ensure that all read, update, and insert operations succeed or fail as a single atomic unit.
 * - **Collections:** Leverages Kotlin `Set` operations (`intersect`, `minus`) for clean and efficient data comparisons.
 *
 * @param chatId The unique identifier of the chat being synchronized.
 * @param participants The list of participant entities loaded from the server to synchronize against the local cache.
 */
@Dao
interface ChatParticipantsCrossRefDao {

    @Upsert
    suspend fun upsertCrossRefs(crossRefs: List<ChatParticipantCrossRef>)

    @Query("SELECT userId FROM chatparticipantcrossref WHERE chatId = :chatId")
    suspend fun getActiveParticipantIdsByChat(chatId: String): List<String>

    @Query("SELECT userId FROM chatparticipantcrossref WHERE chatId = :chatId")
    suspend fun getAllParticipantIdsByChat(chatId: String): List<String>

    @Query(
        """
        UPDATE chatparticipantcrossref
        SET isActive = 0
        WHERE chatId = :chatId AND userId IN (:userIds)
    """,
    )
    suspend fun markParticipantsAsInactive(chatId: String, userIds: List<String>)

    @Query(
        """
        UPDATE chatparticipantcrossref
        SET isActive = 1
        WHERE chatId = :chatId AND userId IN (:userIds)
    """,
    )
    suspend fun reactivateParticipants(chatId: String, userIds: List<String>)

    @Transaction
    suspend fun syncChatParticipants(
        chatId: String,
        participants: List<ChatParticipantEntity>,
    ) {
        if (participants.isEmpty()) {
            return
        }

        val serverParticipantIds = participants.map { it.userId }.toSet()
        val allLocalParticipantIds = getAllParticipantIdsByChat(chatId).toSet()
        val activeLocalParticipantIds = getActiveParticipantIdsByChat(chatId).toSet()
        val inactiveLocalParticipantIds = allLocalParticipantIds - activeLocalParticipantIds

        val participantsToReactivate = serverParticipantIds.intersect(inactiveLocalParticipantIds)
        val participantsToDeactivate = activeLocalParticipantIds - serverParticipantIds

        reactivateParticipants(chatId, participantsToReactivate.toList())
        markParticipantsAsInactive(chatId, participantsToDeactivate.toList())

        val completelyNewParticipantIds = serverParticipantIds - allLocalParticipantIds
        val newCrossRefs = completelyNewParticipantIds.map { userId ->
            ChatParticipantCrossRef(
                chatId = chatId,
                userId = userId,
                isActive = true,
            )
        }
        upsertCrossRefs(newCrossRefs)
    }
}
