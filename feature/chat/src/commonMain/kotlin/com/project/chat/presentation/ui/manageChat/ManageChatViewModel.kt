@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.project.chat.presentation.ui.manageChat

import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.domain.models.ChatParticipant
import com.project.chat.domain.participant.ChatParticipantService
import com.project.chat.presentation.Res
import com.project.chat.presentation.components.manageChat.ManageChatAction
import com.project.chat.presentation.components.manageChat.ManageChatState
import com.project.chat.presentation.error_participant_not_found
import com.project.chat.presentation.mappers.toUi
import com.project.core.domain.auth.SessionStorage
import com.project.core.domain.util.DataError
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.presentation.util.UiText
import com.project.core.presentation.util.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ManageChatViewModel(
    private val chatRepository: ChatRepository,
    private val chatParticipantService: ChatParticipantService,
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private val flowChatId = MutableStateFlow<String?>(null)

    private val eventChannel = Channel<ManageChatEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(ManageChatState())
    val state = flowChatId
        .flatMapLatest { chatId ->
            if (chatId != null) {
                combine(
                    chatRepository.getActiveParticipantsByChatId(chatId),
                    chatRepository.getChatInfoById(chatId)
                        .map { it.chat.creatorId }
                        .distinctUntilChanged(),
                    sessionStorage.observeAuthInfo(),
                ) { participants, creatorId, authInfo ->
                    Triple(participants, creatorId, authInfo?.user?.id)
                }
            } else {
                emptyFlow()
            }
        }
        .combine(_state) { (participants, creatorId, currentUserId), currentState ->
            // Only the creator can remove members, and never themselves.
            val canManage = currentUserId != null && currentUserId == creatorId
            val removableParticipantIds = if (canManage) {
                participants.map(ChatParticipant::userId)
                    .filter { it != creatorId }
                    .toSet()
            } else {
                emptySet()
            }
            currentState.copy(
                existingChatParticipants = participants.map { it.toUi() },
                removableParticipantIds = removableParticipantIds,
            )
        }
        .onStart {
            if (!hasLoadedInitialData) {
                searchFlow.launchIn(viewModelScope)
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ManageChatState(),
        )

    private val searchFlow = snapshotFlow { _state.value.queryTextState.text.toString() }
        .debounce(1.seconds)
        .onEach { query ->
            performSearch(query)
        }

    fun onAction(action: ManageChatAction) {
        when (action) {
            ManageChatAction.OnAddClick -> addParticipant()
            ManageChatAction.OnPrimaryActionClick -> addParticipantsToChat()
            is ManageChatAction.ChatParticipants.OnSelectChat -> {
                flowChatId.update { action.chatId }
            }
            is ManageChatAction.OnRemoveParticipantClick -> {
                _state.update {
                    it.copy(participantToRemove = action.participant, removeError = null)
                }
            }
            ManageChatAction.OnConfirmRemoveParticipant -> removeParticipant()
            ManageChatAction.OnDismissRemoveDialog -> {
                _state.update { it.copy(participantToRemove = null) }
            }
            else -> Unit
        }
    }

    private fun removeParticipant() {
        val chatId = flowChatId.value ?: return
        val participant = state.value.participantToRemove ?: return

        viewModelScope.launch {
            _state.update { it.copy(removingUserId = participant.id, removeError = null) }
            chatRepository
                .removeParticipant(chatId, participant.id)
                .onSuccess {
                    // The participant list refreshes via fetchChatById / CHAT_PARTICIPANTS_CHANGED.
                    _state.update { it.copy(participantToRemove = null, removingUserId = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            participantToRemove = null,
                            removingUserId = null,
                            removeError = error.toUiText(),
                        )
                    }
                }
        }
    }

    private fun addParticipant() {
        state.value.currentSearchResult?.let { participantFromSearch ->
            val isAlreadySelected = state.value.selectedChatParticipants.any {
                it.id == participantFromSearch.id
            }
            val isAlreadyInChat = state.value.existingChatParticipants.any {
                it.id == participantFromSearch.id
            }
            val updatedParticipants = if (isAlreadyInChat || isAlreadySelected) {
                state.value.selectedChatParticipants
            } else {
                state.value.selectedChatParticipants + participantFromSearch
            }

            state.value.queryTextState.clearText()
            _state.update {
                it.copy(
                    selectedChatParticipants = updatedParticipants,
                    canAddParticipant = false,
                    currentSearchResult = null,
                )
            }
        }
    }

    private fun addParticipantsToChat() {
        if (state.value.selectedChatParticipants.isEmpty()) {
            return
        }

        val chatId = flowChatId.value ?: return

        val selectedParticipants = state.value.selectedChatParticipants
        val selectedUserIds = selectedParticipants.map { it.id }

        viewModelScope.launch {
            chatRepository
                .addParticipantsToChat(
                    chatId = chatId,
                    userIds = selectedUserIds,
                )
                .onSuccess {
                    eventChannel.send(ManageChatEvent.OnMembersAdded)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = error.toUiText(),
                        )
                    }
                }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _state.update {
                it.copy(
                    currentSearchResult = null,
                    canAddParticipant = false,
                    searchError = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSearching = true,
                    canAddParticipant = false,
                )
            }

            chatParticipantService
                .searchParticipant(query)
                .onSuccess { participant ->
                    _state.update {
                        it.copy(
                            currentSearchResult = participant.toUi(),
                            isSearching = false,
                            canAddParticipant = true,
                            searchError = null,
                        )
                    }
                }
                .onFailure { error ->
                    val errorMessage = when (error) {
                        DataError.Remote.NOT_FOUND -> UiText.Resource(Res.string.error_participant_not_found)
                        else -> error.toUiText()
                    }
                    _state.update {
                        it.copy(
                            searchError = errorMessage,
                            isSearching = false,
                            canAddParticipant = false,
                            currentSearchResult = null,
                        )
                    }
                }
        }
    }
}
