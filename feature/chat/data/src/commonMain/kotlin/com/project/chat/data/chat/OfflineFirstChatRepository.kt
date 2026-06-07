package com.project.chat.data.chat

import com.project.chat.data.lifecycle.AppLifecycleObserver
import com.project.chat.data.mappers.toDomain
import com.project.chat.data.mappers.toEntity
import com.project.chat.data.mappers.toLastMessageView
import com.project.chat.database.ChirpChatDatabase
import com.project.chat.database.entities.ChatInfoEntity
import com.project.chat.database.entities.ChatParticipantEntity
import com.project.chat.database.entities.ChatWithParticipants
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.domain.chat.ChatService
import com.project.chat.domain.models.Chat
import com.project.chat.domain.models.ChatInfo
import com.project.chat.domain.models.ChatParticipant
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import com.project.core.domain.util.asEmptyResult
import com.project.core.domain.util.onSuccess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.supervisorScope

/**
 * Coordinates between the KtorChatService and the local ChatDatabase to enforce an offline-first architecture.
 *
 * ## Strategy / Decisions
 * Implements the "Single Source of Truth" strategy. Data displayed in the app is exclusively read from
 * the local SQLite database. Network responses are never piped directly to the UI; they are only used
 * to update the local database cache. This provides immediate real-time UI updates (via Flow) regardless
 * of the screen the user is on.
 *
 * ## How It Works
 * 1. [getChats]: Listens to the local database for all active participants and maps the emitted entities to Domain models.
 * 2. It filters out chats where the active participant size is less than 1 (though keeps chats where the user is the only remaining participant, as others can be added later).
 * 3. [fetchChats]: Calls the Ktor service. On success, it maps the remote Domain models into Database Entities.
 * 4. Pushes the newly mapped entities into the DAO for an upsert operation.
 *
 * ## Alternatives / Why Not
 * Complex flow management (registering/unregistering collectors based on network state) was avoided.
 * Relying strictly on the database as the emitter removes the need to track UI states manually.
 *
 * Technical Details:
 * - Combines two dependencies: `ChatService` (Remote) and `ChatDatabase` (Local).
 * - Caches incoming 'last messages' explicitly as null if not provided by the API, defaulting to Room's internal population strategy.
 */

/**
 * Offline-first repository managing chat data, including chat deletion and
 * manual participant filtering to work around Room limitations.
 * * ## Strategy / Decisions
 * - **Offline-First Chat Deletion:** When a user successfully leaves a chat remotely,
 * the local chat is simply deleted from the DB. A cascading foreign key automatically
 * cleans up related participants and messages.
 * - **Room Embedded Entity Workaround:** Room completely ignores `JOIN` and `WHERE`
 * clauses when returning custom classes with embedded entities (like `ChatWithParticipants`).
 * It performs the join under the hood and returns *all* participants regardless of active status.
 * To fix this, the repository fetches all chats with all participants, then manually
 * filters out the inactive ones in the mapping layer.
 * - **Parallel Sub-queries:** When filtering active participants for a list of chats,
 * fetching active participant IDs is executed in parallel using `async` blocks
 * within a `supervisorScope`. This speeds up the mapping process compared to sequential suspending calls.
 * * ## How It Works
 * **Leaving a Chat:**
 * 1. Calls `leaveChat` on the remote service.
 * 2. On success, calls `dbChatDao.deleteChatById(chatId)`.
 * 3. Cascade constraints handle remaining local cleanup.
 * * **Fetching Chats & Filtering Participants:**
 * 1. Queries Room for all chats with their participants (active and inactive).
 * 2. Maps the Flow emission.
 * 3. For lists: Opens a `supervisorScope`. Maps each chat using an `async` block to fetch
 * active participant IDs via `dbChatDao.getActiveParticipantsByChatId`.
 * 4. Awaits all async results (`awaitAll()`) and filters the participant list where
 * `userId` is in the fetched active IDs.
 * 5. Maps the strictly filtered local entities to Domain objects.
 * 6. For single chat queries: Applies the exact same manual filtering logic without `async`,
 * as it only processes a single entity.
 * * ## Alternatives / Why Not
 * - **Manual Cursors:** Considered manually running a cursor over the resulting rows and
 * constructing the `ChatWithParticipants` object manually, but this was rejected because
 * it would become a pain to maintain.
 * - **Relying on Room's WHERE clause:** Rejected because Room ignores `WHERE` clauses
 * for relational embedded entities unless returning a normal, single-table entity.
 * * Technical Details:
 * - Employs `supervisorScope` and `async` / `awaitAll` for thread pooling and parallel DB fetches.
 * - Relies on SQLite Foreign Key `CASCADE` for cleanup.
 * - Flow operators: `map`, `first()` (to get a one-off result from the active participant query).
 */
@OptIn(DelicateCoroutinesApi::class)
class OfflineFirstChatRepository(
    private val chatService: ChatService,
    private val db: ChirpChatDatabase,
    private val observer: AppLifecycleObserver,
) : ChatRepository {

    init {
        observer.isInForeground.onEach { isInForeground ->
            println("Is app in foreground: $isInForeground")
        }.launchIn(GlobalScope)
    }

    override fun getChats(): Flow<List<Chat>> {
        return db.chatDao.getChatsWithParticipants()
            .map { allChatsWithParticipants ->
                supervisorScope {
                    allChatsWithParticipants
                        .map { chatWithParticipants ->
                            async {
                                ChatWithParticipants(
                                    chat = chatWithParticipants.chat,
                                    participants = chatWithParticipants
                                        .participants
                                        .onlyActive(chatWithParticipants.chat.chatId),
                                    lastMessage = chatWithParticipants.lastMessage,
                                )
                            }
                        }
                        .awaitAll()
                        .map { it.toDomain() }
                }
            }
    }

    override fun getChatInfoById(chatId: String): Flow<ChatInfo> {
        return db.chatDao.getChatInfoById(chatId)
            .filterNotNull()
            .map { chatInfo ->
                ChatInfoEntity(
                    chat = chatInfo.chat,
                    participants = chatInfo
                        .participants
                        .onlyActive(chatInfo.chat.chatId),
                    messagesWithSenders = chatInfo.messagesWithSenders,
                )
            }
            .map { it.toDomain() }
    }

    override fun getActiveParticipantsByChatId(chatId: String): Flow<List<ChatParticipant>> {
        return db.chatDao.getActiveParticipantsByChatId(chatId)
            .map { participants ->
                participants.map { it.toDomain() }
            }
    }

    override suspend fun fetchChats(): Result<List<Chat>, DataError.Remote> {
        return chatService
            .getChats()
            .onSuccess { chats ->
                val chatsWithParticipants = chats.map { chat ->
                    ChatWithParticipants(
                        chat = chat.toEntity(),
                        participants = chat.participants.map { it.toEntity() },
                        lastMessage = chat.lastMessage?.toLastMessageView(),
                    )
                }

                db.chatDao.upsertChatsWithParticipantsAndCrossRefs(
                    chats = chatsWithParticipants,
                    participantDao = db.chatParticipantDao,
                    crossRefDao = db.chatParticipantsCrossRefDao,
                    messageDao = db.chatMessageDao,
                )
            }
    }

    override suspend fun fetchChatById(chatId: String): EmptyResult<DataError.Remote> {
        return chatService
            .getChatById(chatId)
            .onSuccess { chat ->
                db.chatDao.upsertChatWithParticipantsAndCrossRefs(
                    chat = chat.toEntity(),
                    participants = chat.participants.map { it.toEntity() },
                    participantDao = db.chatParticipantDao,
                    crossRefDao = db.chatParticipantsCrossRefDao,
                )
            }
            .asEmptyResult()
    }

    override suspend fun createChat(otherUserIds: List<String>): Result<Chat, DataError.Remote> {
        return chatService
            .createChat(otherUserIds)
            .onSuccess { chat ->
                db.chatDao.upsertChatWithParticipantsAndCrossRefs(
                    chat = chat.toEntity(),
                    participants = chat.participants.map { it.toEntity() },
                    participantDao = db.chatParticipantDao,
                    crossRefDao = db.chatParticipantsCrossRefDao,
                )
            }
    }

    override suspend fun leaveChat(chatId: String): EmptyResult<DataError.Remote> {
        return chatService
            .leaveChat(chatId)
            .onSuccess {
                db.chatDao.deleteChatById(chatId)
            }
    }

    override suspend fun addParticipantsToChat(
        chatId: String,
        userIds: List<String>,
    ): Result<Chat, DataError.Remote> {
        return chatService
            .addParticipantsToChat(chatId, userIds)
            .onSuccess { chat ->
                db.chatDao.upsertChatWithParticipantsAndCrossRefs(
                    chat = chat.toEntity(),
                    participants = chat.participants.map { it.toEntity() },
                    participantDao = db.chatParticipantDao,
                    crossRefDao = db.chatParticipantsCrossRefDao,
                )
            }
    }

    private suspend fun List<ChatParticipantEntity>.onlyActive(chatId: String): List<ChatParticipantEntity> {
        val activeParticipantIds = db
            .chatDao
            .getActiveParticipantsByChatId(chatId)
            .first()
            .map { it.userId }

        return this.filter { it.userId in activeParticipantIds }
    }
}
