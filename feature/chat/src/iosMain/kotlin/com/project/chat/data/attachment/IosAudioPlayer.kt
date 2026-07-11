@file:OptIn(ExperimentalForeignApi::class)

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.AudioPlayer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.darwin.dispatch_get_main_queue

/** [AudioPlayer] backed by [AVPlayer]; streams the remote URL and observes time every 200ms. */
class IosAudioPlayer : AudioPlayer {

    private var player: AVPlayer? = null
    private var timeObserver: Any? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playingId = MutableStateFlow<String?>(null)
    override val playingId: StateFlow<String?> = _playingId.asStateFlow()

    override suspend fun play(id: String, url: String) {
        if (_playingId.value == id && player != null) {
            player?.play()
            _isPlaying.value = true
            return
        }
        releasePlayer()
        val nsUrl = NSURL.URLWithString(url) ?: return
        val item = AVPlayerItem(uRL = nsUrl)
        val newPlayer = AVPlayer(playerItem = item)
        player = newPlayer
        _playingId.value = id
        _positionMs.value = 0L

        val interval = CMTimeMakeWithSeconds(0.2, 600)
        timeObserver = newPlayer.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = dispatch_get_main_queue(),
        ) { time ->
            val seconds = CMTimeGetSeconds(time)
            if (!seconds.isNaN()) _positionMs.value = (seconds * 1000).toLong()
            val durationTime = newPlayer.currentItem?.duration
            val dur = if (durationTime != null) CMTimeGetSeconds(durationTime) else Double.NaN
            if (!dur.isNaN()) _durationMs.value = (dur * 1000).toLong()
        }
        newPlayer.play()
        _isPlaying.value = true
    }

    override fun pause() {
        player?.pause()
        _isPlaying.value = false
    }

    override fun seekTo(positionMs: Long) {
        player?.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, 600))
        _positionMs.value = positionMs
    }

    override fun release() {
        releasePlayer()
    }

    private fun releasePlayer() {
        timeObserver?.let { observer -> player?.removeTimeObserver(observer) }
        timeObserver = null
        player?.pause()
        player = null
        _isPlaying.value = false
        _playingId.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
    }
}
