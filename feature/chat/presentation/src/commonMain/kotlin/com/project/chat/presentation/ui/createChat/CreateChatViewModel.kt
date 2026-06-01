package com.project.chat.presentation.ui.createChat

import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chirp.feature.chat.presentation.generated.resources.Res
import chirp.feature.chat.presentation.generated.resources.error_participant_not_found
import com.project.chat.domain.chat.ChatParticipantService
import com.project.chat.presentation.mappers.toUi
import com.project.core.domain.util.DataError
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.presentation.util.UiText
import com.project.core.presentation.util.toUiText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Manages the state and interactions for the Create Chat flow, specifically handling the debounced search mechanism to find new participants.
 *
 * ## Strategy / Decisions
 * - **Debounced Search Strategy:** Implements a reactive search flow using Kotlin's `snapshotFlow` and a 1-second `debounce` operator. This prevents spamming the backend API with network calls on every keystroke. It waits for the user to pause typing before executing the search.
 * - **Lifecycle & State Scoping:** State is inherently cleared when the dialog is dismissed because the ViewModel is scoped to the adaptive bottom sheet/dialog visibility. When the parent sets visibility to false, this ViewModel is destroyed, acting as an automatic state cleanup mechanism.
 * - **Error Translation:** Specifically intercepts the 404 (Not Found) network error and translates it into a user-friendly UI text resource ("No participant found") rather than displaying a generic network error.
 *
 * ## How It Works
 * 1. Observes `queryTextState` via `snapshotFlow`.
 * 2. Applies a 1-second `debounce`—canceling previous emissions if a new keystroke occurs within the window.
 * 3. In the `onEach` block, checks if the query is blank. If true, resets search results and disables the "Add" button.
 * 4. If a valid query exists, sets `isSearching = true` and launches a coroutine in `viewModelScope` to call `ChatParticipantService.searchParticipant`.
 * 5. On success, updates the state with the mapped UI participant model and enables the "Add" button.
 * 6. On failure, parses the error (checking for `DataError.Remote.NOT_FOUND`) and updates the `searchError` state.
 * 7. When adding a participant, verifies the user isn't already in `selectedChatParticipants` to prevent duplicate additions.
 *
 * ## Alternatives / Why Not
 * - **Alternative Rejected (Domain Model for UI):** Initially considered skipping the Presentation/UI mapping and making the Composables depend directly on the `ChatParticipant` domain model since the fields (ID, username, profile picture) were virtually identical.
 * - **Reason for Rejection:** The UI components (like Avatar photos) live inside an isolated Design System module. That generic module should not have dependencies on feature-specific chat domain models to maintain architectural separation. Thus, a dedicated UI mapper was retained.
 *
 * Technical Details:
 * - Requires `@OptIn(ExperimentalCoroutinesApi::class)` or similar for flow preview features (`debounce`).
 * - Employs `viewModelScope` for coroutine execution.
 * - Search only yields results for users with *verified* email addresses, as the backend delays chat participant record creation until verification.
 */
@OptIn(FlowPreview::class)
class CreateChatViewModel(
    private val chatParticipantService: ChatParticipantService,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(CreateChatState())

    private val searchFlow = snapshotFlow { _state.value.queryTextState.text.toString() }
        .debounce(1.seconds)
        .onEach { query ->
            performSearch(query)
        }

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                searchFlow.launchIn(viewModelScope)
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CreateChatState(),
        )

    fun onAction(action: CreateChatAction) {
        when (action) {
            CreateChatAction.OnAddClick -> addParticipant()
            CreateChatAction.OnCreateChatClick -> {
            }
            CreateChatAction.OnDismissDialog -> Unit
        }
    }

    private fun addParticipant() {
        state.value.currentSearchResult?.let { participant ->
            val isAlreadyPartOfChat = state.value.selectedChatParticipants.any {
                it.id == participant.id
            }
            if (!isAlreadyPartOfChat) {
                _state.update {
                    it.copy(
                        selectedChatParticipants = it.selectedChatParticipants + participant,
                        canAddParticipant = false,
                        currentSearchResult = null,
                    )
                }
                _state.value.queryTextState.clearText()
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
