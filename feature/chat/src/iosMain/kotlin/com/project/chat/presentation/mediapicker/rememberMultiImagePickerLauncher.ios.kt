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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationSelectionOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify
import platform.posix.memcpy

@Composable
actual fun rememberMultiImagePickerLauncher(
    selectionLimit: Int,
    onResult: (List<PickedAttachment>) -> Unit,
): MultiImagePickerLauncher {
    val scope = rememberCoroutineScope()

    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)

                val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                val dispatchGroup = dispatch_group_create()
                val picked = mutableListOf<PickedAttachment>()

                for (result in results) {
                    val itemProvider = result.itemProvider
                    val identifiers = itemProvider.registeredTypeIdentifiers
                        .filterIsInstance<String>()
                    // A Live Photo registers "com.apple.live-photo-bundle" first; loading that yields
                    // the whole bundle (still + video), which UIImage can't decode -> a black image.
                    // Prefer a plain still-image type so we load just the photo.
                    val typeIdentifier = identifiers.firstOrNull {
                        it == "public.heic" || it == "public.jpeg" || it == "public.png"
                    }
                        ?: identifiers.firstOrNull { it != LIVE_PHOTO_BUNDLE_TYPE }
                        ?: continue
                    val mimeType = UTType
                        .typeWithIdentifier(typeIdentifier)
                        ?.preferredMIMEType
                        ?: "image/jpeg"
                    val suggestedName = itemProvider.suggestedName ?: "image"

                    dispatch_group_enter(dispatchGroup)
                    itemProvider.loadDataRepresentationForTypeIdentifier(
                        typeIdentifier = typeIdentifier,
                    ) { nsData, _ ->
                        // Hop to the composition scope so the shared list is mutated serially.
                        scope.launch {
                            nsData?.let {
                                val bytes = withContext(Dispatchers.Default) { it.toByteArray() }
                                picked.add(
                                    PickedAttachment(
                                        bytes = bytes,
                                        mimeType = mimeType,
                                        fileName = suggestedName,
                                    ),
                                )
                            }
                            dispatch_group_leave(dispatchGroup)
                        }
                    }
                }

                dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
                    scope.launch { onResult(picked.toList()) }
                }
            }
        }
    }

    return remember(selectionLimit) {
        val pickerViewController = PHPickerViewController(
            configuration = PHPickerConfiguration().apply {
                setSelectionLimit(selectionLimit.toLong())
                setFilter(PHPickerFilter.imagesFilter)
                setSelection(PHPickerConfigurationSelectionOrdered)
            },
        )
        pickerViewController.delegate = delegate

        MultiImagePickerLauncher(
            onLaunch = {
                topMostViewController()?.presentViewController(
                    pickerViewController,
                    true,
                    null,
                )
            },
        )
    }
}

private const val LIVE_PHOTO_BUNDLE_TYPE = "com.apple.live-photo-bundle"

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}
