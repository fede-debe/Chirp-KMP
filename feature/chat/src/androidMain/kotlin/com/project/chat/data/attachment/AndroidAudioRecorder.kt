package com.project.chat.data.attachment

import android.content.Context
import android.media.MediaRecorder
import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.RecordedAudio
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
import java.io.File
import kotlin.math.sqrt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Records AAC audio into an m4a file in [Context.getCacheDir] via [MediaRecorder]. Uses the no-arg
 * constructor (deprecated on API 31+ but valid down to our minSdk 26) so we don't branch on version.
 * While recording, [getMaxAmplitude] is polled to drive the live waveform.
 */
@OptIn(ExperimentalUuidApi::class)
class AndroidAudioRecorder(
    private val context: Context,
) : AudioRecorder {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L
    private var amplitudeJob: Job? = null

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    override val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    @Suppress("DEPRECATION")
    override suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File.createTempFile("voice_${System.currentTimeMillis()}", ".m4a", context.cacheDir)
            val mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            startedAtMs = System.currentTimeMillis()
            _amplitudes.value = emptyList()
            startAmplitudePolling()
            true
        } catch (e: Exception) {
            releaseQuietly()
            false
        }
    }

    override fun pause() {
        try {
            recorder?.pause()
        } catch (e: Exception) {
            // ignore
        }
        stopAmplitudePolling()
    }

    override fun resume() {
        try {
            recorder?.resume()
            startAmplitudePolling()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun stop(): RecordedAudio? = withContext(Dispatchers.IO) {
        stopAmplitudePolling()
        _amplitudes.value = emptyList()
        val file = outputFile
        val activeRecorder = recorder
        if (file == null || activeRecorder == null) {
            releaseQuietly()
            return@withContext null
        }
        val durationSeconds = ((System.currentTimeMillis() - startedAtMs) / 1000).toInt()
        try {
            activeRecorder.stop()
        } catch (e: Exception) {
            // stop() throws if stopped before any frame was captured (clip too short).
            releaseQuietly()
            file.delete()
            outputFile = null
            return@withContext null
        }
        releaseQuietly()
        val bytes = file.readBytes()
        file.delete()
        outputFile = null
        if (bytes.isEmpty()) return@withContext null
        RecordedAudio(
            bytes = bytes,
            mimeType = "audio/mp4",
            durationInSeconds = durationSeconds.coerceAtLeast(1),
            fileName = "voice_${Uuid.random()}.m4a",
        )
    }

    override fun cancel() {
        stopAmplitudePolling()
        _amplitudes.value = emptyList()
        releaseQuietly()
        outputFile?.delete()
        outputFile = null
    }

    private fun startAmplitudePolling() {
        stopAmplitudePolling()
        amplitudeJob = scope.launch {
            while (true) {
                val raw = try {
                    recorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                val normalized = sqrt((raw / MAX_AMPLITUDE).coerceIn(0f, 1f))
                _amplitudes.value = (_amplitudes.value + normalized).takeLast(MAX_SAMPLES)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = null
    }

    private fun releaseQuietly() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            // ignore
        }
        recorder = null
    }

    private companion object {
        const val MAX_SAMPLES = 64
        const val POLL_INTERVAL_MS = 90L
        const val MAX_AMPLITUDE = 32_767f
    }
}
