package com.example.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.ColumnInfo
import androidx.room.Insert
import androidx.room.OnConflictStrategy

data class SurahHeader(
    @ColumnInfo(name = "sora") val sora: Int,
    @ColumnInfo(name = "sora_name_en") val soraNameEn: String,
    @ColumnInfo(name = "sora_name_ar") val soraNameAr: String,
    @ColumnInfo(name = "verses_count") val versesCount: Int
)

@Dao
interface QuranDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<QuranVerse>): List<Long>

    @Query("DELETE FROM quran")
    suspend fun clearQuranTable(): Int

    @Query("SELECT sora as sora, MIN(sora_name_en) as sora_name_en, MIN(sora_name_ar) as sora_name_ar, COUNT(*) as verses_count FROM quran GROUP BY sora ORDER BY sora ASC")
    suspend fun getSurahHeaders(): List<SurahHeader>

    @Query("DELETE FROM quran WHERE sora = :surahId")
    suspend fun deleteVersesForSurah(surahId: Int): Int

    @Query("DELETE FROM quran WHERE jozz = :juzId")
    suspend fun deleteVersesForJuz(juzId: Int): Int

    @Query("SELECT * FROM quran WHERE sora = :surahId ORDER BY aya_no ASC")
    suspend fun getVersesForSurah(surahId: Int): List<QuranVerse>
    
    @Query("SELECT * FROM quran WHERE jozz = :juzId ORDER BY id ASC")
    suspend fun getVersesForJuz(juzId: Int): List<QuranVerse>

    @Query("SELECT * FROM quran ORDER BY id ASC")
    suspend fun getAllVerses(): List<QuranVerse>

    @Query("SELECT * FROM quran WHERE id = :id LIMIT 1")
    suspend fun getVerseById(id: Int): QuranVerse?

    @Query("SELECT * FROM quran WHERE aya_text_emlaey LIKE '%' || :query || '%' OR sora_name_en LIKE '%' || :query || '%'")
    suspend fun searchQuran(query: String): List<QuranVerse>
}
