package com.project.chat.presentation.ui.chatListDetail

sealed interface ChatListDetailAction {
    data class OnSelectChat(val chatId: String?) : ChatListDetailAction
    data object OnProfileSettingsClick : ChatListDetailAction
    data object OnCreateChatClick : ChatListDetailAction
    data object OnManageChatClick : ChatListDetailAction
    data object OnDismissCurrentDialog : ChatListDetailAction
}
