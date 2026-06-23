package com.project.chat.data.attachment

import com.project.chat.domain.attachment.CompressedImage
import com.project.chat.domain.attachment.ImageCompressor

/**
 * Desktop is out of scope for the mobile image-attachments task; pass the bytes through unchanged so the
 * shared code compiles and runs. Real desktop compression can be added later.
 */
class DesktopImageCompressor : ImageCompressor {
    override suspend fun compress(bytes: ByteArray, mimeType: String): CompressedImage {
        return CompressedImage(bytes, mimeType)
    }
}
