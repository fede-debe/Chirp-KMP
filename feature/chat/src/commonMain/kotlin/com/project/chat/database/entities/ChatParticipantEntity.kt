package com.project.chat.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database table representing a chat participant to cache user data locally.
 *
 * ## Strategy / Decisions
 * Caching chat data locally avoids inefficient API calls for resources that rarely change.
 * This module is placed in `feature_chat_database` so other modules can import the pre-built
 * schema without knowing the internal Room implementation details.
 * This specific entity is designed as a standalone table because, although participants belong
 * to chats, the participant's core data (like username and profile picture) is independent.
 *
 * ## How It Works
 * 1. Room maps this data class to a SQLite table via the `@Entity` annotation.
 * 2. It utilizes the server-side user ID as the primary key for unique identification.
 * 3. It stores basic user details (username, profile picture URL) that populate the UI.
 *
 * ## Alternatives / Why Not
 * We do not store lists of chats inside this entity. Storing foreign relationships directly in
 * the parent table would violate normalization rules for many-to-many relationships.
 *
 * Technical Details:
 * - Framework: Room (built on SQLite).
 * - Key Constraint: `userId` is the Primary Key.
 */
@Entity
data class ChatParticipantEntity(
    @PrimaryKey
    val userId: String,
    val username: String,
    val profilePictureUrl: String?,
)
