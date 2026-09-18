package com.noorpro.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran")
data class QuranVerse(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "jozz") val jozz: Int,
    @ColumnInfo(name = "sora") val sora: Int,
    @ColumnInfo(name = "sora_name_en") val soraNameEn: String,
    @ColumnInfo(name = "sora_name_ar") val soraNameAr: String,
    @ColumnInfo(name = "page") val page: Int,
    @ColumnInfo(name = "line_start") val lineStart: Int,
    @ColumnInfo(name = "line_end") val lineEnd: Int,
    @ColumnInfo(name = "aya_no") val ayaNo: Int,
    @ColumnInfo(name = "aya_text") val ayaText: String,
    @ColumnInfo(name = "aya_text_emlaey") val ayaTextEmlaey: String,
    @ColumnInfo(name = "maany_aya") val maanyAya: String,
    @ColumnInfo(name = "earab_quran") val earabQuran: String,
    @ColumnInfo(name = "reasons_of_verses") val reasonsOfVerses: String,
    @ColumnInfo(name = "tafseer_saadi") val tafseerSaadi: String,
    @ColumnInfo(name = "tafseer_moysar") val tafseerMoysar: String,
    @ColumnInfo(name = "tafseer_bughiu") val tafseerBughiu: String,
    @ColumnInfo(name = "aya_text_tashkil") val ayaTextTashkil: String
)
