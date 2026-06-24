package com.project.chat.data.attachment

import com.project.chat.data.dto.response.AttachmentUploadUrlResponse
import com.project.chat.domain.attachment.AttachmentService
import com.project.chat.domain.models.MessageAttachment
import com.project.core.data.networking.get
import com.project.core.data.networking.safeCall
import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url

/**
 * Ktor implementation of [AttachmentService].
 *
 * Reuses the same signed-URL → Supabase `PUT` mechanics as the profile-picture upload. The single
 * `/messages/attachments/upload-url` endpoint is used per image (its `chatId`/`mimeType` params are
 * explicitly documented), looping for multi-image messages.
 */
class KtorAttachmentService(
    private val httpClient: HttpClient,
) : AttachmentService {

    override suspend fun uploadAttachment(
        chatId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        durationInSeconds: Int?,
    ): Result<MessageAttachment, DataError.Remote> {
        val uploadUrls = when (
            val result = httpClient.get<AttachmentUploadUrlResponse>(
                route = "/messages/attachments/upload-url",
                queryParams = mapOf(
                    "chatId" to chatId,
                    "mimeType" to mimeType,
                ),
            )
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        val uploadResult = safeCall<Unit> {
            httpClient.put {
                url(uploadUrls.uploadUrl)
                uploadUrls.headers.forEach { (key, value) ->
                    header(key, value)
                }
                setBody(bytes)
            }
        }
        if (uploadResult is Result.Failure) {
            return Result.Failure(uploadResult.error)
        }

        return Result.Success(
            MessageAttachment(
                storageUrl = uploadUrls.publicUrl,
                mimeType = mimeType,
                fileName = fileName,
                sizeInBytes = bytes.size.toLong(),
                durationInSeconds = durationInSeconds,
            ),
        )
    }

    override suspend fun downloadImage(url: String): Result<ByteArray, DataError.Remote> {
        // The public storage URL is absolute, so we bypass the base-URL `get` helper and hit it
        // directly. safeCall maps transport/status failures onto DataError.Remote.
        return safeCall {
            httpClient.get {
                url(url)
            }
        }
    }
}
