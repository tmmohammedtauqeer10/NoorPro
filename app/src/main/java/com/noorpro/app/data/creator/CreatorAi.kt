package com.noorpro.app.data.creator

import android.content.Context
import com.noorpro.app.data.NoorAiLocalCorpus
import com.noorpro.app.data.OfflineQuranAssets
import com.noorpro.app.data.QuranMetaData
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/**
 * 1.1.4 Creator Studio AI helper. Caption ideas, hashtags and translation reuse the existing
 * askNoorAi backend with client-side prompt framing (no new backend mode). Ayah/hadith reference
 * checks run fully offline against the Qur'an + hadith already bundled in the app.
 */
enum class CreatorAiTask(val label: String) { CAPTIONS("Caption ideas"), HASHTAGS("Hashtags"), CHECK("Check references"), TRANSLATE("Translate") }

object CreatorAiPrompts {
    val languages = listOf("ar" to "Arabic", "ur" to "Urdu", "en" to "English")

    fun captions(draft: String, topic: String): String =
        "You are helping a Muslim creator on NoorPro write a post caption. Topic: ${topic.ifBlank { "general reminder" }}. " +
            (if (draft.isNotBlank()) "Their draft: \"${draft.take(600)}\". " else "") +
            "Write 3 short, sincere caption options (max 2 sentences each, no emojis spam, no invented hadith or ayah, no fatwa). " +
            "Return ONLY a numbered list 1. 2. 3. with one caption per line."

    fun hashtags(draft: String, topic: String): String =
        "Suggest 8 relevant, respectful hashtags for this Islamic post on NoorPro. Topic: ${topic.ifBlank { "Reminder" }}. Text: \"${draft.take(600)}\". " +
            "Return ONLY the hashtags separated by spaces, each starting with #, no explanations."

    fun translate(text: String, lang: String): String {
        val name = languages.firstOrNull { it.first == lang }?.second ?: "English"
        return "Translate this post caption into $name. Keep Qur'an quotes and names accurate, keep hashtags unchanged, do not add commentary. " +
            "Return ONLY the translation.\n\n${text.take(1500)}"
    }

    /** "1. foo\n2) bar" → [foo, bar]; strips quotes, the FOLLOWUPS line and the Sources block. */
    fun parseList(answer: String): List<String> = answer.substringBefore("FOLLOWUPS:").substringBefore("\nSources:")
        .lines().map { it.trim() }
        .filter { Regex("^(\\d+[.)]|[-•*])\\s+").containsMatchIn(it) }
        .map { it.replace(Regex("^(\\d+[.)]|[-•*])\\s+"), "").trim().trim('"', '“', '”').replace("**", "") }
        .filter { it.length in 3..400 }.take(5)

    fun parseHashtags(answer: String): List<String> = Regex("#[\\p{L}\\p{N}_]{2,40}").findAll(answer.substringBefore("FOLLOWUPS:"))
        .map { it.value }.distinctBy { it.lowercase() }.take(12).toList()

    fun cleanTranslation(answer: String): String = answer.substringBefore("FOLLOWUPS:").substringBefore("\nSources:")
        .lines().filterNot { it.trim().startsWith("Note: Noor AI") }.joinToString("\n").trim()

    /** Offline hashtags from the topic + words in the caption (used instantly, and if the AI is unreachable). */
    fun localHashtags(text: String, topic: String): List<String> {
        val base = mapOf(
            "quran" to listOf("#Quran", "#QuranReflection", "#Tadabbur"), "dua" to listOf("#Dua", "#DuaOfTheDay"),
            "reminder" to listOf("#IslamicReminder", "#Deen"), "reflection" to listOf("#Reflection", "#Tazkiyah"),
            "ramadan" to listOf("#Ramadan", "#Sawm"), "hadith" to listOf("#Hadith", "#Sunnah"), "salah" to listOf("#Salah", "#Namaz"),
            "community" to listOf("#Ummah", "#MuslimCommunity"), "sabr" to listOf("#Sabr", "#Patience"), "shukr" to listOf("#Shukr", "#Alhamdulillah"),
        )
        val words = (text + " " + topic).lowercase()
        val picked = base.filterKeys { words.contains(it) }.values.flatten()
        return (picked + listOf("#NoorPro", "#Ummah")).distinct().take(8)
    }
}

/** A Qur'an or hadith reference found in a caption, and what the offline data says about it. */
data class RefCheck(
    val raw: String,
    val kind: Kind,
    val ok: Boolean,
    val message: String,
    val surah: Int = 0, val ayah: Int = 0, val ayahEnd: Int = 0,
    val collection: String = "", val number: String = "",
    /** Offline text (Arabic ayah + English, or hadith English) when found. */
    val text: String = "",
) { enum class Kind { QURAN, HADITH } }

object ReferenceChecker {
    private val collections = linkedMapOf(
        "bukhari" to "Sahih al-Bukhari", "muslim" to "Sahih Muslim", "abu ?dawu?o?d" to "Sunan Abi Dawud",
        "tirmidhi" to "Jami at-Tirmidhi", "nasa'?i" to "Sunan an-Nasa'i", "ibn ?majah" to "Sunan Ibn Majah",
    )
    private val dirs = mapOf("Sahih al-Bukhari" to "bukhari", "Sahih Muslim" to "muslim", "Sunan Abi Dawud" to "abudawud",
        "Jami at-Tirmidhi" to "tirmidhi", "Sunan an-Nasa'i" to "nasai", "Sunan Ibn Majah" to "ibnmajah")

    private fun norm(s: String) = s.lowercase().replace(Regex("[^a-z]"), "")
    private val surahIndex: Map<String, Int> by lazy {
        QuranMetaData.surahNamesEn.mapIndexed { i, n -> norm(n.replace(Regex("^(Al|An|Ar|As|At|Ash|Adh|Ad|Az|Aal)-"), "")) to i + 1 }.toMap() +
            QuranMetaData.surahNamesEn.mapIndexed { i, n -> norm(n) to i + 1 }
    }
    fun surahByName(name: String): Int? {
        val n = norm(name.replace(Regex("^(surah|surat|sura)\\s+", RegexOption.IGNORE_CASE), "").replace(Regex("^(al|an|ar|as|at|ash|adh|ad|az|aal)[- ]", RegexOption.IGNORE_CASE), ""))
        if (n.length < 2) return null
        surahIndex[n]?.let { return it }
        // tolerant spelling: Baqarah/Baqara, Ikhlas/Ikhlaas, Rahman/Rahmaan
        val squash = { s: String -> s.replace(Regex("(.)\\1+"), "$1").removeSuffix("h") }
        return surahIndex.entries.firstOrNull { squash(it.key) == squash(n) }?.value
    }

    /** Finds references like "2:255", "Al-Baqarah 2:255", "Surah Yusuf 12:4-6", "Bukhari 6018", "Sahih Muslim #2564". */
    fun find(text: String): List<RefCheck> {
        val out = mutableListOf<RefCheck>()
        val quran = Regex("(?:(?:surah|surat|sura)\\s+)?([A-Za-z'’-]{2,20}(?:\\s[A-Za-z'’-]{2,12})?)?\\s*[(\\[]?\\b(\\d{1,3})\\s*:\\s*(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?", RegexOption.IGNORE_CASE)
        quran.findAll(text).forEach { m ->
            val name = m.groupValues[1].trim()
            val s = m.groupValues[2].toInt(); val a = m.groupValues[3].toInt(); val e = m.groupValues[4].toIntOrNull() ?: a
            out += checkAyah(m.value.trim(), name.takeIf { it.isNotBlank() && !it.equals("surah", true) }, s, a, e)
        }
        val hadith = Regex("(?:sahih\\s+|sunan\\s+|jami\\s+(?:at-)?)?(" + collections.keys.joinToString("|") + ")\\s*(?:no\\.?|#|hadith)?\\s*(\\d{1,5}[a-z]?)", RegexOption.IGNORE_CASE)
        hadith.findAll(text).forEach { m ->
            val key = collections.keys.first { Regex("^$it$", RegexOption.IGNORE_CASE).matches(m.groupValues[1]) }
            out += RefCheck(m.value.trim(), RefCheck.Kind.HADITH, ok = true, message = "Checking ${collections[key]} ${m.groupValues[2]}…",
                collection = collections.getValue(key), number = m.groupValues[2])
        }
        return out
    }

    fun checkAyah(raw: String, name: String?, s: Int, a: Int, e: Int = a): RefCheck {
        val counts = NoorAiLocalCorpus.AYAH_COUNTS
        if (s !in 1..114) return RefCheck(raw, RefCheck.Kind.QURAN, false, "There is no surah $s — the Qur'an has 114 surahs.", s, a, e)
        val max = counts[s - 1]; val sName = QuranMetaData.surahNamesEn[s - 1]
        if (a < 1 || a > max || e < a || e > max) return RefCheck(raw, RefCheck.Kind.QURAN, false, "$sName ($s) has $max ayahs, so $s:$a${if (e != a) "-$e" else ""} doesn't exist.", s, a, e)
        val named = name?.let { surahByName(it) }
        if (name != null && named != null && named != s)
            return RefCheck(raw, RefCheck.Kind.QURAN, false, "“$name” is surah $named, but the number says $s ($sName). Check which one you meant.", s, a, e)
        return RefCheck(raw, RefCheck.Kind.QURAN, true, "$sName $s:$a${if (e != a) "-$e" else ""} ✓ found in the offline Qur'an.", s, a, e)
    }

    /** Fills in the offline text (call off the main thread). */
    fun withText(context: Context, r: RefCheck): RefCheck = runCatching {
        when (r.kind) {
            RefCheck.Kind.QURAN -> if (!r.ok) r else {
                val core = OfflineQuranAssets.loadCore(context)
                val ayahs = core.ayahs.filter { it.surah == r.surah && it.ayah in r.ayah..r.ayahEnd }
                r.copy(text = ayahs.joinToString("\n") { it.arabic } + "\n" + ayahs.joinToString(" ") { it.english })
            }
            RefCheck.Kind.HADITH -> {
                val dir = "hadith/eng-${dirs[r.collection]}"
                val files = context.assets.list(dir).orEmpty()
                var found: String? = null
                for (f in files) {
                    val json = GZIPInputStream(context.assets.open("$dir/$f")).bufferedReader().use { it.readText() }
                    val arr = JSONObject(json).optJSONArray("hadiths") ?: continue
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        if (o.optString("hadithnumber").trim().equals(r.number, true)) { found = o.optString("text").trim(); break }
                    }
                    if (found != null) break
                }
                if (found.isNullOrBlank()) r.copy(ok = false, message = "${r.collection} ${r.number} wasn't found in the offline collection. Double-check the number (numbering differs between editions).")
                else r.copy(ok = true, message = "${r.collection} ${r.number} ✓ found offline — compare the wording below.", text = found.take(700))
            }
        }
    }.getOrElse { r }
}
