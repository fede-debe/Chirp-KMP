@file:OptIn(ExperimentalCoroutinesApi::class)

package com.project.chat.presentation.ui.chatDetail

import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.chat.domain.attachment.AttachmentService
import com.project.chat.domain.attachment.AudioPlayer
import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.ImageCompressor
import com.project.chat.domain.attachment.ImageSaver
import com.project.chat.domain.chat.ChatConnectionClient
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.domain.message.MessageRepository
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ConnectionState
import com.project.chat.domain.models.MessageAttachment
import com.project.chat.domain.models.OutgoingNewMessage
import com.project.chat.presentation.Res
import com.project.chat.presentation.config.ChatFeatureFlags
import com.project.chat.presentation.mappers.toUi
import com.project.chat.presentation.mappers.toUiList
import com.project.chat.presentation.mediapicker.PickedAttachment
import com.project.chat.presentation.mic_permission_denied
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.models.MessageUi
import com.project.chat.presentation.models.PendingAttachmentStatus
import com.project.chat.presentation.models.PendingAttachmentUi
import com.project.chat.presentation.playback_failed
import com.project.chat.presentation.record_failed
import com.project.chat.presentation.today
import com.project.core.domain.auth.SessionStorage
import com.project.core.domain.util.DataErrorException
import com.project.core.domain.util.Paginator
import com.project.core.domain.util.Result
import com.project.core.domain.util.onFailure
import com.project.core.domain.util.onSuccess
import com.project.core.presentation.util.UiText
import com.project.core.presentation.util.toUiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
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
    private val imageCompressor: ImageCompressor,
    private val attachmentService: AttachmentService,
    private val imageSaver: ImageSaver,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    private val eventChannel = Channel<ChatDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    private var recordingTickerJob: Job? = null

    // Id of the newest message we've already accounted for, per chat. Drives new-message detection for
    // autoscroll + the unseen-messages FAB badge. Reset on every chat switch (see switchChat).
    private var lastSeenNewestMessageId: String? = null

    // Outgoing typing-indicator state. `typingChatId` is the chat we've currently signalled "typing" for
    // (null = not signalling). The heartbeat re-sends TYPING_STARTED to keep the server's 3s timer alive;
    // the idle job sends TYPING_STOPPED once the user pauses.
    private var typingChatId: String? = null
    private var typingHeartbeatJob: Job? = null
    private var typingIdleJob: Job? = null

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
                // load first page
                loadNextItems()
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

    private val canSendMessage = combine(
        snapshotFlow { _state.value.messageTextFieldState.text.toString() }
            .map { it.isNotBlank() }
            .distinctUntilChanged(),
        connectionClient.connectionState,
        _state
            .map { it.pendingAttachments to it.isSending }
            .distinctUntilChanged(),
    ) { hasText, connectionState, (pendingAttachments, isSending) ->
        val hasReadyAttachment = pendingAttachments.any {
            it.status == PendingAttachmentStatus.READY
        }
        (hasText || hasReadyAttachment) &&
            connectionState == ConnectionState.CONNECTED &&
            !isSending
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
                observeTypingUsers()
                if (ChatFeatureFlags.typing) observeOutgoingTyping()
                observeChatRemovals()
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
            ChatDetailAction.OnChatOptionsClick -> onChatOptionsClick()
            is ChatDetailAction.OnDeleteMessageClick -> deleteMessage(action.message)
            ChatDetailAction.OnDismissChatOptions -> onDismissChatOptions()
            ChatDetailAction.OnDismissMessageMenu -> onDismissMessageMenu()
            ChatDetailAction.OnLeaveChatClick -> onLeaveChatClick()
            is ChatDetailAction.OnMessageLongClick -> onMessageLongClick(action.message)
            is ChatDetailAction.OnRetryClick -> retryMessage(action.message)
            ChatDetailAction.OnScrollToTop -> onScrollToTop()
            ChatDetailAction.OnSendMessageClick -> sendMessage()
            is ChatDetailAction.OnAttachmentsPicked -> onAttachmentsPicked(action.attachments)
            is ChatDetailAction.OnRemoveAttachment -> onRemoveAttachment(action.id)
            ChatDetailAction.OnRetryPaginationClick -> retryPagination()
            ChatDetailAction.OnHideBanner -> hideBanner()
            is ChatDetailAction.OnTopVisibleIndexChanged -> updateBanner(action.topVisibleIndex)
            is ChatDetailAction.OnFirstVisibleIndexChanged -> updateNearBottom(action.index)
            is ChatDetailAction.OnAttachmentClick -> onAttachmentClick(action.attachment)
            ChatDetailAction.OnDismissAttachmentViewer -> onDismissAttachmentViewer()
            ChatDetailAction.OnSaveOpenedAttachment -> saveOpenedAttachment()
            ChatDetailAction.OnAttachClick -> setAttachmentSheetOpen(true)
            ChatDetailAction.OnDismissAttachmentSheet -> setAttachmentSheetOpen(false)
            // Close the sheet and record the choice; the Root launches the picker/camera only once
            // the sheet has fully closed (see ChatDetailState.pendingAttachmentSource).
            ChatDetailAction.OnTakePhotoClick -> chooseAttachmentSource(AttachmentSource.CAMERA)
            ChatDetailAction.OnPickFromGalleryClick ->
                chooseAttachmentSource(AttachmentSource.GALLERY)
            ChatDetailAction.OnAttachmentLaunchHandled -> {
                _state.update { it.copy(pendingAttachmentSource = null) }
            }
            ChatDetailAction.OnStartRecording -> startRecording()
            ChatDetailAction.OnStopRecording -> stopRecording()
            ChatDetailAction.OnCancelRecording -> cancelRecording()
            ChatDetailAction.OnPauseRecording -> pauseRecording()
            ChatDetailAction.OnResumeRecording -> resumeRecording()
            ChatDetailAction.OnRecordPermissionDenied -> onRecordPermissionDenied()
            is ChatDetailAction.OnPlayAttachment -> playAttachment(action.attachment)
            ChatDetailAction.OnPauseAttachment -> audioPlayer.pause()
            else -> Unit
        }
    }

    private fun setAttachmentSheetOpen(isOpen: Boolean) {
        _state.update { it.copy(isAttachmentSheetOpen = isOpen) }
    }

    private fun chooseAttachmentSource(source: AttachmentSource) {
        _state.update {
            it.copy(
                isAttachmentSheetOpen = false,
                pendingAttachmentSource = source,
            )
        }
    }

    private fun onAttachmentClick(attachment: MessageAttachmentUi) {
        _state.update { it.copy(openedAttachment = attachment) }
    }

    private fun onDismissAttachmentViewer() {
        _state.update { it.copy(openedAttachment = null, isSavingAttachment = false) }
    }

    private fun saveOpenedAttachment() {
        val attachment = state.value.openedAttachment ?: return
        if (state.value.isSavingAttachment) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingAttachment = true) }

            // Pull the full-size bytes from storage, then hand them to the platform saver.
            when (val download = attachmentService.downloadImage(attachment.url)) {
                is Result.Success -> {
                    imageSaver
                        .saveToGallery(
                            bytes = download.data,
                            fileName = attachment.fileName,
                            mimeType = attachment.mimeType,
                        )
                        .onSuccess {
                            _state.update {
                                it.copy(isSavingAttachment = false, openedAttachment = null)
                            }
                            eventChannel.send(ChatDetailEvent.OnAttachmentSaved)
                        }
                        .onFailure { error ->
                            _state.update { it.copy(isSavingAttachment = false) }
                            eventChannel.send(ChatDetailEvent.OnError(error.toUiText()))
                        }
                }
                is Result.Failure -> {
                    _state.update { it.copy(isSavingAttachment = false) }
                    eventChannel.send(ChatDetailEvent.OnError(download.error.toUiText()))
                }
            }
        }
    }

    private fun updateNearBottom(firstVisibleIndex: Int) {
        val isNearBottom = firstVisibleIndex <= 3
        _state.update {
            it.copy(
                isNearBottom = isNearBottom,
                // Reaching the bottom (incl. tapping the scroll-to-latest FAB) marks new messages as seen.
                hasUnseenMessages = if (isNearBottom) false else it.hasUnseenMessages,
            )
        }
    }

    private fun updateBanner(topVisibleIndex: Int) {
        val visibleDate = calculateBannerDateFromIndex(
            messages = state.value.messages,
            index = topVisibleIndex,
        )

        _state.update {
            it.copy(
                bannerState = BannerState(
                    formattedDate = visibleDate,
                    isVisible = visibleDate != null,
                ),
            )
        }
    }

    private fun calculateBannerDateFromIndex(
        messages: List<MessageUi>,
        index: Int,
    ): UiText? {
        if (messages.isEmpty() || index < 0 || index >= messages.size) {
            return null
        }

        val nearestDateSeparator = (index until messages.size)
            .asSequence()
            .mapNotNull { index ->
                val item = messages.getOrNull(index)
                if (item is MessageUi.DateSeparator) item.date else null
            }
            .firstOrNull()

        return when (nearestDateSeparator) {
            is UiText.Resource -> {
                if (nearestDateSeparator.id == Res.string.today) null else nearestDateSeparator
            }
            else -> nearestDateSeparator
        }
    }

    private fun hideBanner() {
        _state.update {
            it.copy(
                bannerState = it.bannerState.copy(
                    isVisible = false,
                ),
            )
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
        val currentChatId = chatIdFlow.value ?: return
        val content = state.value.messageTextFieldState.text.toString().trim()
        val readyAttachments = state.value.pendingAttachments
            .filter { it.status == PendingAttachmentStatus.READY }

        if ((content.isBlank() && readyAttachments.isEmpty()) || state.value.isSending) {
            return
        }

        viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    isSending = true,
                    pendingAttachments = current.pendingAttachments.map {
                        if (it.status == PendingAttachmentStatus.READY) {
                            it.copy(status = PendingAttachmentStatus.UPLOADING)
                        } else {
                            it
                        }
                    },
                )
            }

            // Upload every ready image to Supabase first; only then send the message with the
            // resulting attachment references over the websocket.
            val uploaded = mutableListOf<MessageAttachment>()
            for (attachment in readyAttachments) {
                when (
                    val result = attachmentService.uploadAttachment(
                        chatId = currentChatId,
                        fileName = attachment.fileName,
                        mimeType = attachment.mimeType,
                        bytes = attachment.bytes,
                        durationInSeconds = attachment.durationInSeconds,
                    )
                ) {
                    is Result.Success -> uploaded.add(result.data)
                    is Result.Failure -> {
                        _state.update { current ->
                            current.copy(
                                isSending = false,
                                pendingAttachments = current.pendingAttachments.map {
                                    when {
                                        it.id == attachment.id ->
                                            it.copy(status = PendingAttachmentStatus.FAILED)
                                        it.status == PendingAttachmentStatus.UPLOADING ->
                                            it.copy(status = PendingAttachmentStatus.READY)
                                        else -> it
                                    }
                                },
                            )
                        }
                        eventChannel.send(ChatDetailEvent.OnError(result.error.toUiText()))
                        return@launch
                    }
                }
            }

            val message = OutgoingNewMessage(
                chatId = currentChatId,
                messageId = Uuid.random().toString(),
                content = content,
                attachments = uploaded,
            )

            messageRepository
                .sendMessage(message)
                .onSuccess {
                    state.value.messageTextFieldState.clearText()
                    _state.update {
                        it.copy(
                            pendingAttachments = emptyList(),
                            isSending = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isSending = false) }
                    eventChannel.send(ChatDetailEvent.OnError(error.toUiText()))
                }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun onAttachmentsPicked(picked: List<PickedAttachment>) {
        val remainingSlots = (MAX_ATTACHMENTS - state.value.pendingAttachments.size)
            .coerceAtLeast(0)
        if (remainingSlots == 0 || picked.isEmpty()) {
            return
        }

        val newAttachments = picked.take(remainingSlots).map {
            PendingAttachmentUi(
                id = Uuid.random().toString(),
                fileName = it.fileName,
                mimeType = it.mimeType,
                bytes = it.bytes,
                status = PendingAttachmentStatus.PROCESSING,
            )
        }
        _state.update { it.copy(pendingAttachments = it.pendingAttachments + newAttachments) }

        // Compress each picked image off-thread, then flip it to READY (swaps file icon → thumbnail).
        newAttachments.forEach { pending ->
            viewModelScope.launch {
                val compressed = imageCompressor.compress(pending.bytes, pending.mimeType)
                _state.update { current ->
                    current.copy(
                        pendingAttachments = current.pendingAttachments.map {
                            if (it.id == pending.id) {
                                it.copy(
                                    bytes = compressed.bytes,
                                    mimeType = compressed.mimeType,
                                    status = PendingAttachmentStatus.READY,
                                )
                            } else {
                                it
                            }
                        },
                    )
                }
            }
        }
    }

    private fun onRemoveAttachment(id: String) {
        _state.update { current ->
            current.copy(
                pendingAttachments = current.pendingAttachments.filterNot { it.id == id },
            )
        }
    }

    private fun startRecording() {
        if (state.value.recording != null) return
        viewModelScope.launch {
            val started = audioRecorder.start()
            if (!started) {
                eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.record_failed)))
                return@launch
            }
            _state.update { it.copy(recording = RecordingState(elapsedSeconds = 0)) }
            recordingTickerJob?.cancel()
            recordingTickerJob = viewModelScope.launch {
                while (true) {
                    delay(1_000)
                    _state.update { current ->
                        val active = current.recording ?: return@update current
                        if (active.isPaused) return@update current
                        current.copy(recording = active.copy(elapsedSeconds = active.elapsedSeconds + 1))
                    }
                }
            }
        }
    }

    private fun pauseRecording() {
        val active = state.value.recording ?: return
        if (active.isPaused) return
        audioRecorder.pause()
        _state.update { it.copy(recording = it.recording?.copy(isPaused = true)) }
    }

    private fun resumeRecording() {
        val active = state.value.recording ?: return
        if (!active.isPaused) return
        audioRecorder.resume()
        _state.update { it.copy(recording = it.recording?.copy(isPaused = false)) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun stopRecording() {
        if (state.value.recording == null) return
        recordingTickerJob?.cancel()
        recordingTickerJob = null
        viewModelScope.launch {
            val audio = audioRecorder.stop()
            _state.update { it.copy(recording = null) }
            if (audio == null) {
                eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.record_failed)))
                return@launch
            }
            val remainingSlots = (MAX_ATTACHMENTS - state.value.pendingAttachments.size)
                .coerceAtLeast(0)
            if (remainingSlots == 0) return@launch
            val pending = PendingAttachmentUi(
                id = Uuid.random().toString(),
                fileName = audio.fileName,
                mimeType = audio.mimeType,
                bytes = audio.bytes,
                status = PendingAttachmentStatus.READY,
                durationInSeconds = audio.durationInSeconds,
            )
            _state.update { it.copy(pendingAttachments = it.pendingAttachments + pending) }
        }
    }

    private fun cancelRecording() {
        recordingTickerJob?.cancel()
        recordingTickerJob = null
        audioRecorder.cancel()
        _state.update { it.copy(recording = null) }
    }

    private fun onRecordPermissionDenied() {
        viewModelScope.launch {
            eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.mic_permission_denied)))
        }
    }

    private fun playAttachment(attachment: MessageAttachmentUi) {
        viewModelScope.launch {
            try {
                audioPlayer.play(id = attachment.url, url = attachment.url)
            } catch (e: Exception) {
                eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.playback_failed)))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingTickerJob?.cancel()
        typingHeartbeatJob?.cancel()
        typingIdleJob?.cancel()
        audioPlayer.release()
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
        val newMessages = chatIdFlow.flatMapLatest { chatId ->
            if (chatId != null) {
                messageRepository.getMessagesForChat(chatId)
            } else {
                emptyFlow()
            }
        }

        val isNearBottom = state.map { it.isNearBottom }.distinctUntilChanged()

        combine(
            newMessages,
            isNearBottom,
            sessionStorage.observeAuthInfo(),
        ) { messages, isNearBottom, authInfo ->
            // The newest message is the one with the latest timestamp — the DB and the date-separated UI
            // list order things differently, so first/last position is not reliable. A genuinely new
            // message is one whose id differs from the last id we recorded for this chat (reset to null on
            // every chat switch, so opening a chat doesn't count its existing newest as "new").
            val newest = messages.maxByOrNull { it.message.createdAt }
            val newestId = newest?.message?.id
            val newestSenderId = newest?.message?.senderId
            val isNewMessage = newestId != null &&
                lastSeenNewestMessageId != null &&
                newestId != lastSeenNewestMessageId
            lastSeenNewestMessageId = newestId

            if (isNewMessage) {
                when {
                    // At the bottom: follow the conversation by scrolling to the newest message.
                    isNearBottom -> eventChannel.send(ChatDetailEvent.OnNewMessage)
                    // Scrolled up + from someone else: badge the FAB instead of yanking the user down.
                    newestSenderId != authInfo?.user?.id -> {
                        _state.update { it.copy(hasUnseenMessages = true) }
                    }
                }
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

    /**
     * Folds the incoming typing-presence stream (scoped to the open chat) into the list of usernames shown
     * in the UI. `flatMapLatest` on the chat id resets the fold on every chat switch, so stale typers from a
     * previous chat never leak in. The server only broadcasts to *other* participants, so the local user is
     * already excluded. `scan` keeps a userId→username map so a "stopped" event removes exactly one typer.
     */
    private fun observeTypingUsers() {
        chatIdFlow
            .flatMapLatest { chatId ->
                if (chatId == null) {
                    flowOf(emptyList())
                } else {
                    connectionClient.typingUsers
                        .filter { it.chatId == chatId }
                        .scan(emptyMap<String, String>()) { typers, event ->
                            if (event.isTyping) {
                                typers + (event.userId to event.username)
                            } else {
                                typers - event.userId
                            }
                        }
                        .map { it.values.toList() }
                }
            }
            .distinctUntilChanged()
            .onEach { usernames ->
                _state.update { it.copy(typingUsernames = usernames) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Translates composer text changes into outgoing typing signals: non-blank text starts (and keeps)
     * typing, blank text stops it. The idle timeout and message-send/leave (which clear the field) flow
     * through the same blank-text path.
     */
    /**
     * If the currently-open chat is removed under us (admin kicked us, or the chat was deleted), leave the
     * screen. The local chat row is already deleted by the connection client, so the list updates on its own.
     */
    private fun observeChatRemovals() {
        connectionClient.chatRemovals
            .onEach { removal ->
                if (removal.chatId == chatIdFlow.value) {
                    stopTypingSignal()
                    chatIdFlow.update { null }
                    _state.update {
                        it.copy(
                            chatUi = null,
                            messages = emptyList(),
                            bannerState = BannerState(),
                        )
                    }
                    eventChannel.send(ChatDetailEvent.OnChatRemoved)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeOutgoingTyping() {
        snapshotFlow { _state.value.messageTextFieldState.text.toString() }
            .drop(1) // ignore the initial value emitted on subscription
            .onEach { text ->
                if (text.isNotBlank()) {
                    onUserTyping()
                } else {
                    stopTypingSignal()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onUserTyping() {
        val chatId = chatIdFlow.value ?: return

        if (typingChatId != chatId) {
            stopTypingSignal()
            typingChatId = chatId
            sendTyping(chatId, isTyping = true)
            // The server auto-stops 3s after the last TYPING_STARTED, so refresh it while typing continues.
            typingHeartbeatJob = viewModelScope.launch {
                while (true) {
                    delay(TYPING_HEARTBEAT_MS)
                    sendTyping(chatId, isTyping = true)
                }
            }
        }

        // Restart the idle countdown on every keystroke.
        typingIdleJob?.cancel()
        typingIdleJob = viewModelScope.launch {
            delay(TYPING_IDLE_TIMEOUT_MS)
            stopTypingSignal()
        }
    }

    private fun stopTypingSignal() {
        typingHeartbeatJob?.cancel()
        typingHeartbeatJob = null
        typingIdleJob?.cancel()
        typingIdleJob = null

        val chatId = typingChatId ?: return
        typingChatId = null
        sendTyping(chatId, isTyping = false)
    }

    private fun sendTyping(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            if (isTyping) {
                connectionClient.sendTypingStarted(chatId)
            } else {
                connectionClient.sendTypingStopped(chatId)
            }
        }
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

                    eventChannel.send(
                        ChatDetailEvent.OnChatLeft,
                    )
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
        // Stop signalling typing for the previous chat (the composer text can survive a chat switch, so the
        // text-change observer won't necessarily fire to stop it).
        stopTypingSignal()
        // Reset new-message tracking so the incoming chat doesn't treat its existing newest message as new
        // (which would wrongly badge the FAB), and clear any leftover badge from the previous chat.
        lastSeenNewestMessageId = null
        _state.update { it.copy(hasUnseenMessages = false) }
        chatIdFlow.update { chatId }
        viewModelScope.launch {
            chatId?.let {
                chatRepository.fetchChatById(chatId)
            }
        }
    }

    private companion object {
        const val MAX_ATTACHMENTS = 10

        // Re-send TYPING_STARTED every 2s (below the server's 3s auto-stop) so the indicator stays alive
        // while the user keeps typing; stop signalling 3s after the last keystroke.
        const val TYPING_HEARTBEAT_MS = 2_000L
        const val TYPING_IDLE_TIMEOUT_MS = 3_000L
    }
}
