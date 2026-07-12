# Design: Chat sub-feature flags (KMP)

**Date:** 2026-07-11
**Branch:** `feature/chat-subfeature-flags`
**Status:** Approved

## Purpose

Chat is already gated behind `CHAT_ENABLED` (default off) for the template. Within chat, four
capabilities landed in #98 that a consuming app may or may not want. Make each independently
toggleable so a new project can pick exactly the chat capabilities it needs.

Scope: introduce the four flags and gate the **send/entry side** of each. No changes to receive/display
code, no changes to the features' internal logic.

## Context

- Config mechanism is **BuildKonfig** via the `com.project.convention.buildkonfig` convention plugin
  (single source of truth). `feature/chat` applies it, so it has its own generated
  `com.project.feature.chat.BuildKonfig`.
- `CHAT_ENABLED` (from the prior work) gates the whole chat module's navigation. These sub-flags are
  **nested under it** — only meaningful when `CHAT_ENABLED=true`.
- Every client is the same app, so an off feature is off everywhere; that content type is never
  created and never needs receiving. Hence **send-side-only** gating (decided during brainstorming).

### The four features and their send-side entry points (in `feature/chat/commonMain`)

| Flag | Send/entry surface | (Display, left untouched) |
|---|---|---|
| `CHAT_VOICE_ENABLED` | mic/record button + audio-permission launcher | `VoiceMessagePlayer`, `WaveformView` |
| `CHAT_ATTACHMENTS_ENABLED` | attach button → `AttachmentSourceBottomSheet`, camera/image pickers | `AttachmentComponents`, `ImageViewerOverlay` |
| `CHAT_TYPING_ENABLED` | typing-event emission in `ChatDetailViewModel` | `TypingIndicatorRow`, `TypingLabel` |
| `CHAT_ADMIN_ENABLED` | `ManageChatButtonSection` → manage-chat route | manage-chat screen |

## Decision

**Four boolean flags — `CHAT_VOICE_ENABLED`, `CHAT_TYPING_ENABLED`, `CHAT_ATTACHMENTS_ENABLED`,
`CHAT_ADMIN_ENABLED` — added to the BuildKonfig convention plugin, default `false`.** Surfaced through
a single **`ChatFeatureFlags`** object in `feature/chat/commonMain` (option B), so the UI reads
`ChatFeatureFlags.voice` etc. rather than scattering `BuildKonfig` references across composables. One
source of truth, keeps `BuildKonfig` out of the UI, overridable in previews/tests.

Gating is **send-side only**: hide the entry point / skip the outbound emission. Display code is left
untouched (dormant, never exercised when a flag is off).

## Design

### The flags + the object
- Add the four `FieldSpec.Type.BOOLEAN` fields (default `"false"` when absent, like `CHAT_ENABLED`) to
  `BuildKonfigConventionPlugin`. They land in every buildkonfig module; `feature/chat` reads its own.
- `ChatFeatureFlags` (object, `feature/chat/commonMain`):
  ```
  object ChatFeatureFlags {
      val voice = BuildKonfig.CHAT_VOICE_ENABLED
      val typing = BuildKonfig.CHAT_TYPING_ENABLED
      val attachments = BuildKonfig.CHAT_ATTACHMENTS_ENABLED
      val admin = BuildKonfig.CHAT_ADMIN_ENABLED
  }
  ```

### The four send-side gates
1. **Voice** — the mic/record control in the chat input is shown only `if (ChatFeatureFlags.voice)`;
   its audio-permission launcher is only wired when on.
2. **Attachments** — the attach button that opens `AttachmentSourceBottomSheet` (and the camera/image
   pickers it triggers) is shown only `if (ChatFeatureFlags.attachments)`.
3. **Typing** — `ChatDetailViewModel` emits typing events only `if (ChatFeatureFlags.typing)`; the
   display row stays but never receives events.
4. **Admin** — `ManageChatButtonSection` (the entry to the manage-chat screen) renders only
   `if (ChatFeatureFlags.admin)`, making the screen unreachable when off.

### Chirp stays full-featured
Defaults are `false` (template-friendly). Chirp's `local.properties` sets all four `= true` (alongside
`CHAT_ENABLED=true`), so Chirp behaves exactly as it does today.

## Acceptance

- Compiles with all four flags **off** (default) and **on**.
- With `CHAT_ENABLED=true` and a sub-flag **off**, its entry point is absent: no mic button (voice),
  no attach button (attachments), no typing emitted (typing), no manage-chat button (admin).
- With all on, chat behaves as it does in Chirp today.
- No changes under the features' receive/display code or the chat module's data/domain logic.

## Out of scope

Receive/display gating, per-feature backend/WS changes, the template snapshot, the theme work, and any
rename. `CHAT_ENABLED` itself (already done).
