---
name: websocket
description: Use when sending or receiving a real-time chat event, adding a new websocket message type, or working on the live connection/session lifecycle in the Chirp KMP app — covers the envelope format, incoming/outgoing typed DTOs, connection state, and the DB-as-source-of-truth flow.
---

# WebSocket (Chirp KMP)

## Convention

Real-time chat runs over one websocket connection. The domain exposes only a tech-agnostic contract — `feature/chat/.../domain/chat/ChatConnectionClient.kt`:

```kotlin
interface ChatConnectionClient {
    val chatMessages: Flow<ChatMessage>
    val connectionState: StateFlow<ConnectionState>   // DISCONNECTED, CONNECTING, CONNECTED, ERROR_NETWORK, ERROR_UNKNOWN
}
```

**Wire format = a generic envelope + typed payload.** Every frame is a `WebSocketMessageDto(type: String, payload: String)` where `payload` is itself a JSON string of a specific DTO. Incoming and outgoing DTOs are `@Serializable` sealed hierarchies, each variant carrying a default `type` enum field:

- `IncomingWebSocketDto` (sealed interface) + `IncomingWebSocketType` enum: `NewMessageDto`, `MessageDeletedDto`, `ProfilePictureUpdated`, `ChatParticipantsChangedDto`.
- `OutgoingWebSocketDto` (sealed class) + `OutgoingWebSocketType` enum: currently `NewMessage` only (sealed so typing-indicators etc. extend cleanly).

**Two layers** in `feature/chat/.../data/`:
- `network/KtorWebSocketConnector.kt` — owns the raw `WebSocketSession`. It `combine`s auth + connectivity + foreground state, `flatMapLatest` into a `callbackFlow` session, auto-reconnects via `ConnectionRetryHandler.shouldRetry` + `retryWhen`, maps errors with `ConnectionErrorHandler`, answers `Frame.Ping` with `Pong`, and exposes `messages: Flow<WebSocketMessageDto>` + `sendMessage(String): EmptyResult<DataError.Connection>`.
- `chat/WebSocketChatConnectionClient.kt` — the domain impl. **Incoming:** parse `type` → decode the right subtype → `handleIncomingMessage` writes to Room → re-emit the domain `ChatMessage` *read back from the DB*. The DB is the single source of truth; the socket only mutates it.

**Outgoing** is initiated from `data/message/OfflineFirstMessageRepository.kt`: map domain → `OutgoingWebSocketDto.NewMessage`, optimistically upsert it locally as `SENDING`, serialize (two steps), send, and on failure mark the row `FAILED`. **Message IDs are client-generated** so the client can recognise its own message when the server echoes it back.

## Example

Incoming dispatch (`WebSocketChatConnectionClient`):

```kotlin
override val chatMessages = webSocketConnector.messages
    .mapNotNull { parseIncomingMessage(it) }          // envelope.type → typed DTO
    .onEach { handleIncomingMessage(it) }             // side-effect: write to Room
    .filterIsInstance<IncomingWebSocketDto.NewMessageDto>()
    .mapNotNull { database.chatMessageDao.getMessageById(it.id)?.toDomain() }  // read back from DB
    .shareIn(applicationScope, SharingStarted.WhileSubscribed(5000))

private fun parseIncomingMessage(m: WebSocketMessageDto): IncomingWebSocketDto? = when (m.type) {
    IncomingWebSocketType.NEW_MESSAGE.name       -> json.decodeFromString<IncomingWebSocketDto.NewMessageDto>(m.payload)
    IncomingWebSocketType.MESSAGE_DELETED.name   -> json.decodeFromString<IncomingWebSocketDto.MessageDeletedDto>(m.payload)
    …
    else -> null                                  // unknown type → ignored, never crashes
}
```

Outgoing two-step serialization (`OfflineFirstMessageRepository`):

```kotlin
private fun OutgoingWebSocketDto.NewMessage.toJsonPayload(): String {
    val envelope = WebSocketMessageDto(type = type.name, payload = json.encodeToString(this)) // 1) payload
    return json.encodeToString(envelope)                                                       // 2) envelope
}
```

## What to avoid

- ❌ Leaking "WebSocket" into domain names/types. The contract is `ChatConnectionClient`; `ConnectionState` is a domain enum.
- ❌ Sending a raw payload string without the `WebSocketMessageDto` envelope, or skipping the two-step encode.
- ❌ Emitting socket data straight to the UI. Write to Room first, then read the row back — keep the DB as the single source of truth.
- ❌ Crashing on an unrecognised `type`. `parseIncomingMessage` returns `null` for unknown types (forward-compatibility).
- ❌ Generating message IDs server-side. The client mints the ID so it can confirm the echoed broadcast and reconcile delivery status.
- ❌ Adding a new event without extending both the `…WebSocketType` enum and the sealed `…WebSocketDto`, plus a branch in `parseIncomingMessage`/`handleIncomingMessage`.
