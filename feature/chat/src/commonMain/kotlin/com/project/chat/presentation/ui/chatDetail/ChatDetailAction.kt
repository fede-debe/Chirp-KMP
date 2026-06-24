package com.project.chat.presentation.ui.chatDetail

import com.project.chat.presentation.mediapicker.PickedAttachment
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.models.MessageUi

sealed interface ChatDetailAction {
    data object OnSendMessageClick : ChatDetailAction
    data object OnAttachClick : ChatDetailAction
    data class OnAttachmentsPicked(val attachments: List<PickedAttachment>) : ChatDetailAction
    data class OnRemoveAttachment(val id: String) : ChatDetailAction
    data object OnScrollToTop : ChatDetailAction
    data class OnSelectChat(val chatId: String?) : ChatDetailAction
    data class OnDeleteMessageClick(val message: MessageUi.LocalUserMessage) : ChatDetailAction
    data class OnMessageLongClick(val message: MessageUi.LocalUserMessage) : ChatDetailAction
    data object OnDismissMessageMenu : ChatDetailAction
    data class OnRetryClick(val message: MessageUi.LocalUserMessage) : ChatDetailAction
    data object OnBackClick : ChatDetailAction
    data object OnChatOptionsClick : ChatDetailAction
    data object OnChatMembersClick : ChatDetailAction
    data object OnLeaveChatClick : ChatDetailAction
    data object OnDismissChatOptions : ChatDetailAction
    data object OnRetryPaginationClick : ChatDetailAction
    data object OnHideBanner : ChatDetailAction
    data class OnFirstVisibleIndexChanged(val index: Int) : ChatDetailAction
    data class OnTopVisibleIndexChanged(val topVisibleIndex: Int) : ChatDetailAction
    data class OnAttachmentClick(val attachment: MessageAttachmentUi) : ChatDetailAction
    data object OnDismissAttachmentViewer : ChatDetailAction
    data object OnSaveOpenedAttachment : ChatDetailAction
    data object OnDismissAttachmentSheet : ChatDetailAction
    data object OnTakePhotoClick : ChatDetailAction
    data object OnPickFromGalleryClick : ChatDetailAction
    data object OnAttachmentLaunchHandled : ChatDetailAction
    data object OnMicClick : ChatDetailAction
    data object OnStartRecording : ChatDetailAction
    data object OnStopRecording : ChatDetailAction
    data object OnCancelRecording : ChatDetailAction
    data object OnPauseRecording : ChatDetailAction
    data object OnResumeRecording : ChatDetailAction
    data object OnRecordPermissionDenied : ChatDetailAction
    data class OnPlayAttachment(val attachment: MessageAttachmentUi) : ChatDetailAction
    data object OnPauseAttachment : ChatDetailAction
}

data class OnTopVisibleIndexChanged(val topVisibleIndex: Int) : ChatDetailAction
