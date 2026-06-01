package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.presentation.models.ChatMessageUi
import com.project.core.designSystem.components.avatar.ChatParticipantUi
import com.project.core.designSystem.theme.ChirpTheme
import com.project.core.designSystem.theme.extended
import com.project.core.presentation.util.UiText

@Composable
fun MessageListItemUi(
    chatMessageUi: ChatMessageUi,
    onMessageLongClick: () -> Unit,
    onDismissMessageMenu: () -> Unit,
    onDeleteClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        when (chatMessageUi) {
            is ChatMessageUi.DateSeparator -> {
                DateSeparatorUi(
                    date = chatMessageUi.date.asString(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is ChatMessageUi.LocalUserMessage -> {
                LocalUserMessage(
                    message = chatMessageUi,
                    onMessageLongClick = onMessageLongClick,
                    onDismissMessageMenu = onDismissMessageMenu,
                    onDeleteClick = onDeleteClick,
                    onRetryClick = onRetryClick,
                )
            }
            is ChatMessageUi.OtherUserMessage -> {
                OtherUserMessage(
                    message = chatMessageUi,
                )
            }
        }
    }
}

@Composable
private fun DateSeparatorUi(
    date: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = date,
            modifier = Modifier
                .padding(horizontal = 40.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.extended.textPlaceholder,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
@Preview
fun MessageListItemLocalMessageUiPreview() {
    ChirpTheme {
        MessageListItemUi(
            chatMessageUi = ChatMessageUi.LocalUserMessage(
                id = "1",
                content = "Hello world, this is a preview message that spans multiple lines",
                deliveryStatus = ChatMessageDeliveryStatus.SENT,
                isMenuOpen = true,
                formattedSentTime = UiText.DynamicString("Friday 2:20pm"),
            ),
            onRetryClick = {},
            onMessageLongClick = {},
            onDismissMessageMenu = {},
            onDeleteClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
    }
}

@Composable
@Preview
fun MessageListItemLocalMessageRetryUiPreview() {
    ChirpTheme {
        MessageListItemUi(
            chatMessageUi = ChatMessageUi.LocalUserMessage(
                id = "1",
                content = "Hello world, this is a preview message that spans multiple lines",
                deliveryStatus = ChatMessageDeliveryStatus.FAILED,
                isMenuOpen = false,
                formattedSentTime = UiText.DynamicString("Friday 2:20pm"),
            ),
            onRetryClick = {},
            onMessageLongClick = {},
            onDismissMessageMenu = {},
            onDeleteClick = {},
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@Composable
@Preview
fun MessageListItemOtherMessageUiPreview() {
    ChirpTheme {
        MessageListItemUi(
            chatMessageUi = ChatMessageUi.OtherUserMessage(
                id = "1",
                content = "Hello world, this is a preview message that spans multiple lines",
                formattedSentTime = UiText.DynamicString("Friday 2:20pm"),
                sender = ChatParticipantUi(
                    id = "1",
                    username = "Philipp",
                    initials = "PH",
                ),
            ),
            onRetryClick = {},
            onMessageLongClick = {},
            onDismissMessageMenu = {},
            onDeleteClick = {},
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}
