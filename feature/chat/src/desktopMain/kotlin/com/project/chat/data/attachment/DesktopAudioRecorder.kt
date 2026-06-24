@file:Suppress("ktlint:standard:filename")

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.RecordedAudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Desktop has no voice recording yet (mobile-only feature). All operations are no-ops. */
class DesktopAudioRecorder : AudioRecorder {
    override val amplitudes: StateFlow<List<Float>> = MutableStateFlow(emptyList())
    override suspend fun start(): Boolean = false
    override fun pause() = Unit
    override fun resume() = Unit
    override suspend fun stop(): RecordedAudio? = null
    override fun cancel() = Unit
}
