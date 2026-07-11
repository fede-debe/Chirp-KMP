package com.project.chat.domain.attachment

import kotlinx.coroutines.flow.StateFlow

/**
 * Plays one voice note at a time, streaming from its public URL. A single Koin singleton is shared by
 * the ViewModel (control) and the VoiceMessagePlayer composables (observation), so starting a new id
 * automatically stops the previous one. High-frequency [positionMs] lives here, not in UI state.
 */
interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>

    /** The id of the clip currently loaded (playing or paused), or null when nothing is loaded. */
    val playingId: StateFlow<String?>

    /** Loads + plays [url] under [id]. If [id] is already loaded, resumes; otherwise switches source. */
    suspend fun play(id: String, url: String)
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()
}
