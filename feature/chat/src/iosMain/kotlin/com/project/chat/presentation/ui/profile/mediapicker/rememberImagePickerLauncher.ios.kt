@file:Suppress("ktlint:standard:filename", "filename")

@file:OptIn(ExperimentalForeignApi::class)

package com.project.chat.presentation.ui.profile.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.project.chat.presentation.mediapicker.topMostViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
actual fun rememberImagePickerLauncher(
    onResult: (PickedImageData) -> Unit,
): ImagePickerLauncher {
    val scope = rememberCoroutineScope()
    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)

                val results = didFinishPicking.filterIsInstance<PHPickerResult>()

                val dispatchGroup = dispatch_group_create()
                val imageDataList = mutableListOf<PickedImageData>()

                for (result in results) {
                    dispatch_group_enter(dispatchGroup)

                    val itemProvider = result.itemProvider

                    val typeIdentifiers = itemProvider.registeredTypeIdentifiers
                        .filterIsInstance<String>()
                    // Prefer a plain still-image type; a Live Photo lists "live-photo-bundle" first,
                    // which loads the whole bundle (still + video) and can't be decoded as an image.
                    val primaryType = typeIdentifiers.firstOrNull {
                        it == "public.heic" || it == "public.jpeg" || it == "public.png"
                    }
                        ?: typeIdentifiers.firstOrNull { it != LIVE_PHOTO_BUNDLE_TYPE }

                    if (primaryType == null) {
                        dispatch_group_leave(dispatchGroup)
                        continue
                    }

                    val mimeType = UTType
                        .typeWithIdentifier(primaryType)
                        ?.preferredMIMEType

                    if (mimeType == null) {
                        dispatch_group_leave(dispatchGroup)
                        continue
                    }

                    itemProvider.loadDataRepresentationForTypeIdentifier(
                        typeIdentifier = primaryType,
                    ) { nsData, nsError ->
                        scope.launch {
                            nsData?.let {
                                val bytes = ByteArray(it.length.toInt())

                                withContext(Dispatchers.Default) {
                                    memcpy(bytes.refTo(0), it.bytes, it.length)
                                }

                                imageDataList.add(
                                    PickedImageData(
                                        bytes = bytes,
                                        mimeType = mimeType,
                                    ),
                                )
                            }
                            dispatch_group_leave(dispatchGroup)
                        }
                    }

                    dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
                        scope.launch {
                            imageDataList.firstOrNull()?.let { item ->
                                onResult(item)
                            }
                        }
                    }
                }
            }
        }
    }

    return remember {
        val pickerViewController = PHPickerViewController(
            configuration = PHPickerConfiguration().apply {
                setSelectionLimit(1)
                setFilter(PHPickerFilter.imagesFilter)
                setSelection(PHPickerConfigurationSelectionOrdered)
            },
        )
        pickerViewController.delegate = delegate

        ImagePickerLauncher(
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
