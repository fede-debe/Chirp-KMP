package com.project.chat.database

import androidx.room.RoomDatabase

/**
 * Factory contract to instantiate the Room database across different platforms.
 * * ## Strategy / Decisions
 * Because databases save structured data to the file system, and each platform (Android/iOS)
 * manages its file system differently, this is defined as an `expect` class. This allows
 * platform-specific implementations to handle the precise paths required.
 * * ## How It Works
 * Exposes a `create()` function that returns a `RoomDatabase.Builder` wrapping the
 * `ChirpChatDatabase` type.
 */
expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<ChirpChatDatabase>
}
