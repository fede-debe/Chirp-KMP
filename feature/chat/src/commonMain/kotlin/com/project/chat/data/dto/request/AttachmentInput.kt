package com.project.chat.data.dto.request

import kotlinx.serialization.Serializable

/**
 * Attachment reference sent to the backend when posting a new message, after the bytes have already
 * been uploaded to Supabase. Mirrors the backend `AttachmentInput` contract.
 */
@Serializable
data class AttachmentInput(
    val storageUrl: String,
    val mimeType: String,
    val originalFileName: String,
    val sizeInBytes: Long,
    val durationInSeconds: Int? = null,
)
