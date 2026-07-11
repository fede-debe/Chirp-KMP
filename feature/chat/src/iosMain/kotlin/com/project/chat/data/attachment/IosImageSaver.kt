@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.ImageSaver
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult
import com.project.core.domain.util.Result
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume

/**
 * iOS [ImageSaver] backed by the Photos framework.
 *
 * Wraps `PHPhotoLibrary.performChanges(...)` — which adds the image to the user's photo library — in a
 * suspending call so the success/failure of the asynchronous completion handler maps onto our
 * [EmptyResult]. The first save triggers the system "add to Photos" permission prompt, which requires
 * the `NSPhotoLibraryAddUsageDescription` key in Info.plist.
 */
class IosImageSaver : ImageSaver {

    override suspend fun saveToGallery(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): EmptyResult<DataError.Local> = suspendCancellableCoroutine { continuation ->
        val data = bytes.toNSData()
        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = {
                PHAssetCreationRequest.creationRequestForAsset()
                    .addResourceWithType(
                        type = PHAssetResourceTypePhoto,
                        data = data,
                        options = null,
                    )
            },
            completionHandler = { success, _ ->
                val result = if (success) {
                    Result.Success(Unit)
                } else {
                    Result.Failure(DataError.Local.UNKNOWN)
                }
                continuation.resume(result)
            },
        )
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
}
