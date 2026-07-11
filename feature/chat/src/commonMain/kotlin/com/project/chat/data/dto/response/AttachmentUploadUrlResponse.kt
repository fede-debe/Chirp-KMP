package com.project.chat.data.dto.response

import kotlinx.serialization.Serializable

/**
 * Signed upload URL for a single message attachment. Mirrors the profile-picture upload-url response.
 */
@Serializable
data class AttachmentUploadUrlResponse(
    val uploadUrl: String,
    val publicUrl: String,
    val headers: Map<String, String>,
)
