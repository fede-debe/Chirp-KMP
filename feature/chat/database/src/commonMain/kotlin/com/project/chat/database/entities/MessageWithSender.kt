package com.project.chat.database.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relationship class bundling a specific Chat Message with its Sender.
 *
 * ## Strategy / Decisions
 * Groups the message payload with the sender's profile details. Since this is a one-to-many
 * relationship (one sender, multiple messages), it does not require a cross-reference junction
 * table. We fetch the sender based on the `senderId` cached on the message.
 *
 * ## How It Works
 * 1. `@Embedded` holds the `ChatMessageEntity`.
 * 2. `@Relation` queries the `ChatParticipantEntity` where `userId` matches the message's `senderId`.
 *
 * ## Alternatives / Why Not
 * - **Embedding a list of messages inside the sender:** The instructor notes we *could* do the
 * inverse (getting a Sender and a list of all their messages). However, in the context of this app,
 * knowing every message a user has ever sent across all chats is never needed by the UI, so it
 * was explicitly rejected.
 *
 * Technical Details:
 * - Constraint: One-to-one mapping in the context of a single message object.
 */
data class MessageWithSender(
    @Embedded
    val message: ChatMessageEntity,
    @Relation(
        parentColumn = "senderId",
        entityColumn = "userId",
    )
    val sender: ChatParticipantEntity,
)
