package com.project.chat.domain.models

/**
 * A media attachment (image today, audio later) belonging to a [ChatMessage].
 *
 * @param storageUrl Permanent public URL of the uploaded object in Supabase storage.
 * @param mimeType e.g. "image/jpeg".
 * @param fileName Original file name, shown in the file-icon fallback chip.
 * @param sizeInBytes Size of the uploaded (compressed) file.
 * @param durationInSeconds Non-null only for audio attachments (voice messages, a later task).
 */
data class MessageAttachment(
    val storageUrl: String,
    val mimeType: String,
    val fileName: String,
    val sizeInBytes: Long,
    val durationInSeconds: Int? = null,
)
