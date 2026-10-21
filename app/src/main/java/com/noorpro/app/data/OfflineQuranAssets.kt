package com.noorpro.app.data

import android.content.Context
import android.util.JsonReader
import java.util.zip.GZIPInputStream

/**
 * Bundled, compressed Quran text used before any network source.
 *  - quran/quran_core.jgz : Uthmani Arabic + Kanz-ul-Iman (Urdu) + Ahmed Raza (English), 6236 ayahs
 *    with juz/page metadata (source: api.alquran.cloud editions).
 *  - quran/indopak.jgz    : Indo-Pak word-by-word script rows [surah, ayah, word, text].
 */
object OfflineQuranAssets {
    const val CORE_ASSET = "quran/quran_core.jgz"
    const val INDOPAK_ASSET = "quran/indopak.jgz"
    const val TOTAL_AYAHS = 6236

    /** Translation keys whose full text ships inside the app. */
    val coreTranslationKeys = setOf("en.ahmedraza", "ur.kanzuliman")

    /** Optional translations shipped as quran/tr/{key}.jgz (6236 lines, one ayah per line). */
    val extraTranslationKeys = setOf(
        "en.sahih", "en.yusufali", "en.maududi", "en.itani", "bn.bengali",
        "ta.tamil", "ml.abdulhameed", "ur.jalandhry", "ur.maududi", "tr.diyanet"
    )

    val bundledTranslationKeys = coreTranslationKeys + extraTranslationKeys

    @Volatile private var extraCache: Pair<String, List<String>>? = null

    private fun extraLines(context: Context, key: String): List<String> {
        extraCache?.let { if (it.first == key) return it.second }
        return synchronized(this) {
            extraCache?.takeIf { it.first == key }?.second ?: GZIPInputStream(context.assets.open("quran/tr/$key.jgz"), 64 * 1024)
                .bufferedReader(Charsets.UTF_8).use { it.readLines() }
                .also { check(it.size == TOTAL_AYAHS) { "Bundled translation $key is incomplete" }; extraCache = key to it }
        }
    }

    class CoreAyah(
        val surah: Int, val ayah: Int, val juz: Int, val page: Int,
        val arabic: String, val urdu: String, val english: String
    )

    class Core(val surahNames: List<Pair<String, String>>, val ayahs: List<CoreAyah>)

    @Volatile private var core: Core? = null

    fun loadCore(context: Context): Core {
        core?.let { return it }
        return synchronized(this) {
            core ?: readCore(context).also { core = it }
        }
    }

    /** Releases the in-memory copy after seeding Room. */
    fun releaseCore() { core = null }

    private fun readCore(context: Context): Core {
        val names = ArrayList<Pair<String, String>>(114)
        val ayahs = ArrayList<CoreAyah>(TOTAL_AYAHS)
        GZIPInputStream(context.assets.open(CORE_ASSET), 64 * 1024).use { gz ->
            JsonReader(gz.reader(Charsets.UTF_8)).use { r ->
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "s" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                r.beginArray(); val en = r.nextString(); val ar = r.nextString(); r.endArray()
                                names.add(en to ar)
                            }
                            r.endArray()
                        }
                        "a" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                r.beginArray()
                                ayahs.add(
                                    CoreAyah(
                                        r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt(),
                                        r.nextString(), r.nextString(), r.nextString()
                                    )
                                )
                                r.endArray()
                            }
                            r.endArray()
                        }
                        else -> r.skipValue()
                    }
                }
                r.endObject()
            }
        }
        check(names.size == 114 && ayahs.size == TOTAL_AYAHS) { "Bundled Quran core is incomplete" }
        return Core(names, ayahs)
    }

    /** Streams Indo-Pak word rows without materialising the whole JSON. */
    inline fun forEachIndoPakWord(context: Context, block: (surah: Int, ayah: Int, word: Int, text: String) -> Unit) {
        GZIPInputStream(context.assets.open(INDOPAK_ASSET), 64 * 1024).use { gz ->
            JsonReader(gz.reader(Charsets.UTF_8)).use { r ->
                r.beginArray()
                while (r.hasNext()) {
                    r.beginArray()
                    val s = r.nextInt(); val a = r.nextInt(); val w = r.nextInt(); val t = r.nextString()
                    r.endArray()
                    block(s, a, w, t)
                }
                r.endArray()
            }
        }
    }

    /** Text for a bundled translation, or null if [key] is not bundled. */
    fun bundledTranslation(context: Context, key: String, surah: Int, ayah: Int): String? {
        if (key !in bundledTranslationKeys) return null
        var index = 0
        for (i in 1 until surah) index += IslamicData.surahAyahsCount[i] ?: 0
        index += ayah - 1
        if (key in extraTranslationKeys) return extraLines(context, key).getOrNull(index)
        val c = loadCore(context)
        val hit = c.ayahs.getOrNull(index)?.takeIf { it.surah == surah && it.ayah == ayah }
            ?: c.ayahs.firstOrNull { it.surah == surah && it.ayah == ayah } ?: return null
        return if (key == "ur.kanzuliman") hit.urdu else hit.english
    }
}
