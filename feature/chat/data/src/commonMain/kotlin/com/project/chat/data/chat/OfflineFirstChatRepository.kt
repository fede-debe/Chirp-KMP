package com.project.chat.data.chat

import com.project.chat.data.mappers.toDomain
import com.project.chat.data.mappers.toEntity
import com.project.chat.data.mappers.toLastMessageView
import com.project.chat.database.ChirpChatDatabase
import com.project.chat.database.entities.ChatWithParticipants
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.domain.chat.ChatService
import com.project.chat.domain.models.Chat
import com.project.chat.domain.models.ChatInfo
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import com.project.core.domain.util.asEmptyResult
import com.project.core.domain.util.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

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
class OfflineFirstChatRepository(
    private val chatService: ChatService,
    private val db: ChirpChatDatabase,
) : ChatRepository {

    override fun getChats(): Flow<List<Chat>> {
        return db.chatDao.getChatsWithActiveParticipants()
            .map { chatWithParticipantsList ->
                chatWithParticipantsList.map { it.toDomain() }
            }
    }

    override fun getChatInfoById(chatId: String): Flow<ChatInfo> {
        return db.chatDao.getChatInfoById(chatId)
            .filterNotNull()
            .map { it.toDomain() }
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
}
