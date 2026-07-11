@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.CompressedImage
import com.project.chat.domain.attachment.ImageCompressor
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

/**
 * iOS [ImageCompressor]. Re-encodes the image to JPEG at a reduced quality. Any failure falls back to the
 * original bytes/mime so sending is never blocked.
 */
class IosImageCompressor : ImageCompressor {

    override suspend fun compress(bytes: ByteArray, mimeType: String): CompressedImage =
        withContext(Dispatchers.Default) {
            // Use the nullable factory, not the UIImage(data:) constructor: a constructor can never be
            // null in Kotlin, so when the underlying initWithData: returns nil (an undecodable image)
            // Kotlin/Native throws an NPE instead of falling back. imageWithData returns UIImage?.
            val uiImage = UIImage.imageWithData(bytes.toNSData())
                ?: return@withContext CompressedImage(bytes, mimeType)
            val jpeg = UIImageJPEGRepresentation(uiImage, JPEG_QUALITY)
                ?: return@withContext CompressedImage(bytes, mimeType)
            CompressedImage(jpeg.toByteArray(), "image/jpeg")
        }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return ByteArray(0)
        val result = ByteArray(size)
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
        return result
    }

    private companion object {
        const val JPEG_QUALITY = 0.7
    }
}
