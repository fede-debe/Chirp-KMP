package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.presentation.Res
import com.project.chat.presentation.check_icon
import com.project.chat.presentation.failed
import com.project.chat.presentation.loading_icon
import com.project.chat.presentation.sending
import com.project.chat.presentation.sent
import com.project.core.designsystem.theme.extended
import com.project.core.designsystem.theme.labelXSmall
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MessageStatus(
    status: ChatMessageDeliveryStatus,
    modifier: Modifier = Modifier,
) {
    val (text, icon, color) = when (status) {
        ChatMessageDeliveryStatus.SENDING -> Triple(
            stringResource(Res.string.sending),
            vectorResource(Res.drawable.loading_icon),
            MaterialTheme.colorScheme.extended.textDisabled,
        )
        ChatMessageDeliveryStatus.SENT -> Triple(
            stringResource(Res.string.sent),
            vectorResource(Res.drawable.check_icon),
            MaterialTheme.colorScheme.extended.textTertiary,
        )
        ChatMessageDeliveryStatus.FAILED -> Triple(
            stringResource(Res.string.failed),
            Icons.Default.Close,
            MaterialTheme.colorScheme.error,
        )
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelXSmall,
        )
    }
}
