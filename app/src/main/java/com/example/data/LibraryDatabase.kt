package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "book_downloads")
data class BookDownloadEntity(
    @PrimaryKey val bookId: String,
    val isDownloaded: Boolean = false,
    val filePath: String? = null
)

@Dao
interface BookDownloadDao {
    @Query("SELECT * FROM book_downloads WHERE bookId = :bookId LIMIT 1")
    suspend fun getDownloadStateSync(bookId: String): BookDownloadEntity?

    @Query("SELECT * FROM book_downloads WHERE bookId = :bookId LIMIT 1")
    fun getDownloadState(bookId: String): Flow<BookDownloadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDownloadState(entity: BookDownloadEntity)
}

@Database(entities = [BookDownloadEntity::class], version = 1, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun bookDownloadDao(): BookDownloadDao

    companion object {
        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        fun getDatabase(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "library_v1.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
