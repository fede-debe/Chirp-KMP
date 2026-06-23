package com.project.chat.domain.attachment

import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult

/**
 * Persists an already-downloaded image to the device's photo gallery / camera roll.
 *
 * Platform implementations handle the OS-specific destination and permissions:
 * - Android: MediaStore (no permission on API 29+; legacy storage permission below that).
 * - iOS: the Photos library (requires the `NSPhotoLibraryAddUsageDescription` Info.plist key).
 * - Desktop: a best-effort write into the user's Pictures folder.
 *
 * The bytes are expected to be the fully-resolved image (see [AttachmentService.downloadImage]); this
 * type does not perform any network I/O.
 */
interface ImageSaver {
    suspend fun saveToGallery(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): EmptyResult<DataError.Local>
}
