package com.project.chat.domain.chat

import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ConnectionState
import com.project.chat.domain.models.TypingUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining the real-time client operations for chat functionality, including listening for messages and connection states, and transmitting new messages.
 *
 * ## Strategy / Decisions
 * The interface was specifically named `ChatConnectionClient` (instead of `ChatWebsocketService` or similar) to abstract away the underlying technology. Implementation details like "WebSockets" should not leak into the domain layer, ensuring the real-time communication strategy remains decoupled and can be swapped in the future without domain-level refactoring.
 *
 * ## How It Works
 * 1. Exposes `chatMessages`, a `Flow<ChatMessage>` that the presentation layer observes to react to newly arrived messages in real-time.
 * 2. Exposes `connectionState`, a `StateFlow<ConnectionState>` representing the current health of the connection.
 * 3. Exposes `typingUsers`, a `Flow<TypingUser>` of ephemeral typing-presence events from other participants
 *    (never persisted; the server already supplies the username and excludes the local user).
 * 4. Provides suspending `sendTypingStarted`/`sendTypingStopped` functions to broadcast the local user's typing.
 */
interface ChatConnectionClient {
    val chatMessages: Flow<ChatMessage>
    val connectionState: StateFlow<ConnectionState>
    val typingUsers: Flow<TypingUser>

    suspend fun sendTypingStarted(chatId: String)
    suspend fun sendTypingStopped(chatId: String)
}
