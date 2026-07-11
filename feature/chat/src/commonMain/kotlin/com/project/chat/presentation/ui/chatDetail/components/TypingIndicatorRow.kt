package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.project.chat.presentation.components.typingUsersLabel
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended

/**
 * A subtle "… is typing" line for the chat detail screen. Renders nothing for an empty list; callers wrap it
 * in an [androidx.compose.animation.AnimatedVisibility] keyed on `usernames.isNotEmpty()` to fade it in/out.
 */
@Composable
fun TypingIndicatorRow(
    usernames: List<String>,
    modifier: Modifier = Modifier,
) {
    Text(
        text = typingUsersLabel(usernames),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.extended.textSecondary,
    )
}

@Preview
@Composable
private fun TypingIndicatorRowPreview() {
    ChirpTheme {
        TypingIndicatorRow(usernames = listOf("Cinderella"))
    }
}

@Preview
@Composable
private fun TypingIndicatorRowManyPreview() {
    ChirpTheme(darkTheme = true) {
        TypingIndicatorRow(usernames = listOf("Cinderella", "Josh", "Philipp", "John"))
    }
}
