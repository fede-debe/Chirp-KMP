package com.project.chat.domain.chat

import com.project.chat.domain.models.Chat
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result

interface ChatService {
    suspend fun createChat(
        otherUserIds: List<String>,
    ): Result<Chat, DataError.Remote>

    suspend fun getChats(): Result<List<Chat>, DataError.Remote>
    suspend fun getChatById(chatId: String): Result<Chat, DataError.Remote>
    suspend fun leaveChat(chatId: String): EmptyResult<DataError.Remote>
    suspend fun removeParticipant(
        chatId: String,
        userId: String,
    ): EmptyResult<DataError.Remote>
    suspend fun addParticipantsToChat(
        chatId: String,
        userIds: List<String>,
    ): Result<Chat, DataError.Remote>
}
