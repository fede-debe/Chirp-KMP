package com.project.chat.presentation.ui.createChat

import com.project.chat.domain.models.Chat

sealed interface CreateChatEvent {
    data class OnChatCreated(val chat: Chat) : CreateChatEvent
}
