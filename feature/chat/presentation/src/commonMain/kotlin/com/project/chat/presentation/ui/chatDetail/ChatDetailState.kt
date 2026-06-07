package com.project.chat.presentation.ui.chatDetail

import androidx.compose.foundation.text.input.TextFieldState
import com.project.chat.domain.models.ConnectionState
import com.project.chat.presentation.models.ChatMessageUi
import com.project.chat.presentation.models.ChatUi
import com.project.core.presentation.util.UiText

data class ChatDetailState(
    val chatUi: ChatUi? = null,
    val isLoading: Boolean = false,
    val messages: List<ChatMessageUi> = emptyList(),
    val error: UiText? = null,
    val messageTextFieldState: TextFieldState = TextFieldState(),
    val canSendMessage: Boolean = false,
    val isPaginationLoading: Boolean = false,
    val paginationError: UiText? = null,
    val endReached: Boolean = false,
    val messageWithOpenMenu: ChatMessageUi.LocalUserMessage? = null,
    val bannerState: BannerState = BannerState(),
    val isChatOptionsOpen: Boolean = false,
    val isNearBottom: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
)

data class BannerState(
    val formattedDate: UiText? = null,
    val isVisible: Boolean = false,
)
