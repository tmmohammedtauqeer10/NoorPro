package com.noorpro.app.ui.screens

import android.content.Context
import com.noorpro.app.data.DuaData
import com.noorpro.app.data.LocalHijri
import com.noorpro.app.data.OfflineQuranAssets
import com.noorpro.app.data.media.edit.CountdownKind
import com.noorpro.app.data.media.edit.IslamicDates
import org.json.JSONObject
import java.util.Calendar
import java.util.zip.GZIPInputStream

/*
 * 1.1.3 editor pro — Islamic sticker content from the app's own bundled, offline data:
 * Qur'an (Arabic + Sahih International English), du'a list, Sahih al-Bukhari (first sections), Umm al-Qura Hijri
 * dates. Nothing is fetched from the network.
 */
object EditorStickerData {
    private var hadith: List<QuoteItem>? = null

    fun ayahs(ctx: Context, q: String): List<QuoteItem> {
        val core = runCatching { OfflineQuranAssets.loadCore(ctx) }.getOrNull() ?: return emptyList()
        fun item(a: OfflineQuranAssets.CoreAyah) = QuoteItem(a.english.trim(), "${core.surahNames.getOrNull(a.surah - 1)?.first ?: "Surah ${a.surah}"} ${a.surah}:${a.ayah}", a.arabic.trim())
        val query = q.trim()
        Regex("^(\\d{1,3})\\s*[:.]\\s*(\\d{1,3})$").find(query)?.let { m ->
            val s = m.groupValues[1].toInt(); val a = m.groupValues[2].toInt()
            return core.ayahs.filter { it.surah == s && it.ayah == a }.map(::item)
        }
        if (query.isBlank()) {
            // A short, beloved selection; the first one changes every day ("Ayah of the day").
            val picks = listOf(94 to 6, 2 to 286, 13 to 28, 2 to 152, 65 to 3, 39 to 53, 3 to 139, 20 to 114, 2 to 186, 93 to 5, 16 to 128, 29 to 69)
            val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val rotated = picks.indices.map { picks[(it + day) % picks.size] }
            return rotated.mapNotNull { (s, a) -> core.ayahs.firstOrNull { it.surah == s && it.ayah == a }?.let(::item) }
        }
        val words = query.lowercase().split(Regex("\\s+")).filter { it.length > 1 }
        return core.ayahs.asSequence().filter { a -> a.english.length < 260 && words.all { a.english.lowercase().contains(it) } }.take(30).map(::item).toList()
    }

    fun duas(q: String): List<QuoteItem> {
        val all = DuaData.categories.flatMap { c -> c.duas.map { d -> QuoteItem(d.english.trim(), "${d.title} · ${c.title}".take(48), d.arabic.trim()) } }
        val words = q.trim().lowercase().split(Regex("\\s+")).filter { it.length > 1 }
        return if (words.isEmpty()) all.take(30) else all.filter { d -> words.all { (d.text + " " + d.ref).lowercase().contains(it) } }.take(30)
    }

    fun hadith(ctx: Context, q: String): List<QuoteItem> {
        val list = hadith ?: runCatching {
            val out = ArrayList<QuoteItem>()
            val files = ctx.assets.list("hadith/eng-bukhari").orEmpty().sortedBy { it.substringBefore('.').toIntOrNull() ?: 9999 }.take(6)
            for (f in files) {
                val arr = JSONObject(GZIPInputStream(ctx.assets.open("hadith/eng-bukhari/$f")).bufferedReader().use { it.readText() }).optJSONArray("hadiths") ?: continue
                for (k in 0 until arr.length()) {
                    val o = arr.optJSONObject(k) ?: continue
                    val t = o.optString("text").trim()
                    if (t.length in 25..320) out.add(QuoteItem(t, "Sahih al-Bukhari ${o.optString("hadithnumber")}"))
                }
            }
            out.toList()
        }.getOrDefault(emptyList()).also { hadith = it }
        val words = q.trim().lowercase().split(Regex("\\s+")).filter { it.length > 1 }
        return if (words.isEmpty()) list.take(30) else list.filter { h -> words.all { h.text.lowercase().contains(it) } }.take(30)
    }

    private val hijriOf = IslamicDates.HijriOf { cal -> LocalHijri.fromGregorian(cal).let { it.month to it.day } }

    fun hijriToday(): String = runCatching {
        val h = LocalHijri.fromGregorian(Calendar.getInstance())
        IslamicDates.hijriLabel(h.day, h.month, h.year, LocalHijri.monthsEn)
    }.getOrDefault("")

    fun countdown(kind: CountdownKind): String = runCatching {
        IslamicDates.countdownLabel(kind, IslamicDates.daysUntil(kind, Calendar.getInstance(), hijriOf))
    }.getOrDefault(kind.label)

    fun sources(ctx: Context, prayer: String = "", handle: String = "") = StickerSources(
        ayahs = { ayahs(ctx, it) }, duas = { duas(it) }, hadith = { hadith(ctx, it) },
        hijri = { hijriToday() }, prayer = { prayer }, countdown = { countdown(it) }, handle = handle
    )

    /** "Asr · 3:45 PM" from the same snapshot the prayer widgets use (offline calculator fallback). */
    suspend fun prayerLabel(ctx: Context): String = runCatching {
        val s = com.noorpro.app.prayer.widget.PrayerWidgetUpdater.loadSnapshot(ctx)
        if (s.nextTime.contains("-")) "" else "${s.nextName} · ${com.noorpro.app.prayer.widget.PrayerWidgetUpdater.displayTime(ctx, s.nextTime)}"
    }.getOrDefault("")
}
