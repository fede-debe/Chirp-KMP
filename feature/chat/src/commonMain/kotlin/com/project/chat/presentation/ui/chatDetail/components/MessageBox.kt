package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.chat.domain.models.ConnectionState
import com.project.chat.presentation.Res
import com.project.chat.presentation.cloud_off_icon
import com.project.chat.presentation.models.PendingAttachmentUi
import com.project.chat.presentation.send
import com.project.chat.presentation.send_a_message
import com.project.chat.presentation.upload_icon
import com.project.chat.presentation.util.toUiText
import com.project.core.designsystem.components.buttons.ChirpButton
import com.project.core.designsystem.components.textFields.ChirpMultiLineTextField
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MessageBox(
    messageTextFieldState: TextFieldState,
    isSendButtonEnabled: Boolean,
    connectionState: ConnectionState,
    pendingAttachments: List<PendingAttachmentUi>,
    isSending: Boolean,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pendingAttachments.isNotEmpty()) {
            ComposerAttachmentsRow(
                attachments = pendingAttachments,
                onRemove = onRemoveAttachment,
            )
        }
        ChirpMultiLineTextField(
            state = messageTextFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    val isModifierKeyPressed = keyEvent.isMetaPressed || keyEvent.isCtrlPressed
                    val isSendShortcutPressed = isModifierKeyPressed &&
                        keyEvent.key == Key.Enter &&
                        keyEvent.type == KeyEventType.KeyDown

                    if (isSendShortcutPressed) {
                        onSendClick()
                        true
                    } else {
                        false
                    }
                },
            placeholder = stringResource(Res.string.send_a_message),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
            ),
            onKeyboardAction = onSendClick,
            bottomContent = {
                IconButton(
                    onClick = onAttachClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.upload_icon),
                        contentDescription = "Attach images",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.extended.textSecondary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.cloud_off_icon),
                            contentDescription = connectionState.toUiText().asString(),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.extended.textDisabled,
                        )
                        Text(
                            text = connectionState.toUiText().asString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.extended.textDisabled,
                        )
                    }
                }
                ChirpButton(
                    text = stringResource(Res.string.send),
                    onClick = onSendClick,
                    enabled = isConnected && isSendButtonEnabled,
                    isLoading = isSending,
                )
            },
        )
    }
}

@Composable
@Preview
fun MessageBoxPreview() {
    ChirpTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            MessageBox(
                messageTextFieldState = rememberTextFieldState(),
                isSendButtonEnabled = true,
                connectionState = ConnectionState.CONNECTED,
                pendingAttachments = emptyList(),
                isSending = false,
                onSendClick = {},
                onAttachClick = {},
                onRemoveAttachment = {},
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }
    }
}
