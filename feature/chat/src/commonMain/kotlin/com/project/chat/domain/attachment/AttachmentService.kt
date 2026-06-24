package com.project.chat.domain.attachment

import com.project.chat.domain.models.MessageAttachment
import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result

/**
 * Uploads a single (already-processed) attachment — image or audio — to storage and returns the
 * [MessageAttachment] reference to attach to an outgoing message.
 *
 * ## How It Works
 * 1. Requests a Supabase signed upload URL from the backend for the given chat + mime type.
 * 2. `PUT`s the bytes directly to that URL.
 * 3. Returns a [MessageAttachment] pointing at the permanent public URL.
 */
interface AttachmentService {
    suspend fun uploadAttachment(
        chatId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        durationInSeconds: Int? = null,
    ): Result<MessageAttachment, DataError.Remote>

    /**
     * Downloads the raw bytes of an already-uploaded attachment from its public [url], for the
     * full-screen viewer's "save to device" action. Coil handles display caching separately.
     */
    suspend fun downloadImage(url: String): Result<ByteArray, DataError.Remote>
}
