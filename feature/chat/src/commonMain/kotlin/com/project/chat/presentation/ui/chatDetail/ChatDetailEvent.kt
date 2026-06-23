package com.project.chat.presentation.ui.chatDetail

import com.project.core.presentation.util.UiText

sealed interface ChatDetailEvent {
    data object OnChatLeft : ChatDetailEvent
    data class OnError(val error: UiText) : ChatDetailEvent
    data object OnNewMessage : ChatDetailEvent
    data object OnAttachmentSaved : ChatDetailEvent
}
