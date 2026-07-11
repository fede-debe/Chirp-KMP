# Voice Messages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add record → stage → send → play voice messages to the chat detail screen on the mobile Compose-Multiplatform client, riding the existing image-attachment pipeline.

**Architecture:** Voice is "just another attachment". A headless `AudioRecorder` (Koin singleton, mirrors `ImageCompressor`) captures a clip into a `PendingAttachmentUi` of audio type; the existing `sendMessage()` uploads it through the signed-URL → Supabase PUT → websocket pipeline (made mime-agnostic). Received/sent audio renders a `VoiceMessagePlayer` driven by an `AudioPlayer` singleton instead of an image thumbnail. Android permission uses a `remember*` launcher mirroring `rememberCameraLauncher`; iOS uses `AVAudioSession.requestRecordPermission`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, Ktor. Android `MediaRecorder`/`MediaPlayer`; iOS `AVAudioRecorder`/`AVPlayer` (AVFoundation cinterop). **No new Gradle dependencies.**

---

## Testing & verification convention (read first)

This codebase has **zero unit tests and no test source sets** (confirmed: `find feature core -path '*Test*' -name '*.kt'` → 0 results). The established convention — used to ship Tasks 1–3 (image attachments) — is **verify by compiling both targets**, then hand on-device testing to the user. This plan follows that convention: each task's verification gate is the compile commands below, not a TDD red/green cycle.

**Compile gate (run after every task):**
```bash
./gradlew :feature:chat:compileAndroidMain :feature:chat:compileKotlinIosSimulatorArm64
```
Expected: `BUILD SUCCESSFUL`. Ignore the pre-existing `KtorWebSocketConnector` NonCancellable warning. A clean compile of both targets is the pass condition.

**Device testing (user does this, not the agent):** record + send + receive + play a voice note on iPad and Android; Android RECORD_AUDIO grant + deny flows; iOS first-run mic prompt.

**Commits:** one commit per task, conventional-commit style, ending with the `Co-Authored-By` trailer the repo uses. Only commit when the user has asked you to commit; otherwise leave the working tree staged-and-described per task and let the user commit.

**Conventions to honor (project `.claude/skills`):** `architecture` (domain/data/presentation layering, Koin wiring), `state-management` (State/Action/Event/ViewModel, StateFlow down, Channel events), `ui-components` (`Chirp*` components, `MaterialTheme.colorScheme.extended` tokens), `networking` (`safeCall`, DTO conventions), `error-handling` (`Result`/`DataError`→`UiText`, snackbars via events).

---

## File structure

**New files**
- `feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AudioRecorder.kt` — recorder interface + `RecordedAudio`
- `feature/chat/src/androidMain/kotlin/com/project/chat/data/attachment/AndroidAudioRecorder.kt`
- `feature/chat/src/iosMain/kotlin/com/project/chat/data/attachment/IosAudioRecorder.kt`
- `feature/chat/src/desktopMain/kotlin/com/project/chat/data/attachment/DesktopAudioRecorder.kt`
- `feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AudioPlayer.kt` — player interface
- `feature/chat/src/androidMain/kotlin/com/project/chat/data/attachment/AndroidAudioPlayer.kt`
- `feature/chat/src/iosMain/kotlin/com/project/chat/data/attachment/IosAudioPlayer.kt`
- `feature/chat/src/desktopMain/kotlin/com/project/chat/data/attachment/DesktopAudioPlayer.kt`
- `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.kt` — expect + `AudioPermissionLauncher`
- `feature/chat/src/androidMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.android.kt`
- `feature/chat/src/iosMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.ios.kt`
- `feature/chat/src/desktopMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.desktop.kt`
- `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/util/AudioFormat.kt` — `formatDuration(seconds)`
- `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/VoiceMessagePlayer.kt`
- `feature/chat/src/commonMain/composeResources/drawable/mic_icon.xml`, `play_icon.xml`, `pause_icon.xml`, `stop_icon.xml`

**Edited files**
- `domain/attachment/AttachmentService.kt`, `data/attachment/KtorAttachmentService.kt`
- `presentation/models/PendingAttachmentUi.kt`, `presentation/models/MessageAttachmentUi.kt`
- `presentation/mappers/ChatMessageMappers.kt`
- `presentation/ui/chatDetail/ChatDetailState.kt`, `ChatDetailAction.kt`
- `presentation/ui/chatDetail/ChatDetailViewModel.kt`, `ChatDetailScreen.kt`
- `presentation/ui/chatDetail/components/MessageBox.kt`, `AttachmentComponents.kt`, `MessageList.kt`, `MessageListItemUi.kt`, `LocalUserMessage.kt`, `OtherUserMessage.kt`
- `data/di/ChatDataModule.android.kt`, `ChatDataModule.ios.kt`, `ChatDataModule.desktop.kt`
- `androidMain/AndroidManifest.xml`, `iosApp/iosApp/Info.plist`
- `commonMain/composeResources/values/string.xml`

**Type contract (used across tasks — keep these signatures identical everywhere):**
```kotlin
class RecordedAudio(val bytes: ByteArray, val mimeType: String, val durationInSeconds: Int, val fileName: String)

interface AudioRecorder {
    suspend fun start(): Boolean
    suspend fun stop(): RecordedAudio?
    fun cancel()
}

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val playingId: StateFlow<String?>
    suspend fun play(id: String, url: String)
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()
}

class AudioPermissionLauncher(val isAvailable: Boolean, private val onRequest: () -> Unit) { fun request() = onRequest() }
@Composable expect fun rememberAudioPermissionLauncher(onResult: (granted: Boolean) -> Unit): AudioPermissionLauncher
```

**Design note / deviation from spec:** the spec mentioned a `playingAttachmentId` field in `ChatDetailState`. We do **not** add it: `VoiceMessagePlayer` observes `audioPlayer.playingId` directly (the single source of truth), so a duplicate state field would be dead. Control still flows through the VM via `OnPlayAttachment`/`OnPauseAttachment` actions (architecture-compliant). Also, recorded audio is staged directly in `stopRecording()`, so it never passes through `onAttachmentsPicked` — no "skip compression for audio" guard is needed there.

---

## Task 1: Duration fields on UI models + mapper

**Files:**
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/models/PendingAttachmentUi.kt`
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/models/MessageAttachmentUi.kt`
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/mappers/ChatMessageMappers.kt`

`MessageAttachment` (domain) already has `durationInSeconds: Int?` — we only thread it into the two UI models.

- [ ] **Step 1: Add `durationInSeconds` to `PendingAttachmentUi`** (and its equals/hashCode).

Add the field after `status`:
```kotlin
data class PendingAttachmentUi(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val status: PendingAttachmentStatus,
    val durationInSeconds: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingAttachmentUi) return false
        return id == other.id &&
            fileName == other.fileName &&
            mimeType == other.mimeType &&
            status == other.status &&
            durationInSeconds == other.durationInSeconds &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (durationInSeconds ?: 0)
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
```

- [ ] **Step 2: Add `durationInSeconds` to `MessageAttachmentUi`.**
```kotlin
data class MessageAttachmentUi(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val durationInSeconds: Int? = null,
)
```

- [ ] **Step 3: Set it in the presentation mapper.** In `ChatMessageMappers.kt`, update the private `toUi()`:
```kotlin
private fun MessageAttachment.toUi(): MessageAttachmentUi {
    return MessageAttachmentUi(
        url = storageUrl,
        fileName = fileName,
        mimeType = mimeType,
        durationInSeconds = durationInSeconds,
    )
}
```

- [ ] **Step 4: Compile gate.** Run the compile command. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/models/PendingAttachmentUi.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/models/MessageAttachmentUi.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/mappers/ChatMessageMappers.kt
git commit -m "feat(chat): carry attachment duration into UI models"
```

---

## Task 2: Generalize AttachmentService for audio

**Files:**
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AttachmentService.kt`
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/data/attachment/KtorAttachmentService.kt`
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailViewModel.kt:400` (the `sendMessage()` upload call)

`uploadImage` is renamed to `uploadAttachment` with an optional `durationInSeconds`. The only caller is `sendMessage()`. `downloadImage` is unchanged.

- [ ] **Step 1: Update the interface.** Replace the `uploadImage` declaration in `AttachmentService.kt` with:
```kotlin
suspend fun uploadAttachment(
    chatId: String,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
    durationInSeconds: Int? = null,
): Result<MessageAttachment, DataError.Remote>
```
(Update the KDoc wording from "image" to "attachment".)

- [ ] **Step 2: Update the Ktor implementation.** In `KtorAttachmentService.kt`, rename `override suspend fun uploadImage(` → `override suspend fun uploadAttachment(` and add the `durationInSeconds: Int? = null,` parameter after `bytes`. Set it on the returned model:
```kotlin
return Result.Success(
    MessageAttachment(
        storageUrl = uploadUrls.publicUrl,
        mimeType = mimeType,
        fileName = fileName,
        sizeInBytes = bytes.size.toLong(),
        durationInSeconds = durationInSeconds,
    ),
)
```

- [ ] **Step 3: Update the caller.** In `ChatDetailViewModel.sendMessage()`, change the upload call inside the `for (attachment in readyAttachments)` loop:
```kotlin
val result = attachmentService.uploadAttachment(
    chatId = currentChatId,
    fileName = attachment.fileName,
    mimeType = attachment.mimeType,
    bytes = attachment.bytes,
    durationInSeconds = attachment.durationInSeconds,
)
```

- [ ] **Step 4: Compile gate.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AttachmentService.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/data/attachment/KtorAttachmentService.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailViewModel.kt
git commit -m "feat(chat): generalize attachment upload to carry audio duration"
```

---

## Task 3: AudioRecorder (interface + 3 platform impls + DI)

**Files:**
- Create: `feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AudioRecorder.kt`
- Create: `feature/chat/src/androidMain/kotlin/com/project/chat/data/attachment/AndroidAudioRecorder.kt`
- Create: `feature/chat/src/iosMain/kotlin/com/project/chat/data/attachment/IosAudioRecorder.kt`
- Create: `feature/chat/src/desktopMain/kotlin/com/project/chat/data/attachment/DesktopAudioRecorder.kt`
- Modify: `feature/chat/src/androidMain/.../data/di/ChatDataModule.android.kt`, `ChatDataModule.ios.kt`, `ChatDataModule.desktop.kt`

Mirrors `ImageCompressor`: an interface in commonMain, one concrete class per platform, bound in the platform DI module.

- [ ] **Step 1: Create the interface + model.** `AudioRecorder.kt`:
```kotlin
package com.project.chat.domain.attachment

/**
 * Records a single voice clip from the microphone. Headless (no UI), mirrors [ImageCompressor].
 * Only one recording is active at a time. The caller is responsible for having obtained mic permission.
 */
interface AudioRecorder {
    /** Begins capture. Returns false if the recorder could not start. */
    suspend fun start(): Boolean

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
```

- [ ] **Step 2: Android impl.** `AndroidAudioRecorder.kt`:
```kotlin
package com.project.chat.data.attachment

import android.content.Context
import android.media.MediaRecorder
import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.RecordedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Records AAC audio into an m4a file in [Context.getCacheDir] via [MediaRecorder]. Uses the no-arg
 * constructor (deprecated on API 31+ but valid down to our minSdk 26) so we don't branch on version.
 */
@OptIn(ExperimentalUuidApi::class)
class AndroidAudioRecorder(
    private val context: Context,
) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

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
            startedAtMs = Clock.System.now().toEpochMilliseconds()
            true
        } catch (e: Exception) {
            releaseQuietly()
            false
        }
    }

    override suspend fun stop(): RecordedAudio? = withContext(Dispatchers.IO) {
        val file = outputFile
        val activeRecorder = recorder
        if (file == null || activeRecorder == null) {
            releaseQuietly()
            return@withContext null
        }
        val durationSeconds = ((Clock.System.now().toEpochMilliseconds() - startedAtMs) / 1000).toInt()
        try {
            activeRecorder.stop()
        } catch (e: Exception) {
            // stop() throws if stopped before any frame was captured (clip too short).
            releaseQuietly()
            file.delete()
            return@withContext null
        }
        releaseQuietly()
        val bytes = file.readBytes()
        file.delete()
        if (bytes.isEmpty()) return@withContext null
        RecordedAudio(
            bytes = bytes,
            mimeType = "audio/mp4",
            durationInSeconds = durationSeconds.coerceAtLeast(1),
            fileName = "voice_${Uuid.random()}.m4a",
        )
    }

    override fun cancel() {
        releaseQuietly()
        outputFile?.delete()
        outputFile = null
    }

    private fun releaseQuietly() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            // ignore
        }
        recorder = null
    }
}
```

- [ ] **Step 3: iOS impl.** `IosAudioRecorder.kt`:
```kotlin
@file:OptIn(ExperimentalForeignApi::class)

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.RecordedAudio
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
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
import platform.Foundation.NSData
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

/**
 * Records AAC audio into an m4a file via [AVAudioRecorder]. Duration comes from the recorder's
 * `currentTime` captured immediately before stopping.
 */
class IosAudioRecorder : AudioRecorder {

    private var recorder: AVAudioRecorder? = null
    private var outputUrl: NSURL? = null
    private var lastDurationSeconds: Int = 0

    override suspend fun start(): Boolean = withContext(Dispatchers.Default) {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, null)
            session.setActive(true, null)

            val cachesDir = NSSearchPathForDirectoriesInDomains(
                NSCachesDirectory, NSUserDomainMask, true,
            ).first() as String
            val url = NSURL.fileURLWithPath("$cachesDir/voice_${NSUUID().UUIDString()}.m4a")

            val settings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 1,
                AVEncoderAudioQualityKey to 96, // AVAudioQualityHigh
            )
            val newRecorder = AVAudioRecorder(url, settings, null)
            val ok = newRecorder.record()
            if (!ok) return@withContext false
            recorder = newRecorder
            outputUrl = url
            true
        } catch (e: Exception) {
            recorder = null
            outputUrl = null
            false
        }
    }

    override suspend fun stop(): RecordedAudio? = withContext(Dispatchers.Default) {
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
        recorder?.stop()
        recorder?.deleteRecording()
        recorder = null
        outputUrl = null
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
}
```

- [ ] **Step 4: Desktop stub.** `DesktopAudioRecorder.kt`:
```kotlin
@file:Suppress("ktlint:standard:filename")

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.domain.attachment.RecordedAudio

/** Desktop has no voice recording yet (mobile-only feature). All operations are no-ops. */
class DesktopAudioRecorder : AudioRecorder {
    override suspend fun start(): Boolean = false
    override suspend fun stop(): RecordedAudio? = null
    override fun cancel() = Unit
}
```

- [ ] **Step 5: Bind in DI.** Add the imports and bindings.

In `ChatDataModule.android.kt` add import `com.project.chat.data.attachment.AndroidAudioRecorder` and `com.project.chat.domain.attachment.AudioRecorder`, then inside the module:
```kotlin
single { AndroidAudioRecorder(androidContext()) } bind AudioRecorder::class
```
In `ChatDataModule.ios.kt` add imports and:
```kotlin
singleOf(::IosAudioRecorder) bind AudioRecorder::class
```
In `ChatDataModule.desktop.kt` add imports and:
```kotlin
singleOf(::DesktopAudioRecorder) bind AudioRecorder::class
```

- [ ] **Step 6: Compile gate.** Expected: `BUILD SUCCESSFUL`. (If iOS AVFAudio symbol names mismatch, fix the imports — the device build is the final word; this is the first risky native task.)

- [ ] **Step 7: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AudioRecorder.kt \
        feature/chat/src/androidMain/kotlin/com/project/chat/data/attachment/AndroidAudioRecorder.kt \
        feature/chat/src/iosMain/kotlin/com/project/chat/data/attachment/IosAudioRecorder.kt \
        feature/chat/src/desktopMain/kotlin/com/project/chat/data/attachment/DesktopAudioRecorder.kt \
        feature/chat/src/androidMain/kotlin/com/project/chat/data/di/ChatDataModule.android.kt \
        feature/chat/src/iosMain/kotlin/com/project/chat/data/di/ChatDataModule.ios.kt \
        feature/chat/src/desktopMain/kotlin/com/project/chat/data/di/ChatDataModule.desktop.kt
git commit -m "feat(chat): add AudioRecorder with platform implementations"
```

---

## Task 4: AudioPlayer (interface + 3 platform impls + DI)

**Files:**
- Create: `feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AudioPlayer.kt`
- Create: `feature/chat/src/androidMain/kotlin/com/project/chat/data/attachment/AndroidAudioPlayer.kt`
- Create: `feature/chat/src/iosMain/kotlin/com/project/chat/data/attachment/IosAudioPlayer.kt`
- Create: `feature/chat/src/desktopMain/kotlin/com/project/chat/data/attachment/DesktopAudioPlayer.kt`
- Modify: the three `ChatDataModule.*.kt`

- [ ] **Step 1: Interface.** `AudioPlayer.kt`:
```kotlin
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
```

- [ ] **Step 2: Android impl.** `AndroidAudioPlayer.kt`:
```kotlin
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
```

- [ ] **Step 3: iOS impl.** `IosAudioPlayer.kt`:
```kotlin
@file:OptIn(ExperimentalForeignApi::class)

package com.project.chat.data.attachment

import com.project.chat.domain.attachment.AudioPlayer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
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
        val item = AVPlayerItem(nsUrl)
        val newPlayer = AVPlayer(playerItem = item)
        player = newPlayer
        _playingId.value = id
        _positionMs.value = 0L

        val interval = CMTimeMakeWithSeconds(0.2, 600)
        timeObserver = newPlayer.addPeriodicTimeObserverForInterval(interval, dispatch_get_main_queue()) { time ->
            val seconds = CMTimeGetSeconds(time)
            if (!seconds.isNaN()) _positionMs.value = (seconds * 1000).toLong()
            val dur = newPlayer.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: Double.NaN
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
```

- [ ] **Step 4: Desktop stub.** `DesktopAudioPlayer.kt`:
```kotlin
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
```

- [ ] **Step 5: Bind in DI.** Add to each module (with imports):
  - android: `singleOf(::AndroidAudioPlayer) bind AudioPlayer::class`
  - ios: `singleOf(::IosAudioPlayer) bind AudioPlayer::class`
  - desktop: `singleOf(::DesktopAudioPlayer) bind AudioPlayer::class`

- [ ] **Step 6: Compile gate.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/domain/attachment/AudioPlayer.kt \
        feature/chat/src/androidMain/kotlin/com/project/chat/data/attachment/AndroidAudioPlayer.kt \
        feature/chat/src/iosMain/kotlin/com/project/chat/data/attachment/IosAudioPlayer.kt \
        feature/chat/src/desktopMain/kotlin/com/project/chat/data/attachment/DesktopAudioPlayer.kt \
        feature/chat/src/androidMain/kotlin/com/project/chat/data/di/ChatDataModule.android.kt \
        feature/chat/src/iosMain/kotlin/com/project/chat/data/di/ChatDataModule.ios.kt \
        feature/chat/src/desktopMain/kotlin/com/project/chat/data/di/ChatDataModule.desktop.kt
git commit -m "feat(chat): add AudioPlayer with platform implementations"
```

---

## Task 5: Mic permission launcher + manifest/plist entries

**Files:**
- Create: `feature/chat/src/commonMain/.../presentation/mediapicker/rememberAudioPermissionLauncher.kt`
- Create: `.../androidMain/.../rememberAudioPermissionLauncher.android.kt`
- Create: `.../iosMain/.../rememberAudioPermissionLauncher.ios.kt`
- Create: `.../desktopMain/.../rememberAudioPermissionLauncher.desktop.kt`
- Modify: `feature/chat/src/androidMain/AndroidManifest.xml`
- Modify: `iosApp/iosApp/Info.plist`

Mirrors `rememberCameraLauncher` (expect/actual `@Composable` returning a small launcher object).

- [ ] **Step 1: commonMain expect + class.** `rememberAudioPermissionLauncher.kt`:
```kotlin
@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable

/**
 * Requests microphone permission for voice recording. Mirrors [rememberCameraLauncher]: Android shows a
 * runtime prompt (RECORD_AUDIO) via an activity-result launcher; iOS uses AVAudioSession's prompt;
 * desktop reports unavailable. [onResult] is invoked with whether recording may proceed.
 */
@Composable
expect fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher

/** @param isAvailable whether mic recording is supported on this platform (false on desktop). */
class AudioPermissionLauncher(
    val isAvailable: Boolean,
    private val onRequest: () -> Unit,
) {
    fun request() {
        onRequest()
    }
}
```

- [ ] **Step 2: Android actual.** `rememberAudioPermissionLauncher.android.kt`:
```kotlin
@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onResult(granted)
    }

    return remember {
        AudioPermissionLauncher(
            isAvailable = true,
            onRequest = {
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (alreadyGranted) {
                    onResult(true)
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )
    }
}
```

- [ ] **Step 3: iOS actual.** `rememberAudioPermissionLauncher.ios.kt`:
```kotlin
@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioSession

@Composable
actual fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher {
    return remember {
        AudioPermissionLauncher(
            isAvailable = true,
            onRequest = {
                // iOS shows its own one-time prompt; the callback may run off the main thread, but the
                // ViewModel actions it dispatches only touch StateFlow/Channel, which are thread-safe.
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    onResult(granted)
                }
            },
        )
    }
}
```

- [ ] **Step 4: Desktop actual.** `rememberAudioPermissionLauncher.desktop.kt`:
```kotlin
@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chat.presentation.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): AudioPermissionLauncher {
    return remember {
        AudioPermissionLauncher(
            isAvailable = false,
            onRequest = { onResult(false) },
        )
    }
}
```

- [ ] **Step 5: Android manifest.** In `feature/chat/src/androidMain/AndroidManifest.xml`, add inside `<manifest>` next to the other `uses-permission` entries:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

- [ ] **Step 6: iOS Info.plist.** In `iosApp/iosApp/Info.plist`, add next to `NSCameraUsageDescription`:
```xml
<key>NSMicrophoneUsageDescription</key>
<string>Chirp needs access to your microphone to record and send voice messages in chats.</string>
```

- [ ] **Step 7: Compile gate.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.kt \
        feature/chat/src/androidMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.android.kt \
        feature/chat/src/iosMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.ios.kt \
        feature/chat/src/desktopMain/kotlin/com/project/chat/presentation/mediapicker/rememberAudioPermissionLauncher.desktop.kt \
        feature/chat/src/androidMain/AndroidManifest.xml iosApp/iosApp/Info.plist
git commit -m "feat(chat): add microphone permission launcher and platform declarations"
```

---

## Task 6: Icons, strings, and duration formatter

**Files:**
- Create: `feature/chat/src/commonMain/composeResources/drawable/mic_icon.xml`, `play_icon.xml`, `pause_icon.xml`, `stop_icon.xml`
- Create: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/util/AudioFormat.kt`
- Modify: `feature/chat/src/commonMain/composeResources/values/string.xml`

Icons match the existing stroke/outline style (`strokeColor="#FF000000"`, `strokeWidth="2"`, round caps; tinted at the call site).

- [ ] **Step 1: `mic_icon.xml`** (Lucide "mic"):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M12 2a3 3 0 0 0 -3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0 -3 -3z"
        android:strokeWidth="2"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M19 10v2a7 7 0 0 1 -14 0v-2"
        android:strokeWidth="2"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M12 19v3"
        android:strokeWidth="2"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

- [ ] **Step 2: `play_icon.xml`** (Lucide "play"):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M6 3l14 9 -14 9V3z"
        android:strokeWidth="2"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

- [ ] **Step 3: `pause_icon.xml`** (Lucide "pause"):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M6 4h4v16H6z"
        android:strokeWidth="2"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M14 4h4v16h-4z"
        android:strokeWidth="2"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

- [ ] **Step 4: `stop_icon.xml`** (rounded square):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M7 7h10v10H7z"
        android:strokeWidth="2"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

- [ ] **Step 5: Strings.** Add to `string.xml` (near the other attachment strings):
```xml
<string name="record_voice_message">Record voice message</string>
<string name="stop_recording">Stop recording</string>
<string name="cancel_recording">Cancel recording</string>
<string name="voice_message">Voice message</string>
<string name="mic_permission_denied">Microphone permission is needed to record voice messages.</string>
<string name="record_failed">Couldn\'t record audio. Please try again.</string>
<string name="playback_failed">Couldn\'t play this voice message.</string>
```

- [ ] **Step 6: Duration formatter.** `AudioFormat.kt`:
```kotlin
package com.project.chat.presentation.util

/** Formats a whole number of seconds as m:ss (e.g. 5 -> "0:05", 83 -> "1:23"). */
fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val remaining = safe % 60
    return "$minutes:${remaining.toString().padStart(2, '0')}"
}
```

- [ ] **Step 7: Compile gate.** Expected: `BUILD SUCCESSFUL` (compose-resources regenerates `Res.drawable.*` / `Res.string.*` accessors).

- [ ] **Step 8: Commit.**
```bash
git add feature/chat/src/commonMain/composeResources/drawable/mic_icon.xml \
        feature/chat/src/commonMain/composeResources/drawable/play_icon.xml \
        feature/chat/src/commonMain/composeResources/drawable/pause_icon.xml \
        feature/chat/src/commonMain/composeResources/drawable/stop_icon.xml \
        feature/chat/src/commonMain/composeResources/values/string.xml \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/util/AudioFormat.kt
git commit -m "feat(chat): add voice message icons, strings, and duration formatter"
```

---

## Task 7: State + Action additions

**Files:**
- Modify: `feature/chat/src/commonMain/.../ChatDetailState.kt`
- Modify: `feature/chat/src/commonMain/.../ChatDetailAction.kt`

No `ChatDetailEvent` change — failures reuse the existing `OnError(UiText)`. New actions compile with the VM's existing `else -> Unit` branch until Task 8 handles them.

- [ ] **Step 1: State.** In `ChatDetailState.kt`, add a field to `ChatDetailState` and a `RecordingState` class:
```kotlin
val recording: RecordingState? = null,
```
(add it after `pendingAttachmentSource`), and below `BannerState`:
```kotlin
/** Present only while a voice message is being recorded; drives the composer's recording bar. */
data class RecordingState(
    val elapsedSeconds: Int = 0,
)
```

- [ ] **Step 2: Actions.** In `ChatDetailAction.kt`, add inside the sealed interface:
```kotlin
data object OnMicClick : ChatDetailAction
data object OnStartRecording : ChatDetailAction
data object OnStopRecording : ChatDetailAction
data object OnCancelRecording : ChatDetailAction
data object OnRecordPermissionDenied : ChatDetailAction
data class OnPlayAttachment(val attachment: MessageAttachmentUi) : ChatDetailAction
data object OnPauseAttachment : ChatDetailAction
```
(`MessageAttachmentUi` is already imported in this file.)

- [ ] **Step 3: Compile gate.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailState.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailAction.kt
git commit -m "feat(chat): add recording state and voice message actions"
```

---

## Task 8: ViewModel recording + playback logic

**Files:**
- Modify: `feature/chat/src/commonMain/.../ChatDetailViewModel.kt`

Inject `AudioRecorder` + `AudioPlayer`, handle the new actions, run the recording timer, stage the finished clip, and release the player on clear. (`viewModelOf(::ChatDetailViewModel)` auto-resolves the new constructor params — no DI-module change.)

- [ ] **Step 1: Constructor + imports.** Add imports:
```kotlin
import com.project.chat.domain.attachment.AudioPlayer
import com.project.chat.domain.attachment.AudioRecorder
import com.project.chat.presentation.record_failed
import com.project.chat.presentation.mic_permission_denied
import com.project.chat.presentation.playback_failed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.project.core.presentation.util.UiText
```
(Some of these may already be imported — keep one copy.) Add to the constructor parameter list:
```kotlin
private val audioRecorder: AudioRecorder,
private val audioPlayer: AudioPlayer,
```

- [ ] **Step 2: Ticker field.** Add a private field near `currentPaginator`:
```kotlin
private var recordingTickerJob: Job? = null
```

- [ ] **Step 3: Wire actions.** In `onAction`, replace the trailing `else -> Unit` with the new branches followed by `else -> Unit`:
```kotlin
ChatDetailAction.OnStartRecording -> startRecording()
ChatDetailAction.OnStopRecording -> stopRecording()
ChatDetailAction.OnCancelRecording -> cancelRecording()
ChatDetailAction.OnRecordPermissionDenied -> onRecordPermissionDenied()
is ChatDetailAction.OnPlayAttachment -> playAttachment(action.attachment)
ChatDetailAction.OnPauseAttachment -> audioPlayer.pause()
else -> Unit
```
(`OnMicClick` is handled in the Screen, not the VM, so it falls into `else -> Unit`.)

- [ ] **Step 4: Recording functions.** Add these methods (near `onAttachmentsPicked`):
```kotlin
private fun startRecording() {
    if (state.value.recording != null) return
    viewModelScope.launch {
        val started = audioRecorder.start()
        if (!started) {
            eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.record_failed)))
            return@launch
        }
        _state.update { it.copy(recording = RecordingState(elapsedSeconds = 0)) }
        recordingTickerJob?.cancel()
        recordingTickerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _state.update { current ->
                    val active = current.recording ?: return@update current
                    current.copy(recording = active.copy(elapsedSeconds = active.elapsedSeconds + 1))
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun stopRecording() {
    if (state.value.recording == null) return
    recordingTickerJob?.cancel()
    recordingTickerJob = null
    viewModelScope.launch {
        val audio = audioRecorder.stop()
        _state.update { it.copy(recording = null) }
        if (audio == null) {
            eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.record_failed)))
            return@launch
        }
        val remainingSlots = (MAX_ATTACHMENTS - state.value.pendingAttachments.size).coerceAtLeast(0)
        if (remainingSlots == 0) return@launch
        val pending = PendingAttachmentUi(
            id = Uuid.random().toString(),
            fileName = audio.fileName,
            mimeType = audio.mimeType,
            bytes = audio.bytes,
            status = PendingAttachmentStatus.READY,
            durationInSeconds = audio.durationInSeconds,
        )
        _state.update { it.copy(pendingAttachments = it.pendingAttachments + pending) }
    }
}

private fun cancelRecording() {
    recordingTickerJob?.cancel()
    recordingTickerJob = null
    audioRecorder.cancel()
    _state.update { it.copy(recording = null) }
}

private fun onRecordPermissionDenied() {
    viewModelScope.launch {
        eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.mic_permission_denied)))
    }
}

private fun playAttachment(attachment: MessageAttachmentUi) {
    viewModelScope.launch {
        try {
            audioPlayer.play(id = attachment.url, url = attachment.url)
        } catch (e: Exception) {
            eventChannel.send(ChatDetailEvent.OnError(UiText.Resource(Res.string.playback_failed)))
        }
    }
}
```

- [ ] **Step 5: Cleanup.** Add an `onCleared` override (anywhere in the class body):
```kotlin
override fun onCleared() {
    super.onCleared()
    recordingTickerJob?.cancel()
    audioPlayer.release()
}
```

- [ ] **Step 6: Compile gate.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailViewModel.kt
git commit -m "feat(chat): handle recording and playback in ChatDetailViewModel"
```

---

## Task 9: Composer mic button + recording bar

**Files:**
- Modify: `feature/chat/src/commonMain/.../components/MessageBox.kt`
- Modify: `feature/chat/src/commonMain/.../ChatDetailScreen.kt`

The mic button sits next to the attach button. While `recording != null`, the text field is replaced by a recording bar (red dot + timer + cancel + stop). The Screen owns the permission launcher and turns a mic tap into `OnStartRecording` / `OnRecordPermissionDenied`.

- [ ] **Step 1: MessageBox params + imports.** Add imports to `MessageBox.kt`:
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.project.chat.presentation.mic_icon
import com.project.chat.presentation.stop_icon
import com.project.chat.presentation.record_voice_message
import com.project.chat.presentation.stop_recording
import com.project.chat.presentation.cancel_recording
import com.project.chat.presentation.ui.chatDetail.RecordingState
import com.project.chat.presentation.util.formatDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
```
Add parameters to `MessageBox(...)` (after `onRemoveAttachment`):
```kotlin
recording: RecordingState?,
onMicClick: () -> Unit,
onStopRecording: () -> Unit,
onCancelRecording: () -> Unit,
```

- [ ] **Step 2: Recording bar + mic button.** Replace the `ChirpMultiLineTextField(...)` block so it is shown only when not recording, and render the recording bar otherwise. Inside the `Column`, after the `pendingAttachments` row:
```kotlin
if (recording != null) {
    RecordingBar(
        elapsedSeconds = recording.elapsedSeconds,
        onCancel = onCancelRecording,
        onStop = onStopRecording,
        modifier = Modifier.fillMaxWidth(),
    )
} else {
    ChirpMultiLineTextField(
        // ...keep the existing field exactly as-is...
        bottomContent = {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.upload_icon),
                    contentDescription = "Attach images",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.extended.textSecondary,
                )
            }
            IconButton(
                onClick = onMicClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.mic_icon),
                    contentDescription = stringResource(Res.string.record_voice_message),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.extended.textSecondary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // ...keep the existing connection indicator + ChirpButton exactly as-is...
        },
    )
}
```

- [ ] **Step 3: RecordingBar composable.** Add to `MessageBox.kt`:
```kotlin
@Composable
private fun RecordingBar(
    elapsedSeconds: Int,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
        )
        Text(
            text = formatDuration(elapsedSeconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.extended.textPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.cancel_recording),
                tint = MaterialTheme.colorScheme.extended.textSecondary,
            )
        }
        IconButton(onClick = onStop) {
            Icon(
                imageVector = vectorResource(Res.drawable.stop_icon),
                contentDescription = stringResource(Res.string.stop_recording),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
```
(`extended.textPrimary`, `textSecondary`, `textPlaceholder`, and `surfaceLower` are all confirmed tokens in `ExtendedColors`.)

- [ ] **Step 4: Update the MessageBox preview** to pass the new params:
```kotlin
recording = null,
onMicClick = {},
onStopRecording = {},
onCancelRecording = {},
```

- [ ] **Step 5: Screen wiring.** In `ChatDetailScreen.kt` `ChatDetailRoot`, after the `cameraLauncher` block, add:
```kotlin
val audioPermission = rememberAudioPermissionLauncher { granted ->
    if (granted) {
        viewModel.onAction(ChatDetailAction.OnStartRecording)
    } else {
        viewModel.onAction(ChatDetailAction.OnRecordPermissionDenied)
    }
}
```
Add the import:
```kotlin
import com.project.chat.presentation.mediapicker.rememberAudioPermissionLauncher
```
Then pass an `onMicClick` handler into `ChatDetailScreen` via the existing `onAction` channel. In `ChatDetailScreen`'s `onAction` is per-action; the mic tap needs the launcher, which lives in `ChatDetailRoot`. Handle it in the Root's `onAction` lambda — add a branch before `viewModel.onAction(action)`:
```kotlin
onAction = { action ->
    when (action) {
        is ChatDetailAction.OnChatMembersClick -> onChatMembersClick()
        is ChatDetailAction.OnBackClick -> onBack()
        ChatDetailAction.OnMicClick -> audioPermission.request()
        else -> Unit
    }
    viewModel.onAction(action)
},
```

- [ ] **Step 6: Pass MessageBox params at both call sites.** In `ChatDetailScreen`, both `MessageBox(...)` calls (narrow + wide) get:
```kotlin
recording = state.recording,
onMicClick = { onAction(ChatDetailAction.OnMicClick) },
onStopRecording = { onAction(ChatDetailAction.OnStopRecording) },
onCancelRecording = { onAction(ChatDetailAction.OnCancelRecording) },
```

- [ ] **Step 7: Compile gate.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/MessageBox.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailScreen.kt
git commit -m "feat(chat): add composer mic button and recording bar"
```

---

## Task 10: Audio chip + VoiceMessagePlayer bubble

**Files:**
- Create: `feature/chat/src/commonMain/.../components/VoiceMessagePlayer.kt`
- Modify: `feature/chat/src/commonMain/.../components/AttachmentComponents.kt`
- Modify: `MessageList.kt`, `MessageListItemUi.kt`, `LocalUserMessage.kt`, `OtherUserMessage.kt`

The composer chip shows an audio look + duration; the bubble branches on mime to a player. Play/pause callbacks mirror the existing `onAttachmentClick` threading.

- [ ] **Step 1: VoiceMessagePlayer.** Create `VoiceMessagePlayer.kt`:
```kotlin
package com.project.chat.presentation.ui.chatDetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.chat.domain.attachment.AudioPlayer
import com.project.chat.presentation.Res
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.pause_icon
import com.project.chat.presentation.play_icon
import com.project.chat.presentation.util.formatDuration
import com.project.core.designsystem.theme.extended
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import com.project.chat.presentation.voice_message

/**
 * A play/pause voice-message bubble. Reads playback position straight from the shared [AudioPlayer]
 * singleton so high-frequency updates don't churn ChatDetailState; control flows up via [onPlay]/[onPause].
 */
@Composable
fun VoiceMessagePlayer(
    attachment: MessageAttachmentUi,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioPlayer = koinInject<AudioPlayer>()
    val playingId by audioPlayer.playingId.collectAsStateWithLifecycle()
    val isPlaying by audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val positionMs by audioPlayer.positionMs.collectAsStateWithLifecycle()
    val durationMs by audioPlayer.durationMs.collectAsStateWithLifecycle()

    val isThis = playingId == attachment.url
    val isThisPlaying = isThis && isPlaying

    val totalSeconds = attachment.durationInSeconds ?: 0
    val progress = if (isThis && durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val label = if (isThis && positionMs > 0L) {
        formatDuration((positionMs / 1000).toInt())
    } else {
        formatDuration(totalSeconds)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = vectorResource(
                if (isThisPlaying) Res.drawable.pause_icon else Res.drawable.play_icon,
            ),
            contentDescription = stringResource(Res.string.voice_message),
            tint = MaterialTheme.colorScheme.extended.textSecondary,
            modifier = Modifier
                .size(24.dp)
                .clickable { if (isThisPlaying) onPause() else onPlay() },
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.extended.textSecondary,
        )
    }
}
```

- [ ] **Step 2: Audio chip + bubble branch in `AttachmentComponents.kt`.** Add imports:
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import com.project.chat.presentation.mic_icon
import com.project.chat.presentation.util.formatDuration
```
Update `ComposerAttachmentChip` so audio renders an audio look instead of a thumbnail. Replace the `AttachmentImageThumbnail(...)` call inside the chip's `Box` with:
```kotlin
if (item.mimeType.startsWith("audio/")) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.extended.surfaceLower),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = vectorResource(Res.drawable.mic_icon),
                contentDescription = item.fileName,
                tint = MaterialTheme.colorScheme.extended.textPlaceholder,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = formatDuration(item.durationInSeconds ?: 0),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.extended.textPlaceholder,
            )
        }
    }
} else {
    AttachmentImageThumbnail(
        model = if (showThumbnail) item.bytes else null,
        contentDescription = item.fileName,
        modifier = Modifier.matchParentSize(),
    )
}
```
Update `BubbleAttachmentsRow` to take play/pause callbacks and branch on mime:
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BubbleAttachmentsRow(
    attachments: List<MessageAttachmentUi>,
    onAttachmentClick: (MessageAttachmentUi) -> Unit,
    onPlayAttachment: (MessageAttachmentUi) -> Unit,
    onPauseAttachment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        attachments.forEach { attachment ->
            if (attachment.mimeType.startsWith("audio/")) {
                VoiceMessagePlayer(
                    attachment = attachment,
                    onPlay = { onPlayAttachment(attachment) },
                    onPause = onPauseAttachment,
                )
            } else {
                AttachmentImageThumbnail(
                    model = attachment.url,
                    contentDescription = attachment.fileName,
                    modifier = Modifier
                        .size(BUBBLE_THUMBNAIL_SIZE)
                        .clickable { onAttachmentClick(attachment) },
                )
            }
        }
    }
}
```

- [ ] **Step 3: Thread callbacks through the bubble composables.**

`LocalUserMessage.kt` — add params and forward:
```kotlin
onAttachmentClick: (MessageAttachmentUi) -> Unit,
onPlayAttachment: (MessageAttachmentUi) -> Unit,
onPauseAttachment: () -> Unit,
```
and update its `BubbleAttachmentsRow(...)` call:
```kotlin
BubbleAttachmentsRow(
    attachments = message.attachments,
    onAttachmentClick = onAttachmentClick,
    onPlayAttachment = onPlayAttachment,
    onPauseAttachment = onPauseAttachment,
)
```

`OtherUserMessage.kt` — same: add the two new params and forward them into `BubbleAttachmentsRow`.

`MessageListItemUi.kt` — add the two params to its signature and forward to both `LocalUserMessage(...)` and `OtherUserMessage(...)`:
```kotlin
onPlayAttachment: (MessageAttachmentUi) -> Unit,
onPauseAttachment: () -> Unit,
```
(`OtherUserMessage` currently receives only `onAttachmentClick` — add the two new ones there too.)

`MessageList.kt` — add the two params and forward into `MessageListItemUi(...)`.

- [ ] **Step 4: Dispatch from the Screen.** In `ChatDetailScreen.kt`, update the `MessageList(...)` call to add:
```kotlin
onPlayAttachment = { attachment ->
    onAction(ChatDetailAction.OnPlayAttachment(attachment))
},
onPauseAttachment = {
    onAction(ChatDetailAction.OnPauseAttachment)
},
```

- [ ] **Step 5: Fix previews.** Add `onPlayAttachment = {}` / `onPauseAttachment = {}` to any `MessageListItemUi(...)` previews in `MessageListItemUi.kt` (three previews).

- [ ] **Step 6: Compile gate.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit.**
```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/VoiceMessagePlayer.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/AttachmentComponents.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/MessageList.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/MessageListItemUi.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/LocalUserMessage.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/OtherUserMessage.kt \
        feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailScreen.kt
git commit -m "feat(chat): render voice messages with a play/pause bubble"
```

---

## Task 11: Final verification + device handoff

- [ ] **Step 1: Full compile gate.**
```bash
./gradlew :feature:chat:compileAndroidMain :feature:chat:compileKotlinIosSimulatorArm64
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Lint/format if the project uses it.** Check for a ktlint task and run it if present:
```bash
./gradlew :feature:chat:ktlintCheck || echo "no ktlint task; skip"
```

- [ ] **Step 3: Hand off device testing to the user.** Summarize what to test on-device:
  - Android: tap mic → grant RECORD_AUDIO → record → stop → chip shows "🎙 0:0x" → Send → bubble plays.
  - Android: deny RECORD_AUDIO → snackbar appears, no recording.
  - iOS: first mic tap shows the system prompt; allow → record/send/play; the socket stays connected.
  - Both: play one voice note, then another — the first stops (single active playback).
  - Both: cancel (X) during recording discards with no chip.

---

## Self-review

**Spec coverage:**
- Record (tap start/stop + timer + cancel) → Tasks 7–9. ✅
- Stage as `PendingAttachmentUi` audio chip → Tasks 1, 8, 10. ✅
- Upload + send via existing pipeline → Task 2 (+ unchanged `sendMessage`). ✅
- Playback bubble (play/pause + duration + progress) → Tasks 4, 10. ✅
- Permissions (Android runtime, iOS plist) → Task 5. ✅
- DTO/DB/WS duration already present → no task needed (reused). ✅
- `AudioRecorder`/`AudioPlayer` expect-style interface + impls + DI → Tasks 3, 4. ✅
- Skip compression for audio → not needed (audio bypasses `onAttachmentsPicked`); documented. ✅
- Desktop stubs → Tasks 3, 4, 5. ✅

**Placeholder scan:** No "TBD"/"handle errors"/"similar to" — every code step has full content. One verification caveat remains (iOS native symbol names in the AVFoundation/AVFAudio cinterop), flagged inline; the compile gate + device build are the final word. Not a placeholder.

**Type consistency:** `uploadAttachment(... durationInSeconds: Int? = null)` used identically in Tasks 2 & 8 contract. `RecordedAudio(bytes, mimeType, durationInSeconds, fileName)` consistent in Tasks 3 & 8. `AudioPlayer.play(id, url)` consistent in Tasks 4, 8, 10. `BubbleAttachmentsRow(onPlayAttachment, onPauseAttachment)` consistent across Tasks 10's threading. `RecordingState(elapsedSeconds)` consistent Tasks 7, 8, 9. Action names (`OnStartRecording`, `OnStopRecording`, `OnCancelRecording`, `OnRecordPermissionDenied`, `OnMicClick`, `OnPlayAttachment`, `OnPauseAttachment`) consistent across Tasks 7–10.
</content>
