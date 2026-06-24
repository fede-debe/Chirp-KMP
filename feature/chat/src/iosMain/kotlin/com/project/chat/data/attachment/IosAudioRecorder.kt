@file:OptIn(ExperimentalForeignApi::class)

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.RecordedAudio
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

/**
 * Records AAC audio into an m4a file via [AVAudioRecorder]. Duration comes from the recorder's
 * `currentTime` captured immediately before stopping; metering drives the live waveform.
 */
class IosAudioRecorder : AudioRecorder {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorder: AVAudioRecorder? = null
    private var outputUrl: NSURL? = null
    private var lastDurationSeconds: Int = 0
    private var amplitudeJob: Job? = null

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    override val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    override suspend fun start(): Boolean = withContext(Dispatchers.Default) {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, null)
            session.setActive(true, null)

            val cachesDir = NSSearchPathForDirectoriesInDomains(
                NSCachesDirectory,
                NSUserDomainMask,
                true,
            ).first() as String
            val url = NSURL.fileURLWithPath("$cachesDir/voice_${NSUUID().UUIDString()}.m4a")

            val settings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 1,
                // 96 == AVAudioQualityHigh
                AVEncoderAudioQualityKey to 96,
            )
            val newRecorder = AVAudioRecorder(uRL = url, settings = settings, error = null)
            newRecorder.meteringEnabled = true
            val ok = newRecorder.record()
            if (!ok) return@withContext false
            recorder = newRecorder
            outputUrl = url
            _amplitudes.value = emptyList()
            startAmplitudePolling()
            true
        } catch (e: Exception) {
            recorder = null
            outputUrl = null
            false
        }
    }

    override fun pause() {
        recorder?.pause()
        stopAmplitudePolling()
    }

    override fun resume() {
        recorder?.record()
        startAmplitudePolling()
    }

    override suspend fun stop(): RecordedAudio? = withContext(Dispatchers.Default) {
        stopAmplitudePolling()
        _amplitudes.value = emptyList()
        val activeRecorder = recorder ?: return@withContext null
        val url = outputUrl ?: return@withContext null
        lastDurationSeconds = activeRecorder.currentTime.toInt()
        activeRecorder.stop()
        recorder = null
        outputUrl = null
        val data = NSData.dataWithContentsOfURL(url) ?: return@withContext null
        val bytes = data.toByteArray()
        if (bytes.isEmpty()) return@withContext null
        RecordedAudio(
            bytes = bytes,
            mimeType = "audio/mp4",
            durationInSeconds = lastDurationSeconds.coerceAtLeast(1),
            fileName = "voice_${NSUUID().UUIDString()}.m4a",
        )
    }

    override fun cancel() {
        stopAmplitudePolling()
        _amplitudes.value = emptyList()
        recorder?.stop()
        recorder?.deleteRecording()
        recorder = null
        outputUrl = null
    }

    private fun startAmplitudePolling() {
        stopAmplitudePolling()
        amplitudeJob = scope.launch {
            while (true) {
                val activeRecorder = recorder
                val normalized = if (activeRecorder != null) {
                    activeRecorder.updateMeters()
                    // averagePowerForChannel returns dBFS in [-160, 0]; map [DB_FLOOR, 0] onto [0, 1].
                    val power = activeRecorder.averagePowerForChannel(0u)
                    ((power - DB_FLOOR) / -DB_FLOOR).coerceIn(0f, 1f)
                } else {
                    0f
                }
                _amplitudes.value = (_amplitudes.value + normalized).takeLast(MAX_SAMPLES)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = null
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return ByteArray(0)
        val result = ByteArray(size)
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
        return result
    }

    private companion object {
        const val MAX_SAMPLES = 64
        const val POLL_INTERVAL_MS = 90L

        // Treat -50 dBFS as silence; map [-50, 0] dB onto [0, 1].
        const val DB_FLOOR = -50f
    }
}
