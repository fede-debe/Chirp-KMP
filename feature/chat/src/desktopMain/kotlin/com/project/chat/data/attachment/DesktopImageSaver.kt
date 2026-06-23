package com.project.chat.data.attachment

import com.project.chat.domain.attachment.ImageSaver
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Desktop [ImageSaver]. There is no system photo gallery, so this writes the image into a `Chirp`
 * folder under the user's home `Pictures` directory as a best-effort equivalent.
 */
class DesktopImageSaver : ImageSaver {

    override suspend fun saveToGallery(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): EmptyResult<DataError.Local> = withContext(Dispatchers.IO) {
        runCatching {
            val picturesDir = File(System.getProperty("user.home"), "Pictures/Chirp").apply {
                mkdirs()
            }
            File(picturesDir, displayName(fileName, mimeType)).writeBytes(bytes)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Failure(DataError.Local.UNKNOWN) },
        )
    }

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
}
