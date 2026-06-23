package com.project.chat.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.project.chat.database.dao.ChatDao
import com.project.chat.database.dao.ChatMessageDao
import com.project.chat.database.dao.ChatParticipantDao
import com.project.chat.database.dao.ChatParticipantsCrossRefDao
import com.project.chat.database.entities.ChatEntity
import com.project.chat.database.entities.ChatMessageEntity
import com.project.chat.database.entities.ChatParticipantCrossRef
import com.project.chat.database.entities.ChatParticipantEntity
import com.project.chat.database.view.LastMessageView

/**
 * Primary database instance for Room that wires together the tables, views, and versions.
 * * ## Strategy / Decisions
 * Implemented as an abstract class inheriting from `RoomDatabase`. The version is set to 1,
 * with the understanding that schema versions only need updating once the app is deployed
 * to real users and schema changes are introduced in subsequent updates.
 * * Technical Details:
 * - Entities included: `ChatEntity`, `ParticipantEntity`, `ChatMessageEntity`, `ChatParticipantCrossRef`.
 * - Views included: `LastMessageView`.
 * - Schema export is enabled (`exportSchema`).
 * - Contains abstract value references to all DAOs.
 * - Saves to the file system as `chirp.db`.
 */
@Database(
    entities = [
        ChatEntity::class,
        ChatParticipantEntity::class,
        ChatMessageEntity::class,
        ChatParticipantCrossRef::class,
    ],
    views = [
        LastMessageView::class,
    ],
    version = 3,
)
@ConstructedBy(ChirpChatDatabaseConstructor::class)
abstract class ChirpChatDatabase : RoomDatabase() {
    abstract val chatDao: ChatDao
    abstract val chatParticipantDao: ChatParticipantDao
    abstract val chatMessageDao: ChatMessageDao
    abstract val chatParticipantsCrossRefDao: ChatParticipantsCrossRefDao

    companion object {
        const val DB_NAME = "chirp.db"
    }
}
