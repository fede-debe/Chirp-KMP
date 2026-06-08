package com.project.chat.database

import androidx.room.RoomDatabaseConstructor

/**
 * KMP-specific constructor required by Room to initialize the database.
 * * ## Strategy / Decisions
 * Room requires this `expect object` to exist in Kotlin Multiplatform projects to bridge
 * the database instantiation. The actual implementation is generated automatically by the
 * Room Gradle plugin.
 * * Technical Details:
 * - Implements `RoomDatabaseConstructor<ChirpChatDatabase>`.
 * - Overrides the `initialize()` function.
 * - Uses the `@Suppress("NO_ACTUAL_FOR_EXPECT")` annotation to silence compiler warnings,
 * as the `actual` implementation is generated at compile time via KSP.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ChirpChatDatabaseConstructor : RoomDatabaseConstructor<ChirpChatDatabase> {
    override fun initialize(): ChirpChatDatabase
}
