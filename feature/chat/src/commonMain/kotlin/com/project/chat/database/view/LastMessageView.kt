package com.project.chat.database.view

import androidx.room.DatabaseView

/**
 * Represents a Room Database View that automatically projects the latest message for every chat.
 * This eliminates the need for manual state management of the last message when new messages are inserted.
 *
 * ## Strategy / Decisions
 * Utilizes Room's `@DatabaseView` to offload the responsibility of determining the most recent message to SQLite.
 * This guarantees data consistency and prevents the tedious overhead of running dual updates (inserting a
 * message AND updating a chat entity) for every single incoming or outgoing message.
 *
 * ## How It Works
 * 1. Defines a data class mapping to the essential message properties (ID, Chat ID, Sender ID, Content, Timestamp).
 * 2. Executes a nested SQL query to perform a self-join on the `chat_message_entity` table.
 * 3. The inner query (`m2`) extracts the maximum timestamp (`max_timestamp`) grouped by `chatId`.
 * 4. The outer query (`m1`) matches these maximum timestamps and chat IDs to the actual message records
 * to retrieve the complete message payload.
 * 5. Room automatically keeps the result of this query updated in memory whenever the underlying chat messages change.
 *
 * ## Alternatives / Why Not
 * The alternative was manually updating a `lastMessage` column in the `ChatEntity` every time a message is
 * sent or received. This was rejected because it is highly tedious, error-prone, and requires multiple
 * manual table operations per message.
 *
 * Technical Details:
 * - Requires a self-join with aliases `m1` and `m2`.
 * - A `LIMIT 1` clause was initially considered but explicitly rejected because the outer query must
 * return the latest message for *multiple* chats across the database, relying purely on the `GROUP BY chatId`.
 */
@DatabaseView(
    viewName = "last_message_view_per_chat",
    value = """
        SELECT m1.*, p.username AS senderUsername
        FROM chatmessageentity m1
        JOIN (
            SELECT chatId, MAX(timestamp) AS max_timestamp
            FROM chatmessageentity
            GROUP BY chatId
        ) m2 ON m1.chatId = m2.chatId AND m1.timestamp = m2.max_timestamp
        LEFT JOIN chatparticipantentity p ON m1.senderId = p.userId
    """,
)
data class LastMessageView(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: String,
    val senderUsername: String?,
)
