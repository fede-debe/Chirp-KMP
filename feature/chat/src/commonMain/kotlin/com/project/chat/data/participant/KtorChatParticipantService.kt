package com.project.chat.data.participant

import com.project.chat.data.dto.ChatParticipantDto
import com.project.chat.data.dto.request.ConfirmProfilePictureRequest
import com.project.chat.data.dto.response.ProfilePictureUploadUrlsResponse
import com.project.chat.data.mappers.toDomain
import com.project.chat.domain.models.ChatParticipant
import com.project.chat.domain.models.ProfilePictureUploadUrls
import com.project.chat.domain.participant.ChatParticipantService
import com.project.core.data.networking.delete
import com.project.core.data.networking.get
import com.project.core.data.networking.post
import com.project.core.data.networking.put
import com.project.core.data.networking.safeCall
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import com.project.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url

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
        ).map { it.toDomain() }
    }

    override suspend fun getLocalParticipant(): Result<ChatParticipant, DataError.Remote> {
        return httpClient.get<ChatParticipantDto>(
            route = "/participants",
        ).map { it.toDomain() }
    }

    override suspend fun getProfilePictureUploadUrl(mimeType: String): Result<ProfilePictureUploadUrls, DataError.Remote> {
        return httpClient.post<Unit, ProfilePictureUploadUrlsResponse>(
            route = "/participants/profile-picture-upload",
            queryParams = mapOf(
                "mimeType" to mimeType,
            ),
            body = Unit,
        ).map { it.toDomain() }
    }

    override suspend fun uploadProfilePicture(
        uploadUrl: String,
        imageBytes: ByteArray,
        headers: Map<String, String>,
    ): EmptyResult<DataError.Remote> {
        return safeCall {
            httpClient.put {
                url(uploadUrl)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                setBody(imageBytes)
            }
        }
    }

    override suspend fun confirmProfilePictureUpload(publicUrl: String): EmptyResult<DataError.Remote> {
        return httpClient.post<ConfirmProfilePictureRequest, Unit>(
            route = "/participants/confirm-profile-picture",
            body = ConfirmProfilePictureRequest(publicUrl),
        )
    }

    override suspend fun deleteProfilePicture(): EmptyResult<DataError.Remote> {
        return httpClient.delete(
            route = "/participants/profile-picture",
        )
    }
}
