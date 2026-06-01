package com.project.chat.presentation.models

import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.core.designSystem.components.avatar.ChatParticipantUi
import com.project.core.presentation.util.UiText

sealed interface MessageUiTemp {
    data class LocalUserMessageTemp(
        val id: String,
        val content: String,
        val deliveryStatus: ChatMessageDeliveryStatus,
        val isMenuOpen: Boolean,
        val formattedSentTime: UiText,
    ) : MessageUiTemp

    data class OtherUserMessageTemp(
        val id: String,
        val content: String,
        val formattedSentTime: UiText,
        val sender: ChatParticipantUi,
    ) : MessageUiTemp

    data class DateSeparator(
        val id: String,
        val date: UiText,
    ) : MessageUiTemp
}
