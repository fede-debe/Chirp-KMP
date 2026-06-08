package com.project.chat.presentation.ui.chatList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.presentation.mappers.toUi
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **Header:** ViewModel managing the reactive UI state for the chat list screen.
 * * ## Strategy / Decisions
 * - **Flow Combining for State Construction:** Uses a `combine` operator to merge manual UI state changes, the real-time chat list from the Repository, and local session storage auth data into a single SSOT for the View.
 * - **Non-blocking Data Initialization:** Fetch operations are pushed to background coroutines to avoid freezing the reactive state streams.
 * * ## How It Works
 * 1. Initializes a `combine` block listening to `repository.getChats()` and `sessionStorage.observeAuthInfo()`.
 * 2. If `authInfo` is null, it bails out (handled globally by MainViewModel for logouts).
 * 3. Extracts the logged-in user's ID from session storage to pass into UI mappers (identifying the local participant).
 * 4. Maps the raw database domain models into UI-friendly `ChatUi` models (including dynamic initial generation via substring).
 * 5. Calls `loadChats()` on initialization, which launches a background coroutine to trigger `repository.fetchChats()`.
 * * ## Alternatives / Why Not
 * - **Calling Suspend Fetch Directly:** Rejected calling the suspending `fetchChats` directly in the initialization flow chain, as it would block the `combine` block from registering and emitting cached local data immediately.
 * * Technical Details: Runs fetch logic in `viewModelScope.launch`. Requires mapping between domain constraints (Instants/Epochs) and View constraints (Formatted Strings).
 */
class ChatListViewModel(
    private val repository: ChatRepository,
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(ChatListState())
    val state = combine(
        _state,
        repository.getChats(),
        sessionStorage.observeAuthInfo(),
    ) { currentState, chats, authInfo ->
        if (authInfo == null) {
            return@combine ChatListState()
        }

        currentState.copy(
            chats = chats.map { it.toUi(authInfo.user.id) },
            localParticipant = authInfo.user.toUi(),
        )
    }
        .onStart {
            if (!hasLoadedInitialData) {
                loadChats()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ChatListState(),
        )

    fun onAction(action: ChatListAction) {
        when (action) {
            is ChatListAction.OnSelectChat -> {
                _state.update {
                    it.copy(
                        selectedChatId = action.chatId,
                    )
                }
            }
            ChatListAction.OnUserAvatarClick -> {
                _state.update {
                    it.copy(
                        isUserMenuOpen = true,
                    )
                }
            }
            ChatListAction.OnProfileSettingsClick,
            ChatListAction.OnLogoutClick,
            ChatListAction.OnDismissUserMenu,
            -> {
                _state.update {
                    it.copy(
                        isUserMenuOpen = false,
                    )
                }
            }
            else -> Unit
        }
    }

    private fun loadChats() {
        viewModelScope.launch {
            repository.fetchChats()
        }
    }
}
