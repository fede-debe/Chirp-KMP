package com.project.chat.presentation.ui.chatList

import com.project.core.presentation.util.UiText

sealed interface ChatListEvent {
    data object OnLogoutSuccess : ChatListEvent
    data class OnLogoutError(val error: UiText) : ChatListEvent
}
