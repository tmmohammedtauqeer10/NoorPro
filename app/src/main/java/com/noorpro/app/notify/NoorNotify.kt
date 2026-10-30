package com.noorpro.app.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.noorpro.app.MainActivity
import com.noorpro.app.R

/**
 * 1.0.96: one design language for every NoorPro notification.
 *
 * - [SMALL_ICON]: 1.0.97 the NoorPro N-crescent logo (white on transparent, 24dp vector) for every
 *   channel and the status bar.
 * - [accent]: NoorPro green, gold is used for highlights inside the art.
 * - Prayer art is bundled (drawable-nodpi, ~250 KB total): a rounded large-icon tile per time of day
 *   (1.0.97: the cream N logo on that time of day's tint) and a text-free 2.3:1 banner (geometric lattice, masjid skyline, sun / crescent) in a dark
 *   and a light version, picked from the system night mode so it matches the shade.
 */
object NoorNotify {
    val SMALL_ICON: Int = R.drawable.ic_stat_noorpro
    const val ACCENT = 0xFF0B6243.toInt()
    const val GOLD = 0xFFD4AF37.toInt()

    fun accent(context: Context): Int = ContextCompat.getColor(context, R.color.noor_notification_accent)

    enum class Phase { DAWN, NOON, AFTERNOON, SUNSET, NIGHT }

    /** Time-of-day look for a prayer / reminder name. */
    fun phaseFor(prayer: String?): Phase = when (prayer?.trim()?.lowercase()) {
        "fajr", "sunrise", "tahajjud" -> Phase.DAWN
        "dhuhr", "zuhr", "jumuah", "jumu'ah", "jummah" -> Phase.NOON
        "asr" -> Phase.AFTERNOON
        "maghrib", "iftar" -> Phase.SUNSET
        "isha", "suhoor", "sehri" -> Phase.NIGHT
        else -> Phase.NOON
    }

    fun arabicName(prayer: String?): String? = when (prayer?.trim()?.lowercase()) {
        "fajr" -> "\u0627\u0644\u0641\u062C\u0631"
        "dhuhr", "zuhr" -> "\u0627\u0644\u0638\u0647\u0631"
        "asr" -> "\u0627\u0644\u0639\u0635\u0631"
        "maghrib" -> "\u0627\u0644\u0645\u063A\u0631\u0628"
        "isha" -> "\u0627\u0644\u0639\u0634\u0627\u0621"
        "jumuah", "jumu'ah", "jummah" -> "\u0627\u0644\u062C\u0645\u0639\u0629"
        else -> null
    }

    /** The five daily prayers that can be marked as prayed from a notification. */
    fun isFardPrayer(prayer: String?): Boolean = prayer in setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun tileRes(phase: Phase): Int = when (phase) {
        Phase.DAWN -> R.drawable.notif_tile_dawn
        Phase.NOON -> R.drawable.notif_tile_noon
        Phase.AFTERNOON -> R.drawable.notif_tile_afternoon
        Phase.SUNSET -> R.drawable.notif_tile_sunset
        Phase.NIGHT -> R.drawable.notif_tile_night
    }

    fun bannerRes(phase: Phase, dark: Boolean): Int = when (phase) {
        Phase.DAWN -> if (dark) R.drawable.notif_banner_dawn_dark else R.drawable.notif_banner_dawn_light
        Phase.NOON -> if (dark) R.drawable.notif_banner_noon_dark else R.drawable.notif_banner_noon_light
        Phase.AFTERNOON -> if (dark) R.drawable.notif_banner_afternoon_dark else R.drawable.notif_banner_afternoon_light
        Phase.SUNSET -> if (dark) R.drawable.notif_banner_sunset_dark else R.drawable.notif_banner_sunset_light
        Phase.NIGHT -> if (dark) R.drawable.notif_banner_night_dark else R.drawable.notif_banner_night_light
    }

    fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun bitmap(context: Context, res: Int): Bitmap? = runCatching { BitmapFactory.decodeResource(context.resources, res) }.getOrNull()

    fun tile(context: Context, prayer: String?): Bitmap? = bitmap(context, tileRes(phaseFor(prayer)))

    fun banner(context: Context, prayer: String?): Bitmap? = bitmap(context, bannerRes(phaseFor(prayer), isDark(context)))

    /** Round initials avatar (green gradient, cream letters) for people without a photo. */
    fun avatar(name: String, sizePx: Int = 192): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), 0xFF0B3D31.toInt(), 0xFF0E8C73.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bg)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = sizePx * 0.03f; color = GOLD }
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - ring.strokeWidth, ring)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFAF3E6.toInt(); textSize = sizePx * 0.38f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val y = sizePx / 2f - (tp.descent() + tp.ascent()) / 2f
        c.drawText(initials(name), sizePx / 2f, y, tp)
        return bmp
    }

    fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "N"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }

    private val immutable = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** Opens the app on a screen ("OPEN_PRAYER", "OPEN_QIBLA", "OPEN_QURAN", ...). */
    fun openApp(context: Context, requestCode: Int, extra: String): PendingIntent {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(extra, true)
        }
        return PendingIntent.getActivity(context, requestCode, open, immutable)
    }

    /** Broadcast to [com.noorpro.app.receiver.NotificationActionReceiver]. */
    fun action(context: Context, requestCode: Int, action: String, extras: Intent.() -> Unit = {}): PendingIntent {
        val i = Intent(context, com.noorpro.app.receiver.NotificationActionReceiver::class.java).apply {
            this.action = action
            extras()
        }
        return PendingIntent.getBroadcast(context, requestCode, i, immutable)
    }
}
