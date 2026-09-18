package com.noorpro.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "zakat_history")
data class ZakatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val totalAssets: Double,
    val zakatDue: Double,
    val goldValue: Double,
    val silverValue: Double,
    val cashValue: Double,
    val businessValue: Double,
    val assetsValue: Double
)

@Dao
interface ZakatHistoryDao {
    @Query("SELECT * FROM zakat_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<ZakatHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ZakatHistoryEntity)

    @Delete
    suspend fun delete(history: ZakatHistoryEntity)
}

@Database(entities = [ZakatHistoryEntity::class], version = 1, exportSchema = false)
abstract class ZakatDatabase : RoomDatabase() {
    abstract fun zakatDao(): ZakatHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ZakatDatabase? = null

        fun getDatabase(context: android.content.Context): ZakatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZakatDatabase::class.java,
                    "zakat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
