package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey val date: String,
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
)

@Entity(tableName = "qaza_counts")
data class QazaCountEntity(
    @PrimaryKey @ColumnInfo(name = "prayer_name") val prayerName: String,
    @ColumnInfo(name = "total_remaining") val totalRemaining: Int,
    @ColumnInfo(name = "total_completed") val totalCompleted: Int = 0
)

@Dao
interface PrayerTrackerDao {
    @Query("SELECT * FROM prayer_logs WHERE date = :date LIMIT 1")
    fun getLogForDate(date: String): Flow<PrayerLogEntity?>

    @Query("SELECT * FROM prayer_logs WHERE date = :date LIMIT 1")
    suspend fun getLogForDateSync(date: String): PrayerLogEntity?

    @Query("SELECT * FROM prayer_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<PrayerLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerLog(log: PrayerLogEntity)

    @Query("SELECT * FROM qaza_counts")
    fun getAllQazaCounts(): Flow<List<QazaCountEntity>>

    @Query("SELECT * FROM qaza_counts")
    suspend fun getAllQazaCountsSync(): List<QazaCountEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultQazaCounts(counts: List<QazaCountEntity>)

    @Query("UPDATE qaza_counts SET total_remaining = :remaining, total_completed = :completed WHERE prayer_name = :prayerName")
    suspend fun updateQazaCount(prayerName: String, remaining: Int, completed: Int)
}

@Database(entities = [PrayerLogEntity::class, QazaCountEntity::class], version = 2, exportSchema = false)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerTrackerDao(): PrayerTrackerDao

    companion object {
        @Volatile private var INSTANCE: PrayerDatabase? = null
        fun getDatabase(context: Context): PrayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerDatabase::class.java,
                    "prayer_tracker.db"
                )
                    // No destructive fallback: prayer/Qaza logs are user data. On a
                    // schema change, bump the version AND add a proper Migration
                    // instead of re-enabling fallbackToDestructiveMigration().
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
