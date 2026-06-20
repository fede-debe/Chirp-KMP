@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.ui.profile.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.draganddrop.DragAndDropTarget

@Composable
expect fun rememberDragAndDropTarget(
    onHover: (Boolean) -> Unit,
    onDrop: (PickedImageData) -> Unit,
): DragAndDropTarget
