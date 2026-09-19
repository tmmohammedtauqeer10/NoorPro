package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surahId: Int,
    val surahNameEng: String,
    val surahNameAr: String,
    val ayahNumber: Int,
    val ayahText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE surahId = :surahId AND ayahNumber = :ayahNumber")
    suspend fun deleteBookmark(surahId: Int, ayahNumber: Int): Int

    @Query("SELECT * FROM bookmarks WHERE surahId = :surahId AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getBookmark(surahId: Int, ayahNumber: Int): BookmarkEntity?
    
    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksSync(): List<BookmarkEntity>
}

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        fun getDatabase(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookmarkDatabase::class.java,
                    "bookmarks_v2.db"
                )
                    // No destructive fallback: bookmarks are user data. When the
                    // schema changes, bump the version AND add a proper Migration
                    // instead of re-enabling fallbackToDestructiveMigration().
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
