package com.noorpro.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

/** 1.2.0 (114): Noor AI chat history in Room (local only, never uploaded). */
@Entity(tableName = "conversations")
data class NoorAiConversationEntity(
    @androidx.room.PrimaryKey val id: Long,
    val mode: String,
    val title: String,
    val updated: Long,
    val pinned: Boolean = false,
    val customTitle: Boolean = false,
    val created: Long = updated,
)

@Entity(tableName = "messages", primaryKeys = ["convId", "msgId"], indices = [Index("convId")])
data class NoorAiMessageEntity(
    val convId: Long,
    val msgId: Long,
    val seq: Int,
    val text: String,
    val isUser: Boolean,
    val time: Long,
    val feedback: Int = 0,
)

@Dao
abstract class NoorAiHistoryDao {
    @Query("SELECT * FROM conversations ORDER BY pinned DESC, updated DESC LIMIT 300")
    abstract suspend fun conversations(): List<NoorAiConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    abstract suspend fun conversation(id: Long): NoorAiConversationEntity?

    @Query("SELECT * FROM messages WHERE convId = :id ORDER BY seq")
    abstract suspend fun messages(id: Long): List<NoorAiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(c: NoorAiConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessages(m: List<NoorAiMessageEntity>)

    @Query("DELETE FROM messages WHERE convId = :id")
    abstract suspend fun deleteMessages(id: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    abstract suspend fun deleteConversation(id: Long)

    @Query("UPDATE conversations SET title = :title, customTitle = :custom WHERE id = :id")
    abstract suspend fun rename(id: Long, title: String, custom: Boolean)

    @Query("UPDATE conversations SET pinned = :pinned WHERE id = :id")
    abstract suspend fun setPinned(id: Long, pinned: Boolean)

    @Query(
        "SELECT * FROM conversations WHERE title LIKE '%' || :q || '%' OR id IN " +
            "(SELECT DISTINCT convId FROM messages WHERE text LIKE '%' || :q || '%') ORDER BY pinned DESC, updated DESC LIMIT 100"
    )
    abstract suspend fun search(q: String): List<NoorAiConversationEntity>

    @Query("SELECT COUNT(*) FROM conversations")
    abstract suspend fun count(): Int

    @Transaction
    open suspend fun replaceMessages(c: NoorAiConversationEntity, items: List<NoorAiMessageEntity>) {
        upsert(c)
        deleteMessages(c.id)
        if (items.isNotEmpty()) insertMessages(items)
    }

    @Transaction
    open suspend fun delete(id: Long) {
        deleteMessages(id)
        deleteConversation(id)
    }
}

@Database(entities = [NoorAiConversationEntity::class, NoorAiMessageEntity::class], version = 1, exportSchema = false)
abstract class NoorAiHistoryDb : RoomDatabase() {
    abstract fun dao(): NoorAiHistoryDao

    companion object {
        @Volatile private var instance: NoorAiHistoryDb? = null
        fun get(context: Context): NoorAiHistoryDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NoorAiHistoryDb::class.java, "noor_ai_history.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build().also { instance = it }
        }
    }
}
