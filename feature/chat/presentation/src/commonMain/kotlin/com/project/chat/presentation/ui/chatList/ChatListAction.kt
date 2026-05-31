package com.project.chat.presentation.ui.chatList

sealed interface ChatListAction {
    data object OnButtonAClick : ChatListAction
    data class OnTypeInTextFieldB(val text: String) : ChatListAction
}
