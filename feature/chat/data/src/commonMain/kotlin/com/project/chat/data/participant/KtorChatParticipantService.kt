package com.project.chat.data.participant

import com.project.chat.data.dto.ChatParticipantDto
import com.project.chat.data.mappers.toDomain
import com.project.chat.domain.models.ChatParticipant
import com.project.chat.domain.participant.ChatParticipantService
import com.project.core.data.networking.get
import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result
import com.project.core.domain.util.map
import io.ktor.client.HttpClient

/**
 * Ktor-based implementation of the `ChatParticipantService` responsible for executing HTTP requests to the participants API.
 *
 * ## Strategy / Decisions
 * - Uses the injected application-wide Ktor `HttpClient`.
 * - Relies on custom HTTP client extension functions (from `core-data`) to streamline standard GET requests and error handling.
 *
 * ## How It Works
 * 1. Takes the user's search query and maps it to a `query` parameter inside a parameter map.
 * 2. Executes a GET request against the relative `/participants` route.
 * 3. Expects a JSON response mapped to a `ChatParticipantDto`.
 * 4. Utilizes a mapper function to convert the resulting DTO into the domain-level `ChatParticipant` model before returning.
 */
class KtorChatParticipantService(
    private val httpClient: HttpClient,
) : ChatParticipantService {

    override suspend fun searchParticipant(query: String): Result<ChatParticipant, DataError.Remote> {
        return httpClient.get<ChatParticipantDto>(
            route = "/participants",
            queryParams = mapOf(
                "query" to query,
            ),
        ).map { dto -> dto.toDomain() }
    }

    override suspend fun getLocalParticipant(): Result<ChatParticipant, DataError.Remote> {
        return httpClient.get<ChatParticipantDto>(
            route = "/participants",
        ).map { it.toDomain() }
    }
}
