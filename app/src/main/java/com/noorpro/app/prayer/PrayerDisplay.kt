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
    fun hijriLabel(context: Context, short: Boolean, atMillis: Long = System.currentTimeMillis()): String = runCatching {
        val adjustment = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getInt("hijri_adjustment", 0)
        val cal = android.icu.util.IslamicCalendar()
        cal.timeInMillis = atMillis
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

    /** "6:29 PM" from epoch millis in the device time zone. */
    fun clock12(millis: Long, tz: java.util.TimeZone = java.util.TimeZone.getDefault()): String =
        java.text.SimpleDateFormat("h:mm a", java.util.Locale.ENGLISH).apply { timeZone = tz }.format(java.util.Date(millis))

    /** "6:29 PM" from a "HH:mm" prayer time; returns the input unchanged if it cannot be parsed. */
    fun clock12(hhmm: String): String {
        val parts = hhmm.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
        val m = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: return hhmm
        val suffix = if (h < 12) "AM" else "PM"
        val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
        return String.format(java.util.Locale.ENGLISH, "%d:%02d %s", h12, m, suffix)
    }

    /** "Thu, 1 Oct 2026" */
    fun gregorianLabel(millis: Long, tz: java.util.TimeZone = java.util.TimeZone.getDefault()): String =
        java.text.SimpleDateFormat("EEE, d MMM yyyy", java.util.Locale.ENGLISH).apply { timeZone = tz }.format(java.util.Date(millis))

    /** Notification title, e.g. "Maghrib \u2014 6:29 PM". */
    fun prayerTitle(prayer: String, timeLabel: String): String = "$prayer \u2014 $timeLabel"

    /** Notification body line 1: "Thu, 1 Oct 2026 \u00B7 29 Rabi al-Awwal 1448 AH" (Hijri omitted when unknown). */
    fun dateLine(gregorian: String, hijri: String): String =
        if (hijri.isBlank()) gregorian else "$gregorian \u00B7 $hijri"

    /** Same place string the Home card shows (saved by the UI); falls back to coordinates. */
    fun locationLabel(context: Context): String =
        PrayerPrefs(context).locationLabel
}
