@file:OptIn(ExperimentalCoroutinesApi::class)

package com.project.chat.presentation.ui.chatDetail

import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.chat.domain.chat.ChatConnectionClient
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.domain.message.MessageRepository
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ConnectionState
import com.project.chat.domain.models.OutgoingNewMessage
import com.project.chat.presentation.mappers.toUi
import com.project.chat.presentation.mappers.toUiList
import com.project.chat.presentation.models.MessageUi
import com.project.core.domain.auth.SessionStorage
import com.project.core.domain.util.DataErrorException
import com.project.core.domain.util.Paginator
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.presentation.util.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatDetailViewModel(
    private val chatRepository: ChatRepository,
    private val sessionStorage: SessionStorage,
    private val messageRepository: MessageRepository,
    private val connectionClient: ChatConnectionClient,
) : ViewModel() {

    private val eventChannel = Channel<ChatDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    private val chatIdFlow = MutableStateFlow<String?>(null)

    private var hasLoadedInitialData = false

    /**
     * Sets up and manages the pagination state for a specific chat instance, triggering network fetches and updating the UI state accordingly.
     *
     * ## Strategy / Decisions
     * - **Database as Single Source of Truth:** The pagination `onRequest` block does not manually append messages to a local list. It delegates fetching to the `messageRepository`, which inserts them into the local database. The UI automatically observes the DB, so we get UI updates "for free" once the DB synchronizes.
     * - **State Management Constraints:** Because the View Model outlives the detail screen (living as long as the list + detail screens combined), pagination state (`endReached`) would persist across different chats. It must be explicitly reset when setting up a new chat to avoid locking up pagination.
     *
     * ## How It Works
     * 1. **Chat Switch Trigger:** A flow (`chatIdFlow`) listens for chat selection. If a valid ID is received, `setupPaginationForChat()` is called. If the chat is unselected (or navigated back), `currentPagination` is cleared (`null`).
     * 2. **Initialization:** A `Pagination` instance is instantiated with `initialKey = null` (signaling the first page).
     * 3. **Callback Flow:**
     * - `onLoadUpdated`: Updates the UI state's `isPaginationLoading` flag.
     * - `onRequest`: Invokes `messageRepository.fetchMessages()` using the `beforeTimestamp` key.
     * - `getNextKey`: Finds the oldest message in the fetched chunk using `minOfOrNull` on the `createdAt` date, converting that Instant to a String to act as the next cursor.
     * - `onError`: Checks if the throwable is a `DataErrorException`, extracts the UI text, and sends a `DetailEvent.OnError` to trigger a snackbar.
     * - `onSuccess`: Checks if the fetched messages list is empty; if so, flips the state's `endReached` to true.
     * 4. **Cleanup & Launch:** Resets `endReached` and `isPaginationLoading` to false, then immediately triggers `loadNextItems()` in the `viewModelScope` to fetch the first chunk.
     *
     * ## Alternatives / Why Not
     * - *Why not use the new key in onSuccess?* The new key passed to `onSuccess` is ignored because determining if the end of pagination is reached relies solely on whether the API returned an empty list. It's not worth trying to find a timestamp in an empty list.
     *
     * ## Technical Details
     * - **Types:** Key is `String?` (timestamp), Item is `ChatMessage`.
     * - **Dependencies:** Relies on `messageRepository` for fetching and an event channel for error UI events.
     */
    private var currentPaginator: Paginator<String?, ChatMessage>? = null

    private val chatInfoFlow = chatIdFlow
        .onEach { chatId ->
            if (chatId != null) {
                setupPaginatorForChat(chatId)
            } else {
                currentPaginator = null
            }
        }
        .flatMapLatest { chatId ->
            if (chatId != null) {
                chatRepository.getChatInfoById(chatId)
            } else {
                emptyFlow()
            }
        }

    private val _state = MutableStateFlow(ChatDetailState())

    private val canSendMessage = snapshotFlow { _state.value.messageTextFieldState.text.toString() }
        .map { it.isBlank() }
        .combine(connectionClient.connectionState) { isMessageBlank, connectionState ->
            !isMessageBlank && connectionState == ConnectionState.CONNECTED
        }

    private val stateWithMessages = combine(
        _state,
        chatInfoFlow,
        sessionStorage.observeAuthInfo(),
    ) { currentState, chatInfo, authInfo ->
        if (authInfo == null) {
            return@combine ChatDetailState()
        }

        currentState.copy(
            chatUi = chatInfo.chat.toUi(authInfo.user.id),
            messages = chatInfo.messages.toUiList(authInfo.user.id),
        )
    }

    val state = chatIdFlow
        .flatMapLatest { chatId ->
            if (chatId != null) {
                stateWithMessages
            } else {
                _state
            }
        }
        .onStart {
            if (!hasLoadedInitialData) {
                observeConnectionState()
                observeChatMessages()
                observeCanSendMessage()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ChatDetailState(),
        )

    fun onAction(action: ChatDetailAction) {
        when (action) {
            is ChatDetailAction.OnSelectChat -> switchChat(action.chatId)
            ChatDetailAction.OnBackClick -> {}
            ChatDetailAction.OnChatMembersClick -> {}
            ChatDetailAction.OnChatOptionsClick -> onChatOptionsClick()
            is ChatDetailAction.OnDeleteMessageClick -> deleteMessage(action.message)
            ChatDetailAction.OnDismissChatOptions -> onDismissChatOptions()
            ChatDetailAction.OnDismissMessageMenu -> onDismissMessageMenu()
            ChatDetailAction.OnLeaveChatClick -> onLeaveChatClick()
            is ChatDetailAction.OnMessageLongClick -> onMessageLongClick(action.message)
            is ChatDetailAction.OnRetryClick -> retryMessage(action.message)
            ChatDetailAction.OnScrollToTop -> onScrollToTop()
            ChatDetailAction.OnSendMessageClick -> sendMessage()
            ChatDetailAction.OnRetryPaginationClick -> retryPagination()
            else -> Unit
        }
    }

    private fun retryPagination() = loadNextItems()

    private fun onScrollToTop() = loadNextItems()

    private fun loadNextItems() {
        viewModelScope.launch {
            currentPaginator?.loadNextItems()
        }
    }

    private fun onDismissMessageMenu() {
        _state.update {
            it.copy(
                messageWithOpenMenu = null,
            )
        }
    }

    private fun onMessageLongClick(message: MessageUi.LocalUserMessage) {
        _state.update {
            it.copy(
                messageWithOpenMenu = message,
            )
        }
    }

    private fun deleteMessage(message: MessageUi.LocalUserMessage) {
        viewModelScope.launch {
            messageRepository
                .deleteMessage(message.id)
                .onFailure { error ->
                    eventChannel.send(ChatDetailEvent.OnError(error.toUiText()))
                }
        }
    }

    private fun retryMessage(message: MessageUi.LocalUserMessage) {
        viewModelScope.launch {
            messageRepository
                .retryMessage(message.id)
                .onFailure { error ->
                    eventChannel.send(ChatDetailEvent.OnError(error.toUiText()))
                }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun sendMessage() {
        val currentChatId = chatIdFlow.value
        val content = state.value.messageTextFieldState.text.toString().trim()
        if (content.isBlank() || currentChatId == null) {
            return
        }

        viewModelScope.launch {
            val message = OutgoingNewMessage(
                chatId = currentChatId,
                messageId = Uuid.random().toString(),
                content = content,
            )

            messageRepository
                .sendMessage(message)
                .onSuccess {
                    state.value.messageTextFieldState.clearText()
                }
                .onFailure { error ->
                    eventChannel.send(ChatDetailEvent.OnError(error.toUiText()))
                }
        }
    }

    private fun observeCanSendMessage() {
        canSendMessage.onEach { canSend ->
            _state.update {
                it.copy(
                    canSendMessage = canSend,
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeChatMessages() {
        val currentMessages = state
            .map { it.messages }
            .distinctUntilChanged()

        val newMessages = chatIdFlow.flatMapLatest { chatId ->
            if (chatId != null) {
                messageRepository.getMessagesForChat(chatId)
            } else {
                emptyFlow()
            }
        }

        val isNearBottom = state.map { it.isNearBottom }.distinctUntilChanged()

        combine(
            currentMessages,
            newMessages,
            isNearBottom,
        ) { currentMessages, newMessages, isNearBottom ->
            val lastNewId = newMessages.lastOrNull()?.message?.id
            val lastCurrentId = currentMessages.lastOrNull()?.id

            if (lastNewId != lastCurrentId && isNearBottom) {
                eventChannel.send(ChatDetailEvent.OnNewMessage)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeConnectionState() {
        connectionClient
            .connectionState
            .onEach { connectionState ->
                if (connectionState == ConnectionState.CONNECTED) {
                    currentPaginator?.loadNextItems()
                }

                _state.update {
                    it.copy(
                        connectionState = connectionState,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun setupPaginatorForChat(chatId: String) {
        currentPaginator = Paginator(
            initialKey = null,
            onLoadUpdated = { isLoading ->
                _state.update { it.copy(isPaginationLoading = isLoading) }
            },
            onRequest = { beforeTimestamp ->
                messageRepository.fetchMessages(chatId, beforeTimestamp)
            },
            getNextKey = { messages ->
                messages.minOfOrNull { it.createdAt }?.toString()
            },
            onError = { throwable ->
                if (throwable is DataErrorException) {
                    _state.update {
                        it.copy(
                            paginationError = throwable.error.toUiText(),
                        )
                    }
                }
            },
            onSuccess = { messages, _ ->
                _state.update {
                    it.copy(
                        endReached = messages.isEmpty(),
                        paginationError = null,
                    )
                }
            },
        )

        _state.update {
            it.copy(
                endReached = false,
                isPaginationLoading = false,
            )
        }
    }

    private fun onLeaveChatClick() {
        val chatId = chatIdFlow.value ?: return

        _state.update {
            it.copy(
                isChatOptionsOpen = false,
            )
        }

        viewModelScope.launch {
            chatRepository
                .leaveChat(chatId)
                .onSuccess {
                    _state.value.messageTextFieldState.clearText()

                    chatIdFlow.update { null }
                    _state.update {
                        it.copy(
                            chatUi = null,
                            messages = emptyList(),
                            bannerState = BannerState(),
                        )
                    }
                }
                .onFailure { error ->
                    eventChannel.send(
                        ChatDetailEvent.OnError(
                            error.toUiText(),
                        ),
                    )
                }
        }
    }

    private fun onDismissChatOptions() {
        _state.update {
            it.copy(
                isChatOptionsOpen = false,
            )
        }
    }

    private fun onChatOptionsClick() {
        _state.update {
            it.copy(
                isChatOptionsOpen = true,
            )
        }
    }

    private fun switchChat(chatId: String?) {
        chatIdFlow.update { chatId }
        viewModelScope.launch {
            chatId?.let {
                chatRepository.fetchChatById(chatId)
            }
        }
    }
}
