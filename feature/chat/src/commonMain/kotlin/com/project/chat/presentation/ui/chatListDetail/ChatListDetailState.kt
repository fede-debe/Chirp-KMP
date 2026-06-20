package com.project.chat.presentation.ui.chatListDetail

data class ChatListDetailState(
    val selectedChatId: String? = null,
    val dialogState: DialogState = DialogState.Hidden,
)

sealed interface DialogState {
    data object Hidden : DialogState
    data object CreateChat : DialogState
    data object Profile : DialogState
    data class ManageChat(val chatId: String) : DialogState
}
