package com.noorpro.app.quran

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 1.0.76 word-by-word data from Quran.Foundation (quran.com) public endpoints:
 *  - words + word-by-word English meaning/transliteration: api.quran.com/api/v4/verses/by_chapter
 *  - per-word recitation timings ("segments"): api.qurancdn.com/api/qdc/audio/reciters/{id}/audio_files
 *  - single-word audio: audio.qurancdn.com/wbw/SSS_AAA_WWW.mp3 (streamed, never stored)
 * Text is shown unmodified with the attribution "Quran data provided by Quran.Foundation".
 * Only the JSON is cached in-app (filesDir/qwbw) for offline display; audio is streamed only.
 */
data class QWord(val position: Int, val text: String, val meaning: String, val transliteration: String, val audioPath: String)
data class QVerse(val ayah: Int, val key: String, val juz: Int, val words: List<QWord>)
/** [fromMs,toMs] per word position for one verse, plus the verse window. */
data class QVerseTiming(val ayah: Int, val fromMs: Long, val toMs: Long, val words: List<Triple<Int, Long, Long>>)
data class QChapterAudio(val audioUrl: String, val timings: List<QVerseTiming>)

data class HifzReciter(val id: Int, val name: String)
val HIFZ_RECITERS = listOf(HifzReciter(7, "Mishary al-Afasy"), HifzReciter(6, "Mahmoud al-Husary"), HifzReciter(12, "Al-Husary (Muallim)"), HifzReciter(9, "Al-Minshawi"))

const val QF_ATTRIBUTION = "Quran data, word timings & word audio provided by Quran.Foundation (quran.com)"
const val WBW_AUDIO_BASE = "https://audio.qurancdn.com/"

class WordByWordRepository(private val context: Context) {
    private val dir = File(context.filesDir, "qwbw").apply { mkdirs() }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12_000; c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", "NoorPro/1.0 (Android; Quran reader)")
        c.setRequestProperty("Accept", "application/json")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    private suspend fun cached(name: String, url: String): String = withContext(Dispatchers.IO) {
        val f = File(dir, name)
        if (f.exists() && f.length() > 50) return@withContext f.readText()
        val body = get(url)
        f.writeText(body)
        body
    }

    suspend fun verses(chapter: Int): List<QVerse> {
        val raw = cached("w_$chapter.json",
            "https://api.quran.com/api/v4/verses/by_chapter/$chapter?words=true&word_fields=text_uthmani&per_page=300&language=en")
        return parseVerses(raw)
    }

    suspend fun chapterAudio(chapter: Int, reciter: Int): QChapterAudio {
        val raw = cached("a_${reciter}_$chapter.json",
            "https://api.qurancdn.com/api/qdc/audio/reciters/$reciter/audio_files?chapter=$chapter&segments=true")
        return parseAudio(raw)
    }

    fun clearCache() { dir.listFiles()?.forEach { it.delete() } }

    companion object {
        fun parseVerses(raw: String): List<QVerse> {
            val arr = JSONObject(raw).getJSONArray("verses")
            return (0 until arr.length()).map { i ->
                val v = arr.getJSONObject(i)
                val ws = v.optJSONArray("words") ?: JSONArray()
                QVerse(
                    ayah = v.getInt("verse_number"), key = v.getString("verse_key"), juz = v.optInt("juz_number", 0),
                    words = (0 until ws.length()).map { ws.getJSONObject(it) }
                        .filter { it.optString("char_type_name") == "word" }
                        .map { w ->
                            QWord(w.getInt("position"), w.optString("text_uthmani", w.optString("text")),
                                w.optJSONObject("translation")?.optString("text").orEmpty(),
                                w.optJSONObject("transliteration")?.optString("text").orEmpty(),
                                w.optString("audio_url"))
                        }
                )
            }
        }

        fun parseAudio(raw: String): QChapterAudio {
            val f = JSONObject(raw).getJSONArray("audio_files").getJSONObject(0)
            val vt = f.optJSONArray("verse_timings") ?: JSONArray()
            return QChapterAudio(f.getString("audio_url"), (0 until vt.length()).map { i ->
                val t = vt.getJSONObject(i)
                val segs = t.optJSONArray("segments") ?: JSONArray()
                QVerseTiming(
                    ayah = t.getString("verse_key").substringAfter(":").toInt(),
                    fromMs = t.getLong("timestamp_from"), toMs = t.getLong("timestamp_to"),
                    // Segments are [position, from, to]; malformed one-element entries are skipped.
                    words = (0 until segs.length()).mapNotNull { j ->
                        val s = segs.optJSONArray(j) ?: return@mapNotNull null
                        if (s.length() < 3) null else Triple(s.getInt(0), s.getLong(1), s.getLong(2))
                    }
                )
            })
        }

        /** Word position being recited at [posMs] within a verse, or null between words. */
        fun activeWord(t: QVerseTiming?, posMs: Long): Int? =
            t?.words?.lastOrNull { posMs >= it.second && posMs < it.third + 120 }?.first
    }
}
