package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.chat.domain.attachment.AudioPlayer
import com.project.chat.presentation.Res
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.pause_icon
import com.project.chat.presentation.play_icon
import com.project.chat.presentation.util.formatDuration
import com.project.chat.presentation.voice_message
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject

/**
 * A play/pause voice-message player rendered inside a chat bubble. Reads playback position straight from
 * the shared [AudioPlayer] singleton so high-frequency updates don't churn ChatDetailState; control flows
 * up via [onPlay]/[onPause].
 */
@Composable
fun VoiceMessagePlayer(
    attachment: MessageAttachmentUi,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioPlayer = koinInject<AudioPlayer>()
    val playingId by audioPlayer.playingId.collectAsStateWithLifecycle()
    val isPlaying by audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val positionMs by audioPlayer.positionMs.collectAsStateWithLifecycle()
    val durationMs by audioPlayer.durationMs.collectAsStateWithLifecycle()

    val isThis = playingId == attachment.url
    val isThisPlaying = isThis && isPlaying

    val totalSeconds = attachment.durationInSeconds ?: 0
    val progress = if (isThis && durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val label = if (isThis && positionMs > 0L) {
        formatDuration((positionMs / 1000).toInt())
    } else {
        formatDuration(totalSeconds)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { if (isThisPlaying) onPause() else onPlay() },
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = vectorResource(
                    if (isThisPlaying) Res.drawable.pause_icon else Res.drawable.play_icon,
                ),
                contentDescription = stringResource(Res.string.voice_message),
                tint = MaterialTheme.colorScheme.extended.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            color = MaterialTheme.colorScheme.extended.accentGreen,
            trackColor = MaterialTheme.colorScheme.extended.surfaceOutline,
            modifier = Modifier
                .width(140.dp)
                .clip(CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.extended.textSecondary,
        )
    }
}
