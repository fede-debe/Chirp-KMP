package com.project.chat.presentation.ui.createChat

sealed interface CreateChatAction {
    data object OnAddClick : CreateChatAction
    data object OnDismissDialog : CreateChatAction
    data object OnCreateChatClick : CreateChatAction
}
