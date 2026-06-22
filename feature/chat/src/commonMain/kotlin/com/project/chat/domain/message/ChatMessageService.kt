package com.project.chat.domain.message

import com.project.chat.domain.models.ChatMessage
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result

interface ChatMessageService {
    suspend fun fetchMessages(
        chatId: String,
        before: String? = null,
    ): Result<List<ChatMessage>, DataError.Remote>

    suspend fun deleteMessage(messageId: String): EmptyResult<DataError.Remote>
}
