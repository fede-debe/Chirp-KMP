package com.project.chat.data.dto

import kotlinx.serialization.Serializable

/**
 * Attachment as it appears on a message coming back from the backend (and as persisted to the local
 * DB as JSON). Mirrors the backend `ChatAttachmentDto` contract.
 */
@Serializable
data class ChatAttachmentDto(
    val storageUrl: String,
    val mimeType: String,
    val originalFileName: String,
    val sizeInBytes: Long,
    val durationInSeconds: Int? = null,
)
