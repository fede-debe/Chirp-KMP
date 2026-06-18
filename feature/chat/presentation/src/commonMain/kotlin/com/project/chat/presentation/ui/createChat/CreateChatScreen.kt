package com.project.chat.presentation.ui.createChat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.chat.domain.models.Chat
import com.project.chat.presentation.Res
import com.project.chat.presentation.components.manageChat.ManageChatAction
import com.project.chat.presentation.components.manageChat.ManageChatScreen
import com.project.chat.presentation.create_chat
import com.project.core.designsystem.components.dialogs.ChirpAdaptiveDialogSheetLayout
import com.project.core.presentation.util.ObserveAsEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateChatRoot(
    onDismiss: () -> Unit,
    onChatCreated: (Chat) -> Unit,
    viewModel: CreateChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CreateChatEvent.OnChatCreated -> onChatCreated(event.chat)
        }
    }

    ChirpAdaptiveDialogSheetLayout(
        onDismiss = onDismiss,
    ) {
        ManageChatScreen(
            headerText = stringResource(Res.string.create_chat),
            primaryButtonText = stringResource(Res.string.create_chat),
            state = state,
            onAction = { action ->
                when (action) {
                    ManageChatAction.OnDismissDialog -> onDismiss()
                    else -> Unit
                }
                viewModel.onAction(action)
            },
        )
    }
}
