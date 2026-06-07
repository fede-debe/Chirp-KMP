package com.project.chat.presentation.models

import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.core.designSystem.components.avatar.ChatParticipantUi
import com.project.core.presentation.util.UiText

sealed class ChatMessageUi(open val id: String) {
    data class LocalUserMessage(
        override val id: String,
        val content: String,
        val deliveryStatus: ChatMessageDeliveryStatus,
        val formattedSentTime: UiText,
    ) : ChatMessageUi(id)

    data class OtherUserMessage(
        override val id: String,
        val content: String,
        val formattedSentTime: UiText,
        val sender: ChatParticipantUi,
    ) : ChatMessageUi(id)

    data class DateSeparator(
        override val id: String,
        val date: UiText,
    ) : ChatMessageUi(id)
}
