# Tasks 5 & 6 — handoff (Typing Indicators, Chat Admin)

Companion to `docs/task-4-voice-messages-handoff.md`. Same client (`feature/chat`, Compose Multiplatform, mobile-first) and same **working rules**: one task at a time (user device-tests each before the next), verify by compiling `:feature:chat` for Android + iOS, mobile-now/desktop-stubs, and explain with analogies + tables + a single decision question.

> ⚠️ **Backend contract:** the user's original spec listed exact endpoints/WebSocket message shapes, but that detail is no longer in context. Before coding either task, **ask the user (or check backend docs) for the precise contract** — the names/shapes below are the *expected* pattern, not confirmed strings. The backend (`chirp.adamapp.dev`) is already complete.

---

## Task 5 — Typing Indicators (WebSocket)

**Goal:** show "X is typing…" in a chat while another participant types; broadcast your own typing while you type in the composer.

**Where it plugs in (confirmed client files):**
- WebSocket envelope is `WebSocketMessageDto { type, payload }`; `payload` is an escaped JSON string parsed by typed DTOs.
  - Incoming: `data/dto/websocket/IncomingWebSocketDto.kt` (sealed) + `IncomingWebSocketType` enum. Parsed in `data/chat/WebSocketChatConnectionClient.kt` `parseIncomingMessage()` (switch on `message.type`), using the single `Json { ignoreUnknownKeys = true }` from `data/di/ChatDataModule.kt`.
  - Outgoing: `data/dto/websocket/OutgoingWebSocketDto.kt` + `OutgoingWebSocketType`. Sent via `data/network/KtorWebSocketConnector.kt`.
- `domain/chat/ChatConnectionClient` exposes `chatMessages` + `connectionState`; add a typing stream (e.g. `typingUsers: Flow<...>`).

**Add:**
- A **TYPING incoming** DTO + enum value → parsed → exposed as a flow keyed by chatId/userId (+ isTyping or a TTL).
- A **TYPING outgoing** DTO + enum value → sent when the local user types.
- `ChatDetailViewModel`: observe the typing stream → `ChatDetailState.typingUsernames: List<String>` (resolve userId→username from participants). Send outgoing typing off `messageTextFieldState` changes — **debounce**: emit "started" on first keystroke, "stopped" after ~2–3s idle and on send. Reset on chat switch.
- UI: a "… is typing" row in `ui/chatDetail/` (under the header or above the composer). Possibly surface in the chat list too (`ChatListState`).

**Confirm with user/backend:** exact `type` string(s) (one `TYPING` w/ `isTyping`, or `TYPING_STARTED`/`TYPING_STOPPED`), payload fields (chatId, userId, username?), whether the server broadcasts to other participants only, and any server-side TTL.

**Mirror:** trace `NEW_MESSAGE` and `MESSAGE_DELETED` end-to-end (DTO → enum → parse → handle → state) and copy that shape.

---

## Task 6 — Chat Admin

**Goal:** manage a chat's members/roles (likely: add member, remove member, promote/demote admin, maybe rename/delete chat), gated to the chat creator/admin.

**Where it plugs in (confirmed client files):**
- Manage-members already exists: `presentation/ui/...` `ManageChatViewModel` (registered in `presentation/di/ChatPresentationModule.kt`), reached via `onChatMembersClick` from chat detail.
- Participant data layer: `KtorChatParticipantService` (bound in `data/di/ChatDataModule.kt`), `OfflineFirstChatParticipantRepository` (`domain/...ChatParticipantRepository`).
- `ChatRepository` already has `leaveChat(chatId)` and `fetchChatById`; `ChatUi` carries `creator` + participants.

**Add:**
- Service + repository methods for each admin action (add/remove participant, set role, delete chat) → call the backend admin endpoints.
- `ManageChatViewModel` state/actions for those, with **permission gating** (only creator/admin sees destructive/role actions) — derive from `creator` / role on the chat + current user id (`SessionStorage.observeAuthInfo`).
- UI in the manage-members screen: per-participant actions (remove, promote/demote), an "add member" entry, and chat-level actions if in scope.
- Errors via the existing `Result/DataError → UiText` mapping (`onFailure { eventChannel.send(OnError(error.toUiText())) }`).

**Confirm with user/backend:** the role model (explicit admin role vs. creator-only), the exact endpoints (add/remove participant, change role, delete chat), and which actions each role may perform.

**Mirror:** `KtorChatParticipantService` + `OfflineFirstChatParticipantRepository` for new calls, and the existing `leaveChat` flow in `ChatRepository`/`ChatDetailViewModel` for the optimistic-update + event pattern.
