# Task 4 — Voice Messages (handoff)

Handoff for a fresh session. Tasks 1–3 are done (email-verification UI, local encryption, **image attachments** incl. camera/zoom/save/Live-Photos). Task 4 is **Voice Messages**, on the **mobile** Compose-Multiplatform client (`feature/chat`). Backend (`chirp.adamapp.dev`) is already complete and **already supports voice**: the attachment model/DTO carry `durationInSeconds`.

## Working rules (carry these over — they are strict)
- **One task at a time.** Do NOT start the next task until the user explicitly says so — they test each on-device first. When done, summarize and wait for confirmation.
- **Explanation style (from user memory):** explain with analogies + tables + a single decision question; no jargon.
- **Mobile now, desktop later:** desktop gets a no-op/stub for platform features.
- **Verify by compiling:** `./gradlew :feature:chat:compileAndroidMain :feature:chat:compileKotlinIosSimulatorArm64` (clean = build success; ignore the pre-existing `KtorWebSocketConnector` NonCancellable warning). Cannot run the iPad locally — hand device tests to the user.

## Scope (what to build)
1. **Record**: hold-to-record (or tap-to-toggle) mic button in the composer, with a running timer + cancel.
2. **Stage**: a finished recording becomes a `PendingAttachmentUi` of audio type (bytes + `audio/*` mime + duration), shown as a removable chip — exactly like a picked image. User taps Send.
3. **Upload + send**: reuse the existing signed-URL → Supabase PUT → `OutgoingNewMessage` pipeline.
4. **Playback bubble**: when an attachment's mime starts with `audio/`, render a voice-message player (play/pause + duration + progress) instead of the image thumbnail.
5. **Permissions**: Android `RECORD_AUDIO` (runtime — needs a permission launcher, unlike camera), iOS `NSMicrophoneUsageDescription` in `iosApp/iosApp/Info.plist`.

## Exactly where it plugs into the existing attachment pipeline
The image-attachments feature already built the whole rail; voice rides it with two new platform pieces (record, play) and a mime-type branch in the UI.

**Reuse as-is:**
- `domain/models/MessageAttachment` already has `durationInSeconds: Int?` → set it for voice.
- `data/dto/.../ChatAttachmentDto` + WS `IncomingWebSocketDto.NewMessageDto` already include `durationInSeconds`. `Json { ignoreUnknownKeys = true }` (single instance in `ChatDataModule.kt`) handles extra server fields.
- DB `ChatAttachmentEntity` already persists duration; mappers in `data/mappers/ChatMessageMappers.kt`.
- `OutgoingNewMessage(chatId, messageId, content, attachments)` — unchanged.
- `ChatDetailViewModel.sendMessage()` loops `attachmentService.uploadImage(...)` over ready attachments → sends. The PUT to Supabase is mime-agnostic, so audio uploads the same way.

**Change/add:**
- `domain/attachment/AttachmentService`: generalize `uploadImage(chatId, fileName, mimeType, bytes)` → `uploadAttachment(..., durationInSeconds: Int? = null)` (or add `uploadAudio`). Set `durationInSeconds` on the returned `MessageAttachment`. Impl: `data/attachment/KtorAttachmentService.kt` (the `/messages/attachments/upload-url?chatId=&mimeType=` GET → PUT bytes flow). **Verify the upload-url endpoint accepts an `audio/*` mime** (it should).
- **New expect/actual `AudioRecorder`** in `domain/attachment/` + `{android,ios,desktop}Main` impls, bound in `data/di/ChatDataModule.*.kt`. Android: `MediaRecorder` → m4a/aac in `cacheDir`. iOS: `AVAudioRecorder`. Returns bytes + mime (e.g. `audio/mp4`) + duration. **Mirror `ImageCompressor`** (interface in commonMain, platform impls, DI binding) and the camera-permission approach in `rememberCameraLauncher`.
- **New expect/actual `AudioPlayer`** (Android `MediaPlayer`/ExoPlayer; iOS `AVPlayer`/`AVAudioPlayer`): play/pause/seek + position flow. Desktop = stub.
- `presentation/models/PendingAttachmentUi`: already has bytes/mime/status — add `durationInSeconds` for the composer chip.
- `presentation/models/MessageAttachmentUi`: currently `(url, fileName, mimeType)` — **add `durationInSeconds`** for the playback bubble. Set it in `presentation/mappers/ChatMessageMappers.kt`.
- `ChatDetailViewModel.onAttachmentsPicked`: **skip `ImageCompressor` for audio** (stage audio directly as READY; only compress images).
- UI: composer mic button in `ui/chatDetail/components/MessageBox.kt`; recording overlay; new `VoiceMessagePlayer` composable; branch in `ui/chatDetail/components/AttachmentComponents.kt` `BubbleAttachmentsRow` → audio mime ⇒ player, else `AttachmentImageThumbnail`.
- State/Action/Event: `ui/chatDetail/ChatDetail{State,Action,Event}.kt` (recording state, start/stop/cancel actions, play/pause).

## Patterns to mirror (files)
- expect/actual + DI: `domain/attachment/ImageCompressor.kt`, `{android,ios,desktop}Main/.../data/attachment/*ImageCompressor.kt`, `data/di/ChatDataModule.{android,ios,desktop}.kt`.
- Android runtime permission + launcher: `androidMain/.../mediapicker/rememberCameraLauncher.android.kt` (use `ActivityResultContracts.RequestPermission` for `RECORD_AUDIO`).
- Upload: `data/attachment/KtorAttachmentService.kt`. Strings: `commonMain/composeResources/values/string.xml`. Custom vector icons (no material-icons-extended): `commonMain/composeResources/drawable/*.xml` (e.g. add a `mic_icon.xml`).

## iOS gotchas already learned (apply preemptively)
- Present UIKit controllers from `presentation/mediapicker/TopMostViewController.kt` `topMostViewController()` (app **main** window, not deprecated `keyWindow` / the keyboard window).
- If launching a UIKit controller from inside a `ModalBottomSheet`, **launch after the sheet closes** (state flag + `LaunchedEffect` + one `withFrameNanos`) — see `ChatDetailScreen.kt` `pendingAttachmentSource`.
- Use **nullable factories**, not non-null Obj-C constructors (`UIImage.imageWithData(...)`, not `UIImage(data:)`) — constructors can't be null in Kotlin and NPE on nil.
- Socket now only disconnects on real background (`didEnterBackground`), so opening the mic/recorder won't drop it.

## Remaining after Task 4
Task 5 = Typing Indicators (WebSocket), Task 6 = Chat Admin.
