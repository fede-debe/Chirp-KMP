package com.project.chat.domain.participant

import com.project.chat.domain.models.ChatParticipant
import com.project.chat.domain.models.ProfilePictureUploadUrls
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result

/**
 * Defines the contract for fetching chat participant data from the backend.
 *
 * ## Strategy / Decisions
 * - **Service Segregation:** Placed in its own service interface (`ChatParticipantService`) rather than being bundled into the primary `ChatService`.
 * - **Why:** Searching for a generic platform user to add to a chat is logically distinct from querying or modifying an existing chat resource. This segregation mirrors the backend architecture, which separates `ChatParticipantController` from `ChatController`.
 *
 * ## How It Works
 * Declares the `searchParticipant` operation, returning a domain-level `Result` wrapper containing either the found participant or a data error.
 *
 * Technical Details:
 * - Operates entirely on Domain models, abstracting away network specifics (Ktor) and JSON parsing.
 */
interface ChatParticipantService {
    suspend fun searchParticipant(
        query: String,
    ): Result<ChatParticipant, DataError.Remote>

    suspend fun getLocalParticipant(): Result<ChatParticipant, DataError.Remote>

    suspend fun getProfilePictureUploadUrl(
        mimeType: String,
    ): Result<ProfilePictureUploadUrls, DataError.Remote>

    suspend fun uploadProfilePicture(
        uploadUrl: String,
        imageBytes: ByteArray,
        headers: Map<String, String>,
    ): EmptyResult<DataError.Remote>

    suspend fun confirmProfilePictureUpload(
        publicUrl: String,
    ): EmptyResult<DataError.Remote>
}
