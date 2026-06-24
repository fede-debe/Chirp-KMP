package com.project.chat.presentation.util

/** Formats a whole number of seconds as m:ss (e.g. 5 -> "0:05", 83 -> "1:23"). */
fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val remaining = safe % 60
    return "$minutes:${remaining.toString().padStart(2, '0')}"
}
