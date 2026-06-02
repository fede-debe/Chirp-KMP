package com.project.chat.presentation.ui.chatList

import com.project.chat.presentation.models.ChatUi

sealed interface ChatListAction {
    data object OnUserAvatarClick : ChatListAction
    data object OnDismissUserMenu : ChatListAction
    data object OnLogoutClick : ChatListAction
    data object OnConfirmLogout : ChatListAction
    data object OnDismissLogoutDialog : ChatListAction
    data object OnCreateChatClick : ChatListAction
    data object OnProfileSettingsClick : ChatListAction
    data class OnChatClick(val chat: ChatUi) : ChatListAction
}
