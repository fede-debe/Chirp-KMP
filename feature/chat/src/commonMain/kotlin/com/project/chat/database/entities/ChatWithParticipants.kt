package com.project.chat.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.project.chat.database.view.LastMessageView

/**
 * Room relationship class that bundles a Chat with a list of its Participants.
 *
 * ## Strategy / Decisions
 * Uses Room's helper functionality to automatically perform SQL joins under the hood.
 * This allows us to query the database once and receive a fully populated object containing
 * both the chat details and the users inside it, abstracting away the complex join logic.
 *
 * ## How It Works
 * 1. Uses `@Embedded` to hold the parent `ChatEntity`.
 * 2. Uses `@Relation` to fetch the list of `ChatParticipantEntity` objects.
 * 3. Associates them via the `ChatParticipantCrossRef` table using a `Junction`.
 *
 * ## Alternatives / Why Not
 * Writing raw `INNER JOIN` queries manually for many-to-many relationships is error-prone
 * and results in a lot of boilerplate cursor-mapping. Room's `@Junction` cleanly solves this.
 *
 * Technical Details:
 * - Mapping: `@Relation` uses `chatId` as the parent column and `userId` as the entity column.
 * - Junction: Relies on `ChatParticipantCrossRef::class`.
 */

data class ChatWithParticipants(
    @Embedded
    val chat: ChatEntity,
    @Relation(
        parentColumn = "chatId",
        entityColumn = "userId",
        associateBy = Junction(ChatParticipantCrossRef::class),
    )
    val participants: List<ChatParticipantEntity>,
    /**
     * A relation class representing a chat along with its participants and its dynamically resolved most recent message.
     *
     * ## Strategy / Decisions
     * Embeds the `LastMessageView` into the chat fetching logic using Room's `@Relation`. This allows the application
     * to immediately access the latest message when querying chats, abstracting away the complex underlying SQL joins
     * from the DAO and repository layers.
     *
     * ## How It Works
     * 1. Links the parent `ChatEntity` to the `LastMessageView` using `chatId` for both `parentColumn` and `entityColumn`.
     * 2. The `lastMessage` field is marked as nullable (`?`) to gracefully accommodate newly created chats that do not
     * yet have any messages.
     * 3. The legacy `lastMessage` property is completely removed from the base `ChatEntity` since this relation
     * projects the data directly from the view.
     *
     * ## Alternatives / Why Not
     * Keeping the `lastMessage` directly inside the `ChatEntity` was rejected as it creates redundant data that must
     * be manually synchronized, breaking a single source of truth.
     *
     * Technical Details:
     * - Uses `@Relation` with `entity = LastMessageView::class` to map the specific Room View.
     * - When fetched via a DAO method (e.g., `getChatsWithParticipants`), Room returns a list where each
     * element automatically resolves and assigns its respective `lastMessage`.
     */
    @Relation(
        parentColumn = "chatId",
        entityColumn = "chatId",
        entity = LastMessageView::class,
    )
    val lastMessage: LastMessageView?,
)

/**
 * Master aggregate Room relationship class representing a complete Chat view.
 *
 * ## Strategy / Decisions
 * Replicates the `ChatInfo` domain model strictly at the database layer. This class exists
 * to allow the repository to execute a single query that fetches the chat, all of its participants,
 * and every message (along with the sender of each message) deeply nested.
 *
 * ## How It Works
 * 1. `@Embedded` holds the root `ChatEntity`.
 * 2. Uses `@Relation` and `@Junction` to fetch `participants`.
 * 3. Uses a separate `@Relation` mapping `chatId` to `chatId` to fetch `messagesWithSenders`.
 * 4. Room automatically handles fetching the nested `MessageWithSender` objects.
 *
 * ## Alternatives / Why Not
 * We could query these elements sequentially (fetch chat, then fetch participants, then fetch messages),
 * but this would require multiple database transactions and manual threading logic. Using this aggregate
 * object allows Room to optimize the internal queries efficiently.
 *
 * Technical Details:
 * - Deep mapping: Relies on `ChatMessageEntity::class` as the underlying entity to populate
 * the list of `MessageWithSender` objects.
 */
data class ChatInfoEntity(
    @Embedded
    val chat: ChatEntity,
    @Relation(
        parentColumn = "chatId",
        entityColumn = "userId",
        associateBy = Junction(ChatParticipantCrossRef::class),
    )
    val participants: List<ChatParticipantEntity>,
    @Relation(
        parentColumn = "chatId",
        entityColumn = "chatId",
        entity = ChatMessageEntity::class,
    )
    val messagesWithSenders: List<MessageWithSender>,
)
