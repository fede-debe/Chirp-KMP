package com.project.chat.domain.attachment

import kotlinx.coroutines.flow.StateFlow

/**
 * Records a single voice clip from the microphone. Headless (no UI), mirrors [ImageCompressor].
 * Only one recording is active at a time. The caller is responsible for having obtained mic permission.
 */
interface AudioRecorder {
    /**
     * A rolling list of normalized (0f..1f) microphone amplitudes captured while actively recording,
     * for the live waveform. Frozen while paused, cleared on stop/cancel.
     */
    val amplitudes: StateFlow<List<Float>>

    /** Begins capture. Returns false if the recorder could not start. */
    suspend fun start(): Boolean

    /** Temporarily pauses capture (waveform + timer freeze). */
    fun pause()

    /** Resumes capture after a [pause]. */
    fun resume()

    /** Stops and finalizes the recording, returning the encoded clip, or null on failure. */
    suspend fun stop(): RecordedAudio?

    /** Aborts an in-progress recording and discards any partial output. */
    fun cancel()
}

/**
 * A finished recording, ready to be staged as a [com.project.chat.presentation.models.PendingAttachmentUi].
 *
 * @param mimeType always "audio/mp4" (AAC in an m4a container).
 */
class RecordedAudio(
    val bytes: ByteArray,
    val mimeType: String,
    val durationInSeconds: Int,
    val fileName: String,
)
