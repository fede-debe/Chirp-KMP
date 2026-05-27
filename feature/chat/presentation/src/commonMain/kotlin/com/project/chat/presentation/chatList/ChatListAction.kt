package com.project.chat.presentation.chatList

sealed interface ChatListAction {
    data object OnButtonAClick : ChatListAction
    data class OnTypeInTextFieldB(val text: String) : ChatListAction
}
