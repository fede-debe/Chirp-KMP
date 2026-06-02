package com.project.chat.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Database table for storing individual chat messages sent by participants within a chat.
 *
 * ## Strategy / Decisions
 * Establishes a one-to-many relationship with the Chat table. Since one message always
 * belongs to exactly one chat, the many-side (this entity) holds the `chatId` reference.
 * Furthermore, we explicitly set `onDelete = ForeignKey.CASCADE`. This guarantees that if
 * a chat is deleted from the DB, all associated messages are automatically wiped out,
 * preventing orphaned data without manual deletion logic.
 *
 * ## How It Works
 * 1. Defines an `@Entity` with foreign keys pointing to `ChatEntity`.
 * 2. Stores the message content, timestamps, and delivery status.
 * 3. Links to a specific sender via `senderId` (which maps to a `ChatParticipantEntity`).
 *
 * ## Alternatives / Why Not
 * - **TypeConverters for Enums:** The instructor explicitly rejected using a Room TypeConverter
 * for the `DeliveryStatus` enum. Using a TypeConverter would require linking this database
 * entity directly to the domain layer's enum. If DB requirements shift, it could inadvertently
 * force a change in the domain model, violating clean architecture layering.
 * - **Solution:** Serialize the enum to a simple String upon insertion and transform it back
 * when querying.
 *
 * Technical Details:
 * - Foreign Keys: Links to `ChatEntity` via `chatId`.
 * - Cascade Constraints: `onDelete = ForeignKey.CASCADE`.
 * - Timestamps: Stored as `Long` for easy SQLite compatibility.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: String,
    val deliveryStatusTimestamp: Long = timestamp,
)
