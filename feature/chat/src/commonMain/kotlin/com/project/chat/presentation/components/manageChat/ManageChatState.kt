package com.project.chat.presentation.components.manageChat

import androidx.compose.foundation.text.input.TextFieldState
import com.project.core.designsystem.components.avatar.ChatParticipantUi
import com.project.core.presentation.util.UiText

data class ManageChatState(
    val queryTextState: TextFieldState = TextFieldState(),
    val existingChatParticipants: List<ChatParticipantUi> = emptyList(),
    val selectedChatParticipants: List<ChatParticipantUi> = emptyList(),
    val isSearching: Boolean = false,
    val canAddParticipant: Boolean = false,
    val currentSearchResult: ChatParticipantUi? = null,
    val searchError: UiText? = null,
    val isSubmitting: Boolean = false,
    val submitError: UiText? = null,
    // Member management (creator only). `removableParticipantIds` is the set of existing members the local
    // user is allowed to remove — empty unless the local user is the creator (and never includes the creator).
    val removableParticipantIds: Set<String> = emptySet(),
    val participantToRemove: ChatParticipantUi? = null,
    val removingUserId: String? = null,
    val removeError: UiText? = null,
)
