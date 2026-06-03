package com.project.chat.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android-specific implementation of the database factory.
 * * ## How It Works
 * 1. Takes the Android `Context` (specifically ensuring it uses the `applicationContext`).
 * 2. Retrieves the absolute file path for the database using `context.getDatabasePath(dbName)`.
 * 3. Returns `Room.databaseBuilder()` pointing to this absolute path.
 * 4. Omits the explicit class type in the builder because it is inferred.
 */
actual class DatabaseFactory(
    private val context: Context,
) {
    actual fun create(): RoomDatabase.Builder<ChirpChatDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(ChirpChatDatabase.DB_NAME)

        return Room.databaseBuilder(
            context.applicationContext,
            dbFile.absolutePath,
        )
    }
}
