package com.project.chat.presentation.ui.chatList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class ChatListViewModel : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(ChatListState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                // TODO: Load initial data here

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
            is ChatListAction.OnButtonAClick -> {
                // TODO: Handle action
            }

            is ChatListAction.OnTypeInTextFieldB -> {
                // TODO: Handle text input
            }
        }
    }
}
