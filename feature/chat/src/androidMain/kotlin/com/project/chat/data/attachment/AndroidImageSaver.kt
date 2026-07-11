package com.project.chat.data.attachment

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.project.chat.domain.attachment.ImageSaver
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android [ImageSaver] backed by MediaStore.
 *
 * On API 29+ (scoped storage) images are written into `Pictures/Chirp` with no runtime permission,
 * using `IS_PENDING` so the file isn't visible until the bytes are fully flushed. On API 26–28 the
 * same MediaStore insert is used, but it requires the legacy `WRITE_EXTERNAL_STORAGE` permission; if
 * that hasn't been granted the insert throws and we surface a [DataError.Local] failure rather than
 * crashing.
 */
class AndroidImageSaver(
    private val context: Context,
) : ImageSaver {

    override suspend fun saveToGallery(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): EmptyResult<DataError.Local> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName(fileName, mimeType))
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/$ALBUM",
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(collection, values)
                ?: error("MediaStore returned a null URI")

            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Unable to open an output stream for $uri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Failure(DataError.Local.UNKNOWN) },
        )
    }

    /** Use the source name but force the extension to match the (re-encoded) mime type. */
    private fun displayName(fileName: String, mimeType: String): String {
        val base = fileName.substringBeforeLast('.', fileName).ifBlank { "image" }
        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "$base.$extension"
    }

    private companion object {
        const val ALBUM = "Chirp"
    }
}
