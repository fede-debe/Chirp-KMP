# Task 6 — Chat Admin (design)

Companion to `docs/task-5-and-6-handoff.md`. Verified backend contract from `fede-debe/Chirp`
(`ChatController.kt`, `ChatService.kt`, `ChatWebSocketHandler.kt`, `ChatDto.kt`).

## Grounded scope (differs from the handoff's guesses)

The backend has **no role system, no rename, no explicit delete endpoint**. The creator *is* the admin.

| Action | Backend reality |
|---|---|
| Remove a member | ✅ `DELETE /api/chat/{chatId}/participants/{userId}` → `removeParticipantAsAdmin(chatId, adminId, targetUserId)` — **creator-only** (`NotChatAdminException` otherwise), can't target self (`ForbiddenException`) |
| Add a member | ✅ already built (`POST /chat/{chatId}/add`, any participant) |
| Delete chat | ⚠️ no endpoint — but `removeParticipantFromChat` deletes the whole chat when the **creator** leaves (existing `DELETE /chat/{chatId}/leave`) |
| Promote/demote, rename | ❌ not supported |

So Task 6 = **the creator can remove members**, gated, + make removal/deletion real-time.

## The core gap: the client doesn't know the creator

Backend `ChatDto` carries `creator: ChatParticipantDto`; the client `ChatDto` drops it and `ChatEntity` has
no creator column. Thread `creatorId` end-to-end:

| Layer | Change |
|---|---|
| `data/dto/ChatDto` | `+ creator: ChatParticipantDto` |
| `data/mappers/ChatMappers` | `ChatDto.toDomain` → `creatorId = creator.userId`; `Chat.toEntity` writes it; `ChatEntity.toDomain` + `ChatWithParticipants.toDomain` read it |
| `database/entities/ChatEntity` | `+ creatorId: String`; `ChirpChatDatabase` version 3 → 4 (destructive migration already configured) |
| `domain/models/Chat` | `+ creatorId: String` |
| `presentation/models/ChatUi` | `+ creatorId: String` |
| `presentation/mappers/ChatMappers` (`Chat.toUi`) | pass `creatorId` |

## Remove-member data path (mirrors `leaveChat`)

| Layer | Change |
|---|---|
| `domain/chat/ChatService` + `data/chat/KtorChatService` | `+ removeParticipant(chatId, userId): EmptyResult<DataError.Remote>` → `DELETE /chat/$chatId/participants/$userId` |
| `domain/chat/ChatRepository` + `OfflineFirstChatRepository` | `+ removeParticipant(chatId, userId)` → on success remove the participant locally (cross-ref delete) so the member list updates reactively |

## Manage-chat UI + gating

| Layer | Change |
|---|---|
| `ManageChatViewModel` | inject `SessionStorage`; expose `creatorId` (from `getChatInfoById(chatId).chat.creatorId`) + `currentUserId` (`observeAuthInfo`); `removeParticipant` action → repo call + optimistic/error + confirm |
| `ManageChatState` | `+ creatorId`, `+ currentUserId`, `+ removingUserId`, `+ participantToRemove` (confirm), `+ removeError` |
| `ManageChatAction` | `+ OnRemoveParticipantClick(userId)`, `+ OnConfirmRemoveParticipant`, `+ OnDismissRemoveDialog` |
| `ManageChatScreen` | per-participant **remove** icon, shown only when `currentUserId == creatorId` and the row is neither the creator nor self; destructive confirm dialog |

## Real-time (REMOVED_FROM_CHAT / CHAT_DELETED)

Backend already broadcasts these; the client currently ignores them.

| Layer | Change |
|---|---|
| `IncomingWebSocketType` | `+ REMOVED_FROM_CHAT, CHAT_DELETED` |
| `IncomingWebSocketDto` | `+ RemovedFromChatDto(chatId)`, `+ ChatDeletedDto(chatId)` |
| `WebSocketChatConnectionClient` | parse + handle both → `db.chatDao.deleteChatById(chatId)` (cascade cleans up) → chat vanishes from the list; also emit on a new `chatRemovals: Flow<String>` |
| `ChatConnectionClient` | `+ val chatRemovals: Flow<String>` (chatIds removed/deleted under us) |
| `ChatDetailViewModel` | observe `chatRemovals`; if it matches the open chat, clear + send a `ChatDetailEvent.OnChatRemoved` |
| `ChatDetailEvent` + `ChatDetailRoot` | `+ OnChatRemoved` → navigate back |

## "Delete chat" relabel (creator)

Because a creator leaving deletes the chat for everyone, relabel the chat-detail options entry from
"Leave chat" → "Delete chat" when `ChatUi.localParticipant.id == ChatUi.creatorId` (same `leaveChat` call).

## Removal notices (in-app)

So a chat doesn't vanish silently (reads as a bug), the chat list shows a snackbar when the user loses access.
`ChatConnectionClient.chatRemovals` carries a reason (`ChatRemoval(chatId, ChatRemovalReason)`); `ChatListViewModel`
observes it → `ChatListEvent.OnChatRemoved(reason)` → snackbar:
- `REMOVED_BY_ADMIN` → "You were removed from a chat by the admin"
- `CHAT_DELETED` → "A chat was deleted by the admin" (other participants)
- `CHAT_DELETED_BY_ME` → "Chat deleted successfully" (the creator who deleted it)

The deleting admin also receives `CHAT_DELETED` (backend echoes it to every participant). To show *them* a
success confirmation instead of the third-person notice, `chatRemovals` is emitted from `handleIncomingMessage`
(not derived downstream) so it can read the chat's `creatorId` **before** the row is deleted: if the local user
is the creator (or the row was already removed by their own delete) → `CHAT_DELETED_BY_ME`, else `CHAT_DELETED`.

All affected users land on the chat list (removed/deleted users auto-navigate out of the detail), so the list is
the single place the notice is shown. In-app only — background **push** would be server-side (the backend would
need to emit a push on these events).

## Out of scope
Roles/promote/demote, rename — no backend support. **Admin transfer / reassigning the creator** — no backend
endpoint (a creator leaving deletes the chat); would need e.g. `PATCH /chat/{id}/creator` server-side first.
Per-user `typingIndicatorsEnabled` (Task 5 note). Background push notifications (server-side).

## Verify
Compile `:feature:chat` for Android + iOS. User device-tests across platforms before commit.
