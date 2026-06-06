package com.project.chat.presentation.components.manageChat

sealed interface ManageChatAction {
    data object OnAddClick : ManageChatAction
    data object OnDismissDialog : ManageChatAction
    data object OnPrimaryActionClick : ManageChatAction
}
