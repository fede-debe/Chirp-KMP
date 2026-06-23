# Task 4 — Voice Messages (design spec)

**Date:** 2026-06-24
**Module:** `feature/chat` (Compose Multiplatform; mobile now, desktop stub)
**Status:** Approved design — ready for implementation plan

## Summary

Add voice messages to the chat detail screen. A user taps a mic button in the composer
to record (tap again to stop), the finished clip is staged as a removable chip exactly
like a picked image, and tapping **Send** uploads it through the existing
signed-URL → Supabase PUT → websocket pipeline. Received/sent audio attachments render a
play/pause player with a duration + progress bar instead of an image thumbnail.

The image-attachments feature already built the entire attachment "conveyor belt". Voice
rides it with **three new pieces** (a headless recorder, a player, a mic-permission gate)
plus two metadata fields and a mime-type branch in the UI. The backend already supports
voice: the attachment model/DTO carry `durationInSeconds`.

## Goals

- Record a voice clip from the composer (tap-to-start / tap-to-stop, with a live timer and cancel).
- Stage the recording as a `PendingAttachmentUi` of audio type and send it via the existing pipeline.
- Render received/sent audio attachments as a play/pause player (duration + progress).
- Handle microphone permission on Android (runtime) and iOS (Info.plist + system prompt).
- Compile cleanly for Android and iOS; desktop gets no-op stubs.

## Non-goals (YAGNI)

- No waveform visualization — a simple linear progress bar only.
- No hold-to-record / slide-to-cancel gesture (decided: tap to start/stop).
- No audio trimming, speed control, or transcription.
- No desktop recording/playback — stub only.
- No new Gradle dependencies (use platform built-ins, not ExoPlayer/media3).

## Decisions

- **Record interaction:** tap mic to start, tap stop to finish → becomes a chip → tap Send.
  Fits the existing stage-then-send model and is more accessible than hold-to-record.
- **Android audio APIs:** `MediaRecorder` (record → m4a/aac in `cacheDir`) and `MediaPlayer`
  (playback). Both are platform built-ins — no new dependencies.
- **iOS audio APIs:** `AVAudioRecorder` (record) and `AVAudioPlayer` (playback).
- **Output format:** `audio/mp4` (AAC in an m4a container) on both platforms — broadly
  compatible and what `MediaRecorder`/`AVAudioRecorder` produce naturally.
- **Recorder is a headless injected service**, not a `remember*` composable: recording
  presents no UI controller, so it mirrors `ImageCompressor`/`ImageSaver` (interface in
  commonMain, platform impls, Koin binding). Only the Android permission prompt needs the
  Compose/Activity layer, so just that part is a `remember*` launcher like `rememberCameraLauncher`.
- **Single active playback:** only one voice note plays at a time. State holds
  `playingAttachmentId: String?`; high-frequency position/`isPlaying` come from the player's
  own flows collected directly by the player composable, so they don't churn `ChatDetailState`.

## Architecture

### New domain interfaces (`feature/chat/.../domain/attachment/`)

```kotlin
/** Records a single voice clip. Headless; mirrors ImageCompressor. One recording at a time. */
interface AudioRecorder {
    suspend fun start(): Boolean                     // begins capture (mic already permitted); false on failure
    suspend fun stop(): RecordedAudio?               // finalizes; null on failure
    fun cancel()                                      // discards in-progress recording
}

class RecordedAudio(
    val bytes: ByteArray,
    val mimeType: String,        // "audio/mp4"
    val durationInSeconds: Int,
    val fileName: String,        // "voice_<uuid>.m4a"
)

/** Plays a voice note from its public URL. Owned by the ViewModel for single-active playback. */
interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val playingId: StateFlow<String?>     // attachment id currently loaded, or null
    suspend fun play(id: String, url: String)   // loads + plays; switching id stops the previous
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()
}
```

Platform impls under `{android,ios,desktop}Main/.../data/attachment/`:
`AndroidAudioRecorder` / `IosAudioRecorder` / `DesktopAudioRecorder` (stub),
`AndroidAudioPlayer` / `IosAudioPlayer` / `DesktopAudioPlayer` (stub).

### Mic permission gate (`feature/chat/.../presentation/mediapicker/`)

`rememberAudioPermissionLauncher(onResult: (granted: Boolean) -> Unit): AudioPermissionLauncher`
— expect/actual, mirrors `rememberCameraLauncher`.

- **Android:** `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`
  for `Manifest.permission.RECORD_AUDIO`; if already granted, invoke `onResult(true)` immediately.
- **iOS:** `AVAudioSession.sharedInstance().requestRecordPermission { granted -> onResult(granted) }`
  (dispatched back to main).
- **Desktop:** `isAvailable = false`; `request()` reports not granted.

### AttachmentService change

Generalize the upload so audio carries its duration to the returned `MessageAttachment`:

```kotlin
suspend fun uploadAttachment(
    chatId: String,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
    durationInSeconds: Int? = null,
): Result<MessageAttachment, DataError.Remote>
```

Rename `uploadImage` → `uploadAttachment` (default `durationInSeconds = null` keeps image
call sites simple). `KtorAttachmentService` sets `durationInSeconds` on the returned
`MessageAttachment` (the upload-url GET + Supabase PUT are mime-agnostic and unchanged).
`downloadImage` stays as-is (not used for audio playback, which streams from the URL).

## Data flow

### Recording → staging → send

1. User taps mic. The Screen calls `audioPermission.request()`.
2. Permission callback dispatches `OnStartRecording` (granted) or `OnRecordPermissionDenied`.
3. VM `OnStartRecording`: `audioRecorder.start()`, set `recording = RecordingState(elapsedSeconds = 0)`,
   launch a 1-second ticker updating `elapsedSeconds`.
4. User taps stop → `OnStopRecording`: cancel ticker, `audioRecorder.stop()` → `RecordedAudio`.
   Add a `PendingAttachmentUi(status = READY, mimeType = "audio/mp4", durationInSeconds = ...)`.
   Audio is **not** sent through `ImageCompressor`.
5. User taps cancel → `OnCancelRecording`: ticker cancel, `audioRecorder.cancel()`, `recording = null`.
6. User taps Send → existing `sendMessage()` loop calls `attachmentService.uploadAttachment(...)`,
   passing the chip's `durationInSeconds`. Existing FAILED/retry handling applies unchanged.

### Playback

1. Bubble shows `VoiceMessagePlayer(attachment)`. Tapping play dispatches `OnPlayAttachment(attachment)`.
2. VM calls `audioPlayer.play(id = attachment.url, url = attachment.url)`; sets `playingAttachmentId = attachment.url`.
   The attachment's `url` doubles as its identity key, since `MessageAttachmentUi` has no id field today.
3. The composable collects `audioPlayer.isPlaying/positionMs/durationMs/playingId` and renders
   play/pause + a progress bar + `mm:ss`. Only the row whose `url` matches `playingId` shows as playing.
4. Tapping pause dispatches `OnPauseAttachment` → `audioPlayer.pause()`.
5. `audioPlayer.release()` on VM `onCleared()`.

## State / Action / Event changes

**State (`ChatDetailState`)**
- `recording: RecordingState? = null` where `data class RecordingState(val elapsedSeconds: Int)`.
- `playingAttachmentId: String? = null`.

**Actions (`ChatDetailAction`)**
- `OnMicClick` (Screen-level: triggers permission request)
- `OnStartRecording`, `OnStopRecording`, `OnCancelRecording`
- `OnRecordPermissionDenied`
- `OnPlayAttachment(attachment: MessageAttachmentUi)`, `OnPauseAttachment`

**Events (`ChatDetailEvent`)** — reuse `OnError(UiText)` for all device-level failures.

## UI changes

- **`MessageBox`** — add a mic `IconButton` next to the attach button. When `recording != null`,
  replace the text field row with a recording bar: red dot + `mm:ss` timer + cancel (X) + stop button.
  New params: `recording: RecordingState?`, `onMicClick`, `onStopRecording`, `onCancelRecording`.
- **`AttachmentComponents`**
  - `ComposerAttachmentChip`: when mime starts with `audio/`, render an audio chip
    ("🎙 Voice · 0:12" using `mic_icon` + duration) instead of a thumbnail; keep the spinner/remove affordances.
  - `BubbleAttachmentsRow`: branch on mime — `audio/*` ⇒ `VoiceMessagePlayer`, else `AttachmentImageThumbnail`.
- **`VoiceMessagePlayer.kt`** (new) — play/pause button + linear progress + `mm:ss`; collects the
  `AudioPlayer` flows; compares its attachment id against `playingId`.
- **Icons** — add `mic_icon.xml`; add `play_icon.xml`/`pause_icon.xml` if not available in
  material-icons core (core has `PlayArrow`/`Pause`; `Mic` is extended → custom drawable needed).
- **Strings** — `record_voice_message`, `stop_recording`, `cancel_recording`, `voice_message`,
  `mic_permission_denied`, `record_failed`, `playback_failed`.

## Model changes

- `PendingAttachmentUi`: add `durationInSeconds: Int? = null` (update equals/hashCode).
- `MessageAttachmentUi`: add `durationInSeconds: Int?`.
- `presentation/mappers/ChatMessageMappers.kt`: set `durationInSeconds = durationInSeconds` in `toUi()`.

## DI wiring

Bind in each `ChatDataModule.{android,ios,desktop}.kt`:
- `AndroidAudioRecorder(androidContext())` / `IosAudioRecorder()` / `DesktopAudioRecorder()` → `AudioRecorder`
- `AndroidAudioPlayer(androidContext())` / `IosAudioPlayer()` / `DesktopAudioPlayer()` → `AudioPlayer`

Inject `AudioRecorder` + `AudioPlayer` into `ChatDetailViewModel`.

## Permissions

- **Android:** add `<uses-permission android:name="android.permission.RECORD_AUDIO" />` to
  `feature/chat/src/androidMain/AndroidManifest.xml`. Runtime prompt via the permission launcher.
- **iOS:** add `NSMicrophoneUsageDescription` to `iosApp/iosApp/Info.plist` (mirrors the existing
  `NSCameraUsageDescription`). iOS shows its own one-time prompt on first record.

## Error handling

Device-level failures surface as snackbars through the existing `ChatDetailEvent.OnError(UiText)`:
- Mic permission denied → `mic_permission_denied`.
- `audioRecorder.start()`/`stop()` failure → `record_failed`.
- `audioPlayer.play()` failure → `playback_failed`.
- Upload failure → already handled by `sendMessage()` (chip → FAILED + snackbar).

## iOS gotchas to apply preemptively

- Use nullable Obj-C factories, not non-null constructors, where the API can return nil.
- `requestRecordPermission` callback runs off the main thread — hop back to main before touching UI/state.
- The socket only disconnects on real background (`didEnterBackground`), so opening the recorder
  won't drop the connection.

## Verification

- Compile both targets:
  `./gradlew :feature:chat:compileAndroidMain :feature:chat:compileKotlinIosSimulatorArm64`
  (clean = success; ignore the pre-existing `KtorWebSocketConnector` NonCancellable warning).
- Cannot run the iPad locally — device testing (record, send, receive, play, permission prompts,
  Android RECORD_AUDIO grant/deny) is handed to the user.

## Files

**New**
- `domain/attachment/AudioRecorder.kt`, `domain/attachment/AudioPlayer.kt`
- `{android,ios,desktop}Main/.../data/attachment/{Android,Ios,Desktop}AudioRecorder.kt`
- `{android,ios,desktop}Main/.../data/attachment/{Android,Ios,Desktop}AudioPlayer.kt`
- `presentation/mediapicker/rememberAudioPermissionLauncher.kt` (+ 3 actuals)
- `presentation/ui/chatDetail/components/VoiceMessagePlayer.kt`
- `commonMain/composeResources/drawable/mic_icon.xml` (+ play/pause if needed)

**Edited**
- `domain/attachment/AttachmentService.kt`, `data/attachment/KtorAttachmentService.kt`
- `domain/models/MessageAttachment.kt` (already has `durationInSeconds` — set it)
- `presentation/models/PendingAttachmentUi.kt`, `presentation/models/MessageAttachmentUi.kt`
- `presentation/mappers/ChatMessageMappers.kt`
- `presentation/ui/chatDetail/ChatDetail{State,Action,Event}.kt`
- `presentation/ui/chatDetail/ChatDetailViewModel.kt`
- `presentation/ui/chatDetail/ChatDetailScreen.kt`
- `presentation/ui/chatDetail/components/MessageBox.kt`
- `presentation/ui/chatDetail/components/AttachmentComponents.kt`
- `data/di/ChatDataModule.{android,ios,desktop}.kt`
- `androidMain/AndroidManifest.xml`, `iosApp/iosApp/Info.plist`
- `commonMain/composeResources/values/string.xml`
</content>
</invoke>
