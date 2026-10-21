package com.noorpro.app.data.spiritual

import android.content.Context
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/** Bundled, offline Darood / Ruhani / Tasawwuf content (gzip JSON files in the assets content folder). */
data class SpiritualItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val arabic: String,
    val translit: String,
    val urdu: String,
    val english: String,
    val whenText: String,
    val virtue: String,
    val source: String,
    val count: Int
)

data class SpiritualCategory(val id: String, val title: String, val items: List<SpiritualItem>)

data class SpiritualSection(
    val id: String,
    val title: String,
    val titleUr: String,
    val intro: String,
    val disclaimer: String,
    val categories: List<SpiritualCategory>
)

object SpiritualContentRepository {
    private val cache = HashMap<String, SpiritualSection>()

    @Synchronized
    fun load(context: Context, sectionId: String): SpiritualSection {
        cache[sectionId]?.let { return it }
        val text = context.assets.open("content/$sectionId.jgz").use { raw ->
            GZIPInputStream(raw).bufferedReader(Charsets.UTF_8).readText()
        }
        val root = JSONObject(text)
        val cats = root.getJSONArray("categories")
        val categories = (0 until cats.length()).map { ci ->
            val c = cats.getJSONObject(ci)
            val arr = c.getJSONArray("items")
            SpiritualCategory(
                id = c.optString("id"),
                title = c.optString("title"),
                items = (0 until arr.length()).map { ii ->
                    val o = arr.getJSONObject(ii)
                    SpiritualItem(
                        id = sectionId + "/" + o.optString("id"),
                        title = o.optString("title"),
                        subtitle = o.optString("subtitle"),
                        arabic = o.optString("arabic"),
                        translit = o.optString("translit"),
                        urdu = o.optString("urdu"),
                        english = o.optString("english"),
                        whenText = o.optString("when"),
                        virtue = o.optString("virtue"),
                        source = o.optString("source"),
                        count = o.optInt("count", 0)
                    )
                }
            )
        }
        val section = SpiritualSection(
            id = root.optString("id"),
            title = root.optString("title"),
            titleUr = root.optString("titleUr"),
            intro = root.optString("intro"),
            disclaimer = root.optString("disclaimer"),
            categories = categories
        )
        cache[sectionId] = section
        return section
    }
}

/** Favourites and Arabic font size, persisted in SharedPreferences. */
object SpiritualPrefs {
    private const val FILE = "spiritual_content_prefs"
    private const val KEY_FAV = "favourites"
    private const val KEY_FONT = "arabic_font_sp"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun favourites(context: Context): Set<String> = prefs(context).getStringSet(KEY_FAV, emptySet())?.toSet() ?: emptySet()

    fun toggleFavourite(context: Context, id: String): Set<String> {
        val next = favourites(context).toMutableSet().apply { if (!add(id)) remove(id) }
        prefs(context).edit().putStringSet(KEY_FAV, next).apply()
        return next
    }

    fun arabicFontSize(context: Context): Float = prefs(context).getFloat(KEY_FONT, 26f)

    fun setArabicFontSize(context: Context, size: Float) {
        prefs(context).edit().putFloat(KEY_FONT, size).apply()
    }
}
