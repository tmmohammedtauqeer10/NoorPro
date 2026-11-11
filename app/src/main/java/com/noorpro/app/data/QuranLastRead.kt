package com.noorpro.app.data

import android.content.Context

/**
 * 1.0.98: persists the reader's last-read position (surah or juz + ayah) so it survives restarts and
 * the Continue Quran home-screen widget can resume it. Negative ids are juz (same convention as
 * DeenViewModel's recent reads).
 */
object QuranLastRead {
    data class Entry(val surahId: Int, val ayah: Int, val nameEnglish: String, val nameArabic: String, val savedAt: Long)

    private const val PREFS = "quran_last_read"

    fun save(context: Context, surahId: Int, ayah: Int, nameEnglish: String, nameArabic: String) {
        if (surahId == 0) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getInt("surah_id", 0) == surahId && p.getInt("ayah", 0) == ayah) return
        p.edit()
            .putInt("surah_id", surahId)
            .putInt("ayah", ayah.coerceAtLeast(1))
            .putString("name_en", nameEnglish)
            .putString("name_ar", nameArabic)
            .putLong("saved_at", System.currentTimeMillis())
            .apply()
    }

    fun load(context: Context): Entry? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = p.getInt("surah_id", 0)
        if (id == 0) return null
        return Entry(
            surahId = id,
            ayah = p.getInt("ayah", 1).coerceAtLeast(1),
            nameEnglish = p.getString("name_en", null).orEmpty(),
            nameArabic = p.getString("name_ar", null).orEmpty(),
            savedAt = p.getLong("saved_at", 0L),
        )
    }
}
