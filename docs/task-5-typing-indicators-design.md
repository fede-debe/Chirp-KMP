# Task 5 — Typing Indicators (design)

Companion to `docs/task-5-and-6-handoff.md`. Client = `feature/chat` (Compose Multiplatform, mobile-first).
Verified backend contract from `fede-debe/Chirp` (`chat/.../api/dto/ws/`, `ChatWebSocketHandler.kt`).

## Confirmed backend contract

Type names are written from the **server's** perspective, so they invert for the client:

| Server type | Client role | Payload (`payload` JSON string) |
|---|---|---|
| `IncomingWebSocketMessageType.TYPING_STARTED` | **client sends** | `TypingEventDto { chatId }` |
| `IncomingWebSocketMessageType.TYPING_STOPPED` | **client sends** | `TypingEventDto { chatId }` |
| `OutgoingWebSocketMessageType.TYPING_INDICATOR` | **client receives** | `TypingIndicatorDto { chatId, userId, username, isTyping }` |

Server behaviour we rely on (`ChatWebSocketHandler.handleTypingEvent`):
- Broadcasts `TYPING_INDICATOR` to **other** participants only — never echoes the sender.
- The indicator payload **already carries `username`** → no participant→username lookup on the client.
- **3-second auto-stop:** each `TYPING_STARTED` (re)schedules a server-side timer that broadcasts
  `isTyping=false` after 3s. Disconnect also broadcasts `isTyping=false`.
  → While typing continues, the client must re-send `TYPING_STARTED` within each 3s window (heartbeat).

## Decisions

- **Typing is ephemeral presence, not chat content** → it does NOT go through Room. `typingUsers` is a pure
  in-memory `shareIn` flow. (Deliberate deviation from the websocket skill's "write to DB first" rule, which
  governs durable messages.)
- **UI placement:** a thin animated row directly **above the composer** (`MessageBox`), on both the mobile
  and wide-screen layouts.
- **Out of scope:** the backend's per-user `typingIndicatorsEnabled` privacy toggle (no settings screen in
  this task — client always sends).

## Chat-list extension (added after detail was tested)

Show typing on the chat **list** too, without opening a chat. Free on the backend: the server pushes
`TYPING_INDICATOR` for every chat the user belongs to, so the same `typingUsers` stream feeds the list.

- `ChatListState += typingUsersByChat: Map<String, List<String>>`.
- `ChatListViewModel`: inject `ChatConnectionClient` (auto-wired via `viewModelOf`); `observeTypingUsers()`
  folds the stream into a `chatId → (userId → username)` map → `chatId → List<username>`, so a chat drops off
  only once all its typists stop and the list can name them. `scan` seeds an empty map so the list render is
  never delayed.
- `ChatListScreen` passes `typingUsernames = state.typingUsersByChat[chatUi.id].orEmpty()` to each `ChatListItemUi`.
- UI (WhatsApp style): while typing, the last-message preview is **replaced** by the named label
  ("Alice is typing…" / "Alice and Bob…" / "Alice and N others…") in `colorScheme.primary`, **crossfaded**
  with `AnimatedContent` keyed on `isTyping`.
- The label is built by a shared `typingUsersLabel(usernames)` composable in `presentation/components/`,
  reused by both the chat-detail `TypingIndicatorRow` and the list, so the two never drift.
- Same shared `incomingMessages` flow as detail → still one socket.

## Send strategy (driven by `messageTextFieldState` changes)

- First non-blank keystroke → `sendTypingStarted(chatId)` + start a ~2s **heartbeat** that re-sends
  `TYPING_STARTED` (keeps the server's 3s timer alive while typing continues).
- ~3s idle → `sendTypingStopped(chatId)` (+ cancel heartbeat).
- On message send, chat switch, and leave-chat → `sendTypingStopped(chatId)` immediately + cancel jobs.
- Jobs cancelled in `onCleared`.

## Change set (mirrors the existing `NEW_MESSAGE` path)

**Data layer**
- `IncomingWebSocketType` += `TYPING_INDICATOR`.
- `IncomingWebSocketDto` += `TypingIndicatorDto(chatId, userId, username, isTyping, type=TYPING_INDICATOR)`.
- `OutgoingWebSocketType` += `TYPING_STARTED`, `TYPING_STOPPED`.
- `OutgoingWebSocketDto` += `TypingStarted(chatId)`, `TypingStopped(chatId)`.
- `WebSocketChatConnectionClient`:
  - new `typingUsers` flow: parse `TYPING_INDICATOR` → map to domain `TypingUser` → `shareIn(applicationScope)`. No DB.
  - `sendTypingStarted/Stopped`: serialize the concrete DTO via the same two-step `WebSocketMessageDto`
    envelope as `NewMessage`, then `webSocketConnector.sendMessage(...)`.

**Domain**
- `domain/models/TypingUser.kt` = `data class TypingUser(chatId, userId, username, isTyping)`.
- `ChatConnectionClient` += `val typingUsers: Flow<TypingUser>`, `suspend fun sendTypingStarted(chatId)`,
  `suspend fun sendTypingStopped(chatId)`.

**Presentation**
- `ChatDetailState` += `typingUsernames: List<String> = emptyList()`.
- `ChatDetailViewModel`:
  - receive: `chatIdFlow.flatMapLatest { connectionClient.typingUsers.filter { chatId }.scan(map) }`
    → exclude self → `typingUsernames`. Resets on chat switch.
  - send: observe text field per the send strategy above.
- UI: animated typing row above `MessageBox` (both layouts). Text from `typingUsernames`:
  1 → "%1$s is typing…", 2 → "%1$s and %2$s are typing…", 3+ → "%1$s and %2$d others are typing…".
- New string resources in `feature/chat/.../composeResources/values/string.xml`.

## Verify
- Compile `:feature:chat` for Android + iOS. User device-tests across platforms before commit.
