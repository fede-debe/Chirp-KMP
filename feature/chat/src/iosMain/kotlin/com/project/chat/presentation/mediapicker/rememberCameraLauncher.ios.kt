@file:Suppress("ktlint:standard:filename", "filename")
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * Launches the iOS camera via [UIImagePickerController] with the `.camera` source type. Requires the
 * `NSCameraUsageDescription` Info.plist key; iOS shows its own one-time camera-permission prompt the
 * first time the controller is presented. The captured [UIImage] is JPEG-encoded into a
 * [PickedAttachment].
 */
@Composable
actual fun rememberCameraLauncher(
    onResult: (PickedAttachment) -> Unit,
): CameraLauncher {
    val scope = rememberCoroutineScope()

    val delegate = remember {
        object :
            NSObject(),
            UIImagePickerControllerDelegateProtocol,
            UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>,
            ) {
                picker.dismissViewControllerAnimated(true, null)

                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]
                    as? UIImage ?: return
                val data = UIImageJPEGRepresentation(image, JPEG_QUALITY) ?: return
                val bytes = data.toByteArray()

                scope.launch {
                    onResult(
                        PickedAttachment(
                            bytes = bytes,
                            mimeType = "image/jpeg",
                            fileName = "camera_${NSUUID().UUIDString()}.jpg",
                        ),
                    )
                }
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
            }
        }
    }

    return remember {
        CameraLauncher(
            isAvailable = UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
            ),
            onLaunch = {
                val picker = UIImagePickerController()
                picker.sourceType =
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                picker.delegate = delegate
                topMostViewController()?.presentViewController(picker, true, null)
            },
        )
    }
}

private const val JPEG_QUALITY = 0.9

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}
