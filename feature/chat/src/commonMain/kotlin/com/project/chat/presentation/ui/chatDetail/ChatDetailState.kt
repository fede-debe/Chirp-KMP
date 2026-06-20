package com.project.chat.presentation.ui.chatDetail

import androidx.compose.foundation.text.input.TextFieldState
import com.project.chat.domain.models.ConnectionState
import com.project.chat.presentation.models.ChatUi
import com.project.chat.presentation.models.MessageUi
import com.project.core.presentation.util.UiText

data class ChatDetailState(
    val chatUi: ChatUi? = null,
    val isLoading: Boolean = false,
    val messages: List<MessageUi> = emptyList(),
    val error: UiText? = null,
    val messageTextFieldState: TextFieldState = TextFieldState(),
    val canSendMessage: Boolean = false,
    val isPaginationLoading: Boolean = false,
    val paginationError: UiText? = null,
    val endReached: Boolean = false,
    val messageWithOpenMenu: MessageUi.LocalUserMessage? = null,
    val bannerState: BannerState = BannerState(),
    val isChatOptionsOpen: Boolean = false,
    val isNearBottom: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
)

data class BannerState(
    val formattedDate: UiText? = null,
    val isVisible: Boolean = false,
)
