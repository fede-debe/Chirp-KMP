package com.project.chat.presentation.components

import androidx.compose.runtime.Composable
import com.project.chat.presentation.Res
import com.project.chat.presentation.typing_many
import com.project.chat.presentation.typing_one
import com.project.chat.presentation.typing_two
import org.jetbrains.compose.resources.stringResource

/**
 * Formats the currently-typing usernames into a localized label, shared by the chat detail and chat list
 * screens so they stay in sync: "Alice is typing…", "Alice and Bob are typing…",
 * "Alice and 2 others are typing…". Returns "" for an empty list.
 */
@Composable
fun typingUsersLabel(usernames: List<String>): String = when (usernames.size) {
    0 -> ""
    1 -> stringResource(Res.string.typing_one, usernames[0])
    2 -> stringResource(Res.string.typing_two, usernames[0], usernames[1])
    else -> stringResource(Res.string.typing_many, usernames[0], usernames.size - 1)
}
