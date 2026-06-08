package com.project.chat.data.mappers

import com.project.chat.data.dto.ChatParticipantDto
import com.project.chat.database.entities.ChatParticipantEntity
import com.project.chat.domain.models.ChatParticipant

/**
 * Contains extension functions to convert Chat Participant models across architectural boundaries.
 *
 * ## Strategy / Decisions
 * - **Boundary Enforcement:** Specifically contains the `ChatParticipant.toUiModel()` mapping. Even though the domain and UI models only differ slightly (UI model has an `initials` field), the mapping is strictly necessary.
 * - **Why:** Prevents the generic Design System module (which handles UI rendering for avatars) from needing a dependency on the specific `chat` domain module.
 *
 * ## How It Works
 * 1. `ChatParticipantDto.toDomain()`: Maps the raw backend response fields to the internal business logic model.
 * 2. `ChatParticipant.toUiModel()`: Maps the domain model to the presentation state model, forwarding the ID, username, profile picture, and calculating/forwarding initials.
 */
fun ChatParticipantDto.toDomain(): ChatParticipant {
    return ChatParticipant(
        userId = userId,
        username = username,
        profilePictureUrl = profilePictureUrl,
    )
}

fun ChatParticipantEntity.toDomain(): ChatParticipant {
    return ChatParticipant(
        userId = userId,
        username = username,
        profilePictureUrl = profilePictureUrl,
    )
}

fun ChatParticipant.toEntity(): ChatParticipantEntity {
    return ChatParticipantEntity(
        userId = userId,
        username = username,
        profilePictureUrl = profilePictureUrl,
    )
}
