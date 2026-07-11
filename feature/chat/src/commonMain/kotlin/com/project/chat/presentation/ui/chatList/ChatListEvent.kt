package com.project.chat.presentation.ui.chatList

import com.project.chat.domain.models.ChatRemovalReason
import com.project.core.presentation.util.UiText

sealed interface ChatListEvent {
    data object OnLogoutSuccess : ChatListEvent
    data class OnLogoutError(val error: UiText) : ChatListEvent

    /** A chat just disappeared from the list because the user lost access — show an explanatory snackbar. */
    data class OnChatRemoved(val reason: ChatRemovalReason) : ChatListEvent
}
