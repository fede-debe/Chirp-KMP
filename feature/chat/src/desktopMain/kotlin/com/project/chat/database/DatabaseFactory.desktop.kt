package com.project.chat.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.project.core.data.util.appDataDirectory
import java.io.File

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<ChirpChatDatabase> {
        val directory = appDataDirectory

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val dbFile = File(directory, ChirpChatDatabase.DB_NAME)
        return Room.databaseBuilder<ChirpChatDatabase>(dbFile.absolutePath)
            .fallbackToDestructiveMigration(true)
    }
}
