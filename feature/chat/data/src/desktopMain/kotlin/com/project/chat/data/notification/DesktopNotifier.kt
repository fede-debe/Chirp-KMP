package com.project.chat.data.notification

import com.project.chat.domain.chat.ChatConnectionClient
import com.project.chat.domain.chat.ChatRepository
import com.project.core.domain.auth.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Observes incoming chat messages and generates notification payloads for the desktop application.
 * Unlike mobile applications that rely on system-handled push notifications, the desktop client must manually calculate when and what to display via the system tray.
 *
 * ## Strategy / Decisions
 * - **Desktop-Specific Handling:** Push notifications natively handle background messages on mobile. On desktop, we must construct a custom pipeline combining the WebSocket chat flow with our local authentication and system state to replicate this behavior.
 * - **Flow Combination:** We combine the `ChatConnectionClient` (new messages) with `SessionStorage` (auth info) to ensure notifications are only processed for actively logged-in sessions and valid users.
 *
 * ## How It Works
 * 1. Listens to the incoming flow of chat messages and combines it with the current user's authorization info.
 * 2. Extracts the `currentUserId` and filters out any messages where `senderId == currentUserId` (we don't notify users of their own messages) by mapping to null and filtering.
 * 3. Uses `distinctUntilChangedBy` on the `messageId` to prevent the flow from accidentally emitting duplicate notifications for the same event.
 * 4. Fetches the chat information from the `ChatRepository` using the message's `chatId` to determine the participants.
 * 5. Constructs the notification title: Filters the local user out of the participant list, sorts the remaining participants alphabetically by username, and joins them into a comma-separated string.
 * 6. Constructs a `NotificationPayload` using the formatted title and a body formatted as "SenderName: MessageContent".
 *
 * ## Alternatives / Why Not
 * - **Explicit Group vs. 1-on-1 Logic:** Initially considered using a conditional `when` check based on participant size (e.g., size >= 3 for groups vs. one-on-one). This was rejected because uniformly filtering out the local user, sorting, and joining the list automatically yields the correct string for both one-on-one and group chats without needing branching logic.
 *
 * ## Technical Details
 * - **Duplicate Prevention:** Relying on `distinctUntilChangedBy(messageId)` provides an essential safety mechanism against double-triggering desktop notifications.
 * - **Dependencies:** Depends on `ChatConnectionClient`, `SessionStorage`, and `ChatRepository`. Must be provided via DI (e.g., `chatDataModule.desktop`).
 */
class DesktopNotifier(
    private val chatConnectionClient: ChatConnectionClient,
    private val sessionStorage: SessionStorage,
    private val chatRepository: ChatRepository,
) {
    data class NotificationPayload(
        val title: String,
        val message: String,
    )

    fun observeNewNotifications(): Flow<NotificationPayload> {
        return combine(
            chatConnectionClient.chatMessages,
            sessionStorage.observeAuthInfo(),
        ) { chatMessage, authInfo ->
            val currentUserId = authInfo?.user?.id
            if (chatMessage.senderId != currentUserId) {
                (chatMessage to currentUserId)
            } else {
                null
            }
        }
            .filterNotNull()
            .distinctUntilChangedBy { (message, _) -> message.id }
            .map { (message, currentUserId) ->
                val chatInfo = chatRepository.getChatInfoById(message.chatId).firstOrNull()

                val senderName = chatInfo?.chat?.participants?.find {
                    it.userId == message.senderId
                }?.username

                val notificationTitle = chatInfo?.chat?.participants?.let { participants ->
                    participants
                        .filter { it.userId != currentUserId }
                        .sortedBy { it.username }
                        .joinToString(", ") { it.username }
                }

                NotificationPayload(
                    title = notificationTitle ?: "Unknown",
                    message = "$senderName: ${message.content}",
                )
            }
    }
}
