package com.project.chat.domain.chat

import com.project.chat.domain.models.Chat
import com.project.chat.domain.models.ChatInfo
import com.project.chat.domain.models.ChatParticipant
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for chat data management, combining local database and remote API sources.
 *
 * ## Strategy / Decisions
 * This interface establishes the foundation for the "Offline-First" and "Single Source of Truth"
 * principles. By splitting the read (`getChats`) and write/sync (`fetchChats`) operations, the UI
 * is decoupled from network constraints.
 *
 * ## How It Works
 * 1. [getChats] provides a continuous stream of data directly from the local database.
 * 2. [fetchChats] acts as a synchronization trigger that mutates the database.
 * 3. Once [fetchChats] updates the database, the flow from [getChats] automatically emits the new state.
 *
 * ## Alternatives / Why Not
 * Returning the API response directly to the UI from [fetchChats] was rejected. Doing so would
 * violate the Single Source of Truth principle, leading to inconsistent states and race conditions
 * where the UI might show remote data that hasn't been cached yet.
 *
 * Technical Details:
 * - Depends on `kotlinx.coroutines.flow.Flow` for reactive data streams.
 *
 * @see getChats
 * @see fetchChats
 */
interface ChatRepository {
    fun getChats(): Flow<List<Chat>>
    fun getChatInfoById(chatId: String): Flow<ChatInfo>
    fun getActiveParticipantsByChatId(chatId: String): Flow<List<ChatParticipant>>
    suspend fun fetchChats(): Result<List<Chat>, DataError.Remote>
    suspend fun fetchChatById(chatId: String): EmptyResult<DataError.Remote>
    suspend fun createChat(otherUserIds: List<String>): Result<Chat, DataError.Remote>
    suspend fun leaveChat(chatId: String): EmptyResult<DataError.Remote>
    suspend fun addParticipantsToChat(
        chatId: String,
        userIds: List<String>,
    ): Result<Chat, DataError.Remote>
    suspend fun deleteAllChats()
}
