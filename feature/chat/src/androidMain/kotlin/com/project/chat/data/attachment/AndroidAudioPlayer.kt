package com.project.chat.data.attachment

import android.media.MediaPlayer
import com.project.chat.domain.attachment.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [AudioPlayer] backed by [MediaPlayer]; streams the remote URL and polls position every 200ms. */
class AndroidAudioPlayer : AudioPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var positionJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playingId = MutableStateFlow<String?>(null)
    override val playingId: StateFlow<String?> = _playingId.asStateFlow()

    override suspend fun play(id: String, url: String) {
        // Resume if the same clip is just paused.
        if (_playingId.value == id && player != null) {
            player?.start()
            _isPlaying.value = true
            startPolling()
            return
        }
        releasePlayer()
        _playingId.value = id
        _positionMs.value = 0L
        try {
            player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong()
                    mp.start()
                    _isPlaying.value = true
                    startPolling()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _positionMs.value = 0L
                    stopPolling()
                }
                setOnErrorListener { _, _, _ ->
                    reset()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            reset()
            throw e
        }
    }

    override fun pause() {
        player?.pause()
        _isPlaying.value = false
        stopPolling()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.toInt())
        _positionMs.value = positionMs
    }

    override fun release() {
        releasePlayer()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun startPolling() {
        stopPolling()
        positionJob = scope.launch {
            while (true) {
                player?.let { _positionMs.value = it.currentPosition.toLong() }
                delay(200)
            }
        }
    }

    private fun stopPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun reset() {
        _isPlaying.value = false
        _playingId.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
        stopPolling()
        releasePlayer()
    }

    private fun releasePlayer() {
        stopPolling()
        try {
            player?.release()
        } catch (e: Exception) {
            // ignore
        }
        player = null
    }
}
