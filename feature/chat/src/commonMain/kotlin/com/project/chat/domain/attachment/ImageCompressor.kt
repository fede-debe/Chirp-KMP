package com.project.chat.domain.attachment

/**
 * Compresses picked images before upload to keep attachment sizes manageable.
 *
 * Implementations re-encode to JPEG at a reduced quality (and downscale where cheap to do so). On any
 * failure they return the original bytes/mime unchanged, so a compression problem never blocks sending.
 */
interface ImageCompressor {
    suspend fun compress(bytes: ByteArray, mimeType: String): CompressedImage
}

class CompressedImage(
    val bytes: ByteArray,
    val mimeType: String,
)
