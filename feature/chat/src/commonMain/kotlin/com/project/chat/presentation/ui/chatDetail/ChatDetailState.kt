package com.project.chat.presentation.ui.chatDetail

import androidx.compose.foundation.text.input.TextFieldState
import com.project.chat.domain.models.ConnectionState
import com.project.chat.presentation.models.ChatUi
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.models.MessageUi
import com.project.chat.presentation.models.PendingAttachmentUi
import com.project.core.presentation.util.UiText

data class ChatDetailState(
    val chatUi: ChatUi? = null,
    val isLoading: Boolean = false,
    val messages: List<MessageUi> = emptyList(),
    val error: UiText? = null,
    val messageTextFieldState: TextFieldState = TextFieldState(),
    val canSendMessage: Boolean = false,
    val pendingAttachments: List<PendingAttachmentUi> = emptyList(),
    val isSending: Boolean = false,
    val isPaginationLoading: Boolean = false,
    val paginationError: UiText? = null,
    val endReached: Boolean = false,
    val messageWithOpenMenu: MessageUi.LocalUserMessage? = null,
    val bannerState: BannerState = BannerState(),
    val isChatOptionsOpen: Boolean = false,
    val isNearBottom: Boolean = false,
    val hasUnseenMessages: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val openedAttachment: MessageAttachmentUi? = null,
    val isSavingAttachment: Boolean = false,
    val isAttachmentSheetOpen: Boolean = false,
    val pendingAttachmentSource: AttachmentSource? = null,
    val recording: RecordingState? = null,
    /** Usernames of other participants currently typing in this chat (server-supplied, ephemeral). */
    val typingUsernames: List<String> = emptyList(),
)

/** Present only while a voice message is being recorded; drives the composer's recording bar. */
data class RecordingState(
    val elapsedSeconds: Int = 0,
    val isPaused: Boolean = false,
)

/**
 * A chosen attachment source that should be launched once the bottom sheet has fully closed. Launching
 * while the sheet is still dismissing presents the picker onto the sheet's transient window, which is
 * then torn down with the sheet (the picker never appears / the camera session dies with `-17281`).
 */
enum class AttachmentSource {
    CAMERA,
    GALLERY,
}

data class BannerState(
    val formattedDate: UiText? = null,
    val isVisible: Boolean = false,
)
