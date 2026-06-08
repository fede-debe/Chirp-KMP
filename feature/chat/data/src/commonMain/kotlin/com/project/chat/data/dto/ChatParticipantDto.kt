package com.project.chat.data.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object representing the JSON payload returned by the participant search API.
 *
 * ## Strategy / Decisions
 * - **Strict Contract:** Exact property naming matters here to ensure Kotlinx Serialization correctly parses the incoming JSON payload from the backend (requires matching fields: user ID, username, profile picture URL).
 *
 * Technical Details:
 * - Must be annotated with `@Serializable` to prevent JSON parsing crashes.
 */
@Serializable
data class ChatParticipantDto(
    val userId: String,
    val username: String,
    val profilePictureUrl: String?,
)
