package com.project.chat.data.message

import com.project.chat.data.dto.ChatMessageDto
import com.project.chat.data.mappers.toDomain
import com.project.chat.domain.message.ChatMessageConstants
import com.project.chat.domain.message.ChatMessageService
import com.project.chat.domain.models.ChatMessage
import com.project.core.data.networking.delete
import com.project.core.data.networking.get
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import com.project.core.domain.util.map
import io.ktor.client.HttpClient

class KtorChatMessageService(
    private val httpClient: HttpClient,
) : ChatMessageService {

    override suspend fun deleteMessage(messageId: String): EmptyResult<DataError.Remote> {
        return httpClient.delete(
            route = "/messages/$messageId",
        )
    }

    override suspend fun fetchMessages(
        chatId: String,
        before: String?,
    ): Result<List<ChatMessage>, DataError.Remote> {
        return httpClient.get<List<ChatMessageDto>>(
            route = "/chat/$chatId/messages",
            queryParams = buildMap {
                this["pageSize"] = ChatMessageConstants.PAGE_SIZE
                if (before != null) {
                    this["before"] = before
                }
            },
        ).map { it.map { it.toDomain() } }
    }
}
