package com.project.chat.presentation.mappers

import com.project.chat.domain.models.ChatParticipant
import com.project.core.designSystem.components.avatar.ChatParticipantUi

fun ChatParticipant.toUi(): ChatParticipantUi {
    return ChatParticipantUi(
        id = userId,
        username = username,
        initials = initials,
        imageUrl = profilePictureUrl,
    )
}
