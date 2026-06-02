package com.project.chat.data.chat

import com.project.chat.data.dto.ChatDto
import com.project.chat.data.dto.request.CreateChatRequest
import com.project.chat.data.mappers.toDomain
import com.project.chat.domain.chat.ChatService
import com.project.chat.domain.models.Chat
import com.project.core.data.networking.post
import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result
import com.project.core.domain.util.map
import io.ktor.client.HttpClient

class KtorChatService(
    private val httpClient: HttpClient,
) : ChatService {

    override suspend fun createChat(otherUserIds: List<String>): Result<Chat, DataError.Remote> {
        return httpClient.post<CreateChatRequest, ChatDto>(
            route = "/chat",
            body = CreateChatRequest(
                otherUserIds = otherUserIds,
            ),
        ).map { dto -> dto.toDomain() }
    }
}
