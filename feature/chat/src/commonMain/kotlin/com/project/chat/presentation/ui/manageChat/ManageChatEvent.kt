package com.project.chat.presentation.ui.manageChat

sealed interface ManageChatEvent {
    data object OnMembersAdded : ManageChatEvent
}
