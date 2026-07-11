package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.project.chat.presentation.Res
import com.project.chat.presentation.close
import com.project.chat.presentation.image_viewer
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.save_to_device
import com.project.core.designsystem.components.buttons.ChirpButton
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen image viewer shown as a [Dialog] over the chat. The image fits the screen on a dark
 * scrim; the top-end button (or system back / scrim tap) closes it.
 *
 * ## Gestures
 * - Pinch to zoom (1×–[MAX_SCALE]); drag to pan while zoomed.
 * - Double-tap toggles between fit and [DOUBLE_TAP_SCALE].
 * - Long-press reveals a "Save to device" action — tapping it triggers [onSaveClick], which downloads
 *   and saves the image.
 * - Single tap hides the save action.
 *
 * Zoom/pan state is local to this composable, so it resets whenever the viewer is reopened.
 */
@Composable
fun ImageViewerOverlay(
    attachment: MessageAttachmentUi,
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var isSaveActionVisible by remember { mutableStateOf(false) }
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        // Keeps the image from being panned entirely off-screen: the further you zoom, the more
        // overflow there is to pan into on each axis.
        fun clampOffset(candidate: Offset, forScale: Float): Offset {
            if (forScale <= 1f) return Offset.Zero
            val maxX = (forScale - 1f) * containerSize.width / 2f
            val maxY = (forScale - 1f) * containerSize.height / 2f
            return Offset(
                x = candidate.x.coerceIn(-maxX, maxX),
                y = candidate.y.coerceIn(-maxY, maxY),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
        ) {
            AsyncImage(
                model = attachment.url,
                contentDescription = stringResource(Res.string.image_viewer),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .onSizeChanged { containerSize = it }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { isSaveActionVisible = false },
                            onLongPress = { isSaveActionVisible = true },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = DOUBLE_TAP_SCALE
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            scale = newScale
                            offset = clampOffset(offset + pan, newScale)
                        }
                    },
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close),
                    tint = Color.White,
                )
            }

            AnimatedVisibility(
                visible = isSaveActionVisible || isSaving,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ChirpButton(
                    text = stringResource(Res.string.save_to_device),
                    onClick = onSaveClick,
                    isLoading = isSaving,
                )
            }
        }
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
