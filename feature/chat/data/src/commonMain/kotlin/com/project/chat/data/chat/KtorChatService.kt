package com.project.chat.data.chat

import com.project.chat.data.dto.ChatDto
import com.project.chat.data.dto.request.CreateChatRequest
import com.project.chat.data.mappers.toDomain
import com.project.chat.domain.chat.ChatService
import com.project.chat.domain.models.Chat
import com.project.core.data.networking.get
import com.project.core.data.networking.post
import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result
import com.project.core.domain.util.map
import io.ktor.client.HttpClient

/**
 * Handles all remote API communication to fetch chat data for the logged-in user.
 *
 * ## Strategy / Decisions
 * This class operates strictly as a remote data source. It is isolated from caching logic to
 * maintain a clean separation of concerns. It is responsible solely for executing the network request,
 * handling network exceptions, and parsing the JSON response into Domain models.
 *
 * ## How It Works
 * 1. Executes an authenticated HTTP GET request to the `/chat` endpoint.
 * 2. The Ktor client parses the JSON response into a list of `ChatDto` objects.
 * 3. Iterates through the DTOs and maps each to its corresponding Domain representation.
 *
 * ## Alternatives / Why Not
 * Many developers name single-source network wrappers as a "Repository" (e.g., `KtorChatRepository`).
 * The instructor explicitly rejected this naming convention, reserving the "Repository" pattern
 * strictly for classes that combine two or more data sources (e.g., local DB + remote API).
 *
 * Technical Details:
 * - Requires an attached access token containing the user ID for server-side authorization.
 *
 * @return A [Result] containing a mapped `List<Chat>` on success, or a `DataError.Remote` on failure.
 */
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

    override suspend fun getChats(): Result<List<Chat>, DataError.Remote> {
        return httpClient.get<List<ChatDto>>(
            route = "/chat",
        ).map { chatDtoList ->
            chatDtoList.map { it.toDomain() }
        }
    }

    override suspend fun getChatById(chatId: String): Result<Chat, DataError.Remote> {
        return httpClient.get<ChatDto>(
            route = "/chat/$chatId",
        ).map { it.toDomain() }
    }
}
