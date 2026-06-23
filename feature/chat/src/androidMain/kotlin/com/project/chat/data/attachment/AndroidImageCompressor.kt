package com.project.chat.data.attachment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.project.chat.domain.attachment.CompressedImage
import com.project.chat.domain.attachment.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Android [ImageCompressor]. Downsamples while decoding (so very large images don't OOM) and re-encodes
 * to JPEG. Any failure falls back to the original bytes/mime so sending is never blocked.
 */
class AndroidImageCompressor : ImageCompressor {

    override suspend fun compress(bytes: ByteArray, mimeType: String): CompressedImage =
        withContext(Dispatchers.Default) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    ?: return@runCatching CompressedImage(bytes, mimeType)

                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                bitmap.recycle()

                CompressedImage(output.toByteArray(), "image/jpeg")
            }.getOrElse { CompressedImage(bytes, mimeType) }
        }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= MAX_EDGE_PX || h / 2 >= MAX_EDGE_PX) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private companion object {
        const val MAX_EDGE_PX = 1920
        const val JPEG_QUALITY = 70
    }
}
