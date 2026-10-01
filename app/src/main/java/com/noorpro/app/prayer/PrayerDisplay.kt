package com.noorpro.app.prayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.noorpro.app.R

/**
 * Shared presentation helpers for the prayer widgets and notifications so every surface uses the
 * same "Islamic green + gold" look as the Home hero (see HomeScreen / noor_hero_mosque).
 */
object PrayerDisplay {
    /** Emerald accent used for notification tinting (matches existing 0xFF0E8C73). */
    const val EMERALD = 0xFF0E8C73.toInt()

    /** Deep Islamic green used for colorized adhan notifications. */
    const val DEEP_GREEN = 0xFF0B4A3B.toInt()

    /** Matte gold used for the next-prayer accent (MatteGold in ui/theme/Color.kt). */
    const val GOLD = 0xFFD4AF37.toInt()

    private val HIJRI_MONTHS = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal",
        "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal",
        "Dhul-Qadah", "Dhul-Hijjah",
    )

    /**
     * Today's Hijri date, honouring the user's adjustment from Settings.
     * Short = "19 Rabi al-Awwal", long = "19 Rabi al-Awwal 1448 AH". Empty if unavailable.
     */
    fun hijriLabel(context: Context, short: Boolean): String = runCatching {
        val adjustment = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getInt("hijri_adjustment", 0)
        val cal = android.icu.util.IslamicCalendar()
        cal.add(android.icu.util.Calendar.DAY_OF_MONTH, adjustment)
        val month = HIJRI_MONTHS.getOrElse(cal.get(android.icu.util.Calendar.MONTH)) { "" }
        val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val year = cal.get(android.icu.util.Calendar.YEAR)
        if (month.isBlank()) "" else if (short) "$day $month" else "$day $month $year AH"
    }.getOrDefault("")

    /** Renders the emerald masjid tile vector into a small bitmap for notification large icons. */
    fun masjidLargeIcon(context: Context, sizePx: Int = 192): Bitmap? = runCatching {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_masjid_tile) ?: return null
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        bitmap
    }.getOrNull()
}
