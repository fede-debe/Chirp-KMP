@file:OptIn(ExperimentalCoroutinesApi::class)

package com.project.chat.presentation.ui.chatDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.chat.domain.chat.ChatRepository
import com.project.chat.presentation.mappers.toUi
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val chatRepository: ChatRepository,
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private val chatId = MutableStateFlow<String?>(null)

    private var hasLoadedInitialData = false

    private val chatInfoFlow = chatId
        .flatMapLatest { id ->
            if (id != null) {
                chatRepository.getChatInfoById(id)
            } else {
                emptyFlow()
            }
        }

    private val _state = MutableStateFlow(ChatDetailState())

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
        )
    }

    val state = chatId
        .flatMapLatest { id ->
            if (id != null) {
                stateWithMessages
            } else {
                _state
            }
        }
        .onStart {
            if (!hasLoadedInitialData) {
                /** Load initial data here **/
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
            else -> Unit
        }
    }

    private fun switchChat(id: String?) {
        chatId.update { id }
        viewModelScope.launch {
            id?.let {
                chatRepository.fetchChatById(id)
            }
        }
    }
}
