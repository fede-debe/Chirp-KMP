package com.project.chat.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table mapping the many-to-many relationship between Chats and Participants.
 *
 * ## Strategy / Decisions
 * Because one chat has multiple participants and one participant can be in multiple chats,
 * neither table can hold the foreign key of the other. This cross-reference table links them
 * using a composite primary key. We apply `onDelete = ForeignKey.CASCADE` for both foreign keys
 * so if a chat or user is deleted, their junction references are automatically purged.
 * * Crucially, an `isActive` boolean is included to handle "soft deletes" for participants leaving a chat.
 *
 * ## How It Works
 * 1. Defines a composite primary key consisting of both `chatId` and `userId`.
 * 2. Establishes two foreign keys mapping back to `ChatEntity` and `ChatParticipantEntity`.
 * 3. If a user leaves a chat, `isActive` is set to false rather than deleting the user row.
 *
 * ## Alternatives / Why Not
 * - **Deleting the participant row when they leave a chat:** If Participant A leaves Chat B,
 * we cannot simply delete Participant A's record from the DB because they might still be
 * active in Chat A.
 * - **Solution:** The `isActive` flag isolates the removal to the *relationship* level, preserving
 * the user's data for other active chats.
 *
 * Technical Details:
 * - Composite Keys: `["chatId", "userId"]`.
 * - Cascade Constraints: `onDelete = ForeignKey.CASCADE` applied to both `ChatEntity` and `ChatParticipantEntity`.
 */

/**
 * Junction table entity for mapping chats to participants.
 * * ## Strategy / Decisions
 * Since `chatId` and `userId` act as foreign keys but aren't strictly primary keys, they
 * are explicitly indexed. This optimizes query speeds when resolving relationships and removes
 * Room compile-time warnings regarding unindexed foreign keys.
 * * Technical Details:
 * - Uses the `indices = [Index(value = ["chatId", "userId"])]` parameter inside the `@Entity` annotation.
 */
@Entity(
    primaryKeys = ["chatId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChatParticipantEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["userId"]),
    ],
)
data class ChatParticipantCrossRef(
    val chatId: String,
    val userId: String,
    val isActive: Boolean,
)
