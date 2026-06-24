package com.project.chat.presentation.ui.chatList

import com.project.chat.presentation.models.ChatUi
import com.project.core.designsystem.components.avatar.ChatParticipantUi
import com.project.core.presentation.util.UiText

data class ChatListState(
    val chats: List<ChatUi> = emptyList(),
    val error: UiText? = null,
    val localParticipant: ChatParticipantUi? = null,
    val isUserMenuOpen: Boolean = false,
    val showLogoutConfirmation: Boolean = false,
    val selectedChatId: String? = null,
    val isLoading: Boolean = false,
    /** Per chat id, the usernames of other participants currently typing (ephemeral, server-driven). */
    val typingUsersByChat: Map<String, List<String>> = emptyMap(),
)
