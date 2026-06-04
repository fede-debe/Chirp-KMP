package com.project.chat.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.project.chat.database.entities.ChatEntity
import com.project.chat.database.entities.ChatInfoEntity
import com.project.chat.database.entities.ChatMessageEntity
import com.project.chat.database.entities.ChatParticipantCrossRef
import com.project.chat.database.entities.ChatParticipantEntity
import com.project.chat.database.entities.ChatWithParticipants
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface defining the interactions with the chat table.
 * It serves as the primary entry point for querying, inserting, updating, and deleting chat entities and their relations.
 *
 * ## Strategy / Decisions
 * - **Upsert over Insert:** Relying on `@Upsert` rather than standard `@Insert` annotations to automatically handle updates if a chat ID already exists. This removes the need for separate existence checks and custom update logic.
 * - **Reactive Data Layer:** Utilizes Kotlin `Flow` for read operations (e.g., fetching participants or chat counts). This makes the database observable, ensuring the UI remains automatically synchronized; any local data change (like a participant's active status) instantly emits a new state to the app.
 * - **Batched vs. Single Operations:** Implements both single (`upsertChat`) and list-based (`upsertAllChats`) functions to handle targeted data refreshes (fetching the latest details for a single clicked chat) versus bulk payload saving (loading all chats for the user).
 * - **Offline-First Logout Handling:** Includes a `deleteAllChats` function that clears the table. Due to cascading delete foreign keys, deleting chats automatically drops associated messages and cross-references, securely wiping the populated DB instance when a user logs out.
 *
 * ## How It Works
 * 1. The interface is annotated with Room's `@Dao`, letting Room generate the concrete SQLite implementations.
 * 2. Basic CRUD operations execute via Room's ready-made annotations (`@Upsert`, `@Query`, `@Delete`).
 * 3. Relational queries mapping to classes like `ChatWithParticipants` or `ChatInfoEntity` are resolved natively by Room, relying on the `@Relation` and `@Embedded` tags defined in those model classes.
 * 4. The `getActiveParticipantsByChatId` flow explicitly joins `ChatParticipantEntity` (aliased as `p`) with `ChatParticipantCrossRef` (aliased as `cpcr`) to filter participants based on the specific `chatId` and their `isActive` boolean flag, sorting them alphabetically.
 * 5. Iterative functions like `deleteChatsByIds` loop through a provided list of IDs and call `deleteChatById` on each.
 *
 * ## Alternatives / Why Not
 * - **Why not use `@Insert`?** It would require manual conflict resolution or separate update checks. `@Upsert` safely abstracts this away.
 * - **Why not cascade delete participants?** While messages and cross-references cascade on chat deletion, participants do not. A participant can be part of multiple chats, so deleting one chat must not purge the participant from the local DB entirely.
 * - **Why use a manual loop instead of a bulk query for `deleteChatsByIds`?** Iterating over the list allows for fine-grained control, but it is strictly wrapped in a `@Transaction`. This was chosen to guarantee atomicity (rolling back if one delete fails, preventing DB inconsistency) while still maintaining performance comparable to a single query execution.
 *
 * Technical Details:
 * - Offline-first architecture constraint: DB clearance is mandatory on logout to prevent state leakage to other users.
 * - Multiline custom SQL queries use Kotlin triple-quotes and leverage explicit table aliases for readability in complex JOINs.
 * - `@Transaction` is mandatory on loop-based operations to run all queries under a single database connection.
 *
 * @param chatId The unique string identifier for a target chat.
 * @param chatIds A list of string identifiers for bulk deletion operations.
 * @return Returns Kotlin `Flow` objects for observable data streams, or custom relational models like `ChatInfoEntity` and `ChatWithParticipants`.
 */

/**
 * Data Access Object for handling chat-related database queries.
 * * ## Strategy / Decisions
 * Functions that return bundled data (a data class holding a relation) must be annotated
 * with `@Transaction`. This ensures atomic read operations and prevents inconsistent results
 * if underlying tables change while the relation mapping is being processed.
 * * Technical Details:
 * - Fixed a KSP compilation failure ("unused parameter") by ensuring the `chatId` parameter
 * was explicitly bound in the query's `WHERE` clause.
 */
@Dao
interface ChatDao {

    @Upsert
    suspend fun upsertChat(chat: ChatEntity)

    @Upsert
    suspend fun upsertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chatentity WHERE chatId = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Query("SELECT * FROM chatentity ORDER BY lastActivityAt DESC")
    @Transaction
    fun getChatsWithParticipants(): Flow<List<ChatWithParticipants>>

    @Query(
        """
        SELECT DISTINCT c.*
        FROM chatentity c
        JOIN chatparticipantcrossref cpcr ON c.chatId = cpcr.chatId
         WHERE cpcr.isActive = 1
         ORDER BY lastActivityAt DESC
    """,
    )
    @Transaction
    fun getChatsWithActiveParticipants(): Flow<List<ChatWithParticipants>>

    @Query("SELECT * FROM chatentity WHERE chatId = :id")
    @Transaction
    suspend fun getChatById(id: String): ChatWithParticipants?

    @Query("DELETE FROM chatentity")
    suspend fun deleteAllChats()

    @Query("SELECT chatId FROM chatentity")
    suspend fun getAllChatIds(): List<String>

    @Transaction
    suspend fun deleteChatsByIds(chatIds: List<String>) {
        chatIds.forEach { chatId ->
            deleteChatById(chatId)
        }
    }

    @Query("SELECT COUNT(*) FROM chatentity")
    fun getChatCount(): Flow<Int>

    @Query(
        """
        SELECT p.*
        FROM chatparticipantentity p
        JOIN chatparticipantcrossref cpcr ON p.userId = cpcr.userId
        WHERE cpcr.chatId = :chatId AND cpcr.isActive = true
        ORDER BY p.username
    """,
    )
    fun getActiveParticipantsByChatId(chatId: String): Flow<List<ChatParticipantEntity>>

    @Query(
        """
        SELECT c.*
        FROM chatentity c
        JOIN chatparticipantcrossref cpcr ON c.chatId = cpcr.chatId
        WHERE c.chatId = :chatId AND cpcr.isActive = true
    """,
    )
    @Transaction
    fun getChatInfoById(chatId: String): Flow<ChatInfoEntity?>

    @Transaction
    suspend fun upsertChatWithParticipantsAndCrossRefs(
        chat: ChatEntity,
        participants: List<ChatParticipantEntity>,
        participantDao: ChatParticipantDao,
        crossRefDao: ChatParticipantsCrossRefDao,
    ) {
        upsertChat(chat)
        participantDao.upsertParticipants(participants)

        val crossRefs = participants.map {
            ChatParticipantCrossRef(
                chatId = chat.chatId,
                userId = it.userId,
                isActive = true,
            )
        }
        crossRefDao.upsertCrossRefs(crossRefs)
        crossRefDao.syncChatParticipants(chat.chatId, participants)
    }

    @Transaction
    suspend fun upsertChatsWithParticipantsAndCrossRefs(
        chats: List<ChatWithParticipants>,
        participantDao: ChatParticipantDao,
        crossRefDao: ChatParticipantsCrossRefDao,
        messageDao: ChatMessageDao,
    ) {
        upsertChats(chats.map { it.chat })

        val serverChatIds = chats.map { it.chat.chatId }
        val localChatIds = getAllChatIds()
        val staleChatIds = localChatIds - serverChatIds

        chats.forEach { chat ->
            chat.lastMessage?.run {
                messageDao.upsertMessage(
                    ChatMessageEntity(
                        messageId = messageId,
                        chatId = chatId,
                        senderId = senderId,
                        content = content,
                        timestamp = timestamp,
                        deliveryStatus = deliveryStatus,
                    ),
                )
            }
        }

        val allParticipants = chats.flatMap { it.participants }
        participantDao.upsertParticipants(allParticipants)

        val allCrossRefs = chats.flatMap { chatWithParticipants ->
            chatWithParticipants.participants.map { participant ->
                ChatParticipantCrossRef(
                    chatId = chatWithParticipants.chat.chatId,
                    userId = participant.userId,
                    isActive = true,
                )
            }
        }
        crossRefDao.upsertCrossRefs(allCrossRefs)

        chats.forEach { chat ->
            crossRefDao.syncChatParticipants(
                chatId = chat.chat.chatId,
                participants = chat.participants,
            )
        }

        deleteChatsByIds(staleChatIds)
    }
}
