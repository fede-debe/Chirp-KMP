@file:Suppress("ktlint:standard:filename")

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Desktop has no voice playback yet (mobile-only feature). All operations are no-ops. */
class DesktopAudioPlayer : AudioPlayer {
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    override val positionMs: StateFlow<Long> = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = MutableStateFlow(0L)
    override val playingId: StateFlow<String?> = MutableStateFlow(null)
    override suspend fun play(id: String, url: String) = Unit
    override fun pause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun release() = Unit
}
