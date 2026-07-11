package com.project.chat.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database table representing a specific chat room or conversation thread.
 *
 * ## Strategy / Decisions
 * Serves as the central parent entity for a specific chat. It strictly holds data relevant
 * to the chat itself (like last activity) and relies on relationships (Foreign Keys and
 * Junctions) to map out messages and participants.
 *
 * ## How It Works
 * 1. Marked with `@Entity` to create the SQLite table.
 * 2. Uses `chatId` as the unique identifier.
 * 3. Tracks metadata like `lastMessageId` and `lastActivityAt` to help sort the chat list.
 *
 * ## Alternatives / Why Not
 * We do not store participant IDs directly in this table because a chat has multiple
 * participants, and a participant can be in multiple chats. Storing an array or list of IDs
 * directly violates SQL schema structures, necessitating a junction table instead.
 *
 * Technical Details:
 * - Key Constraint: `chatId` is the Primary Key.
 */
@Entity
data class ChatEntity(
    @PrimaryKey
    val chatId: String,
    val lastActivityAt: Long,
    // User id of the chat's creator (the only "admin"). Drives member-management permissions.
    val creatorId: String = "",
)
