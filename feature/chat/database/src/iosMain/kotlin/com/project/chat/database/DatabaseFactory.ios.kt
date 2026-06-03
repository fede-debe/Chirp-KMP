@file:OptIn(ExperimentalForeignApi::class)

package com.project.chat.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific implementation of the database factory.
 * * ## How It Works
 * 1. Uses `NSFileManager.defaultManager` to retrieve the `NSDocumentDirectory` URL.
 * 2. Parses the absolute path string from the returned URL.
 * 3. Appends the database name (`chirp.db`) to the document directory path.
 * 4. Returns `Room.databaseBuilder()` pointing to this constructed file path.
 * * Technical Details:
 * - Opts into `ExperimentalForeignApi` to interface with the iOS file system.
 */
actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<ChirpChatDatabase> {
        val dbFile = documentDirectory() + "/${ChirpChatDatabase.DB_NAME}"

        return Room.databaseBuilder(dbFile)
    }

    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )

        return requireNotNull(documentDirectory?.path)
    }
}
