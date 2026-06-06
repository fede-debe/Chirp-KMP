package com.project.chat.presentation.ui.manageChat

import androidx.lifecycle.ViewModel
import com.project.chat.presentation.components.manageChat.ManageChatAction
import com.project.chat.presentation.components.manageChat.ManageChatState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class ManageChatViewModel : ViewModel() {

    private val eventChannel = Channel<ManageChatEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(ManageChatState())
    val state = _state.asStateFlow()

    fun onAction(action: ManageChatAction) {
        when (action) {
            ManageChatAction.OnAddClick -> {}
            ManageChatAction.OnPrimaryActionClick -> {}
            ManageChatAction.OnDismissDialog -> {}
        }
    }
}
