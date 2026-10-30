package com.noorpro.app.islamic.hifz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.noorpro.app.data.OfflineQuranAssets
import com.noorpro.app.quran.HifzCard
import com.noorpro.app.quran.HifzSchedule

/*
 * 1.1.6 Hifz coach (Remote Config hifz_coach_enabled, default OFF). Builds on quran/HifzStore (SM-2 cards +
 * streaks). Everything is on-device: bundled Uthmani text, Android's free SpeechRecognizer (ar-SA), local plan.
 */

/** Arabic normalisation for comparing a recitation transcript with the Mushaf text. */
object ArabicNormalizer {
    private val BISMILLAH = listOf("بسم", "الله", "الرحمن", "الرحيم")

    fun normalize(word: String): String {
        val sb = StringBuilder(word.length)
        for (ch in word) {
            val c = ch.code
            when {
                c == 0xFEFF || c == 0x0640 -> {}                       // BOM, tatweel
                c in 0x064B..0x065F || c == 0x0670 -> {}              // tashkeel, superscript alef
                c in 0x06D6..0x06ED -> {}                              // Qur'anic annotation marks, small waw/yeh
                ch == 'ٱ' || ch == 'أ' || ch == 'إ' || ch == 'آ' -> sb.append('ا')
                ch == 'ى' -> sb.append('ي')
                ch == 'ة' -> sb.append('ه')
                ch == 'ؤ' -> sb.append('و')
                ch == 'ئ' -> sb.append('ي')
                ch.isLetter() -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /** Words of a text, normalised, dropping pause marks that become empty. */
    fun words(text: String): List<String> = text.split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }

    /** Display words (with tashkeel) paired 1:1 with [words]. */
    fun displayWords(text: String): List<String> = text.replace("\uFEFF", "").split(Regex("\\s+")).filter { normalize(it).isNotEmpty() }

    /** Removes the Bismillah that the bundled text prefixes to ayah 1 of every surah except Al-Fatiha and At-Tawbah. */
    fun stripBismillah(surah: Int, ayah: Int, text: String): String {
        if (surah == 1 || ayah != 1) return text.replace("\uFEFF", "")
        val parts = text.replace("\uFEFF", "").trim().split(Regex("\\s+"))
        return if (parts.size > 4 && parts.take(4).map(::normalize) == BISMILLAH) parts.drop(4).joinToString(" ") else parts.joinToString(" ")
    }

    private fun lev(a: String, b: String): Int {
        val d = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = d[0]; d[0] = i
            for (j in 1..b.length) {
                val t = d[j]
                d[j] = minOf(d[j] + 1, d[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = t
            }
        }
        return d[b.length]
    }

    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        return 1.0 - lev(a, b).toDouble() / maxOf(a.length, b.length)
    }

    /** Same word allowing Uthmani vs. standard spelling (missing long alef, etc.). */
    fun sameWord(expected: String, heard: String): Boolean {
        if (expected == heard) return true
        val e = expected.replace("ا", ""); val h = heard.replace("ا", "")
        if (e == h && e.isNotEmpty()) return true
        return expected.length >= 3 && similarity(expected, heard) >= 0.8 || e.length >= 3 && similarity(e, h) >= 0.8
    }
}

enum class WordStatus { CORRECT, WRONG, MISSED }

data class WordResult(val display: String, val status: WordStatus, val heard: String? = null)

data class RecitationResult(val words: List<WordResult>, val extra: Int, val transcript: String) {
    val correct get() = words.count { it.status == WordStatus.CORRECT }
    val score: Float get() = if (words.isEmpty()) 0f else correct.toFloat() / words.size
    val passed get() = score >= 0.85f
    val missed get() = words.count { it.status == WordStatus.MISSED }
    val wrong get() = words.count { it.status == WordStatus.WRONG }
}

object RecitationChecker {
    /** Global alignment (match 2, mismatch -1, gap -1) of expected vs heard words. */
    fun check(expectedText: String, transcript: String): RecitationResult {
        val display = ArabicNormalizer.displayWords(expectedText)
        val exp = display.map(ArabicNormalizer::normalize)
        val heard = ArabicNormalizer.words(transcript)
        val n = exp.size; val m = heard.size
        val score = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) score[i][0] = -i
        for (j in 0..m) score[0][j] = -j
        for (i in 1..n) for (j in 1..m) {
            val s = if (ArabicNormalizer.sameWord(exp[i - 1], heard[j - 1])) 2 else -1
            score[i][j] = maxOf(score[i - 1][j - 1] + s, score[i - 1][j] - 1, score[i][j - 1] - 1)
        }
        val out = ArrayList<WordResult>(n); var extra = 0
        var i = n; var j = m
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val same = ArabicNormalizer.sameWord(exp[i - 1], heard[j - 1])
                if (score[i][j] == score[i - 1][j - 1] + (if (same) 2 else -1)) {
                    out += WordResult(display[i - 1], if (same) WordStatus.CORRECT else WordStatus.WRONG, heard[j - 1]); i--; j--; continue
                }
            }
            if (i > 0 && score[i][j] == score[i - 1][j] - 1) { out += WordResult(display[i - 1], WordStatus.MISSED); i-- }
            else { extra++; j-- }
        }
        return RecitationResult(out.reversed(), extra, transcript)
    }
}

/** Progressive hiding: level 0 shows every word, 1–3 hide ~25/50/75 %, 4 hides all. Deterministic per index. */
object HifzHide {
    const val MAX_LEVEL = 4
    val labels = listOf("Show all", "Hide ¼", "Hide ½", "Hide ¾", "Hide all")
    fun hidden(index: Int, level: Int): Boolean = when {
        level <= 0 -> false
        level >= MAX_LEVEL -> true
        else -> ((index * 3 + 1) % 4) < level
    }
    fun hiddenCount(words: Int, level: Int) = (0 until words).count { hidden(it, level) }
}

data class HifzAyah(val surah: Int, val ayah: Int, val text: String) {
    val words get() = ArabicNormalizer.displayWords(text)
}

data class HifzRange(val surah: Int, val from: Int, val to: Int) {
    val size get() = to - from + 1
}

data class SurahInfo(val id: Int, val name: String, val arabic: String, val ayahs: Int)

/** Bundled Uthmani text access (no network). */
object HifzQuran {
    fun surahs(context: Context): List<SurahInfo> {
        val core = OfflineQuranAssets.loadCore(context)
        val counts = core.ayahs.groupingBy { it.surah }.eachCount()
        return core.surahNames.mapIndexed { i, (en, ar) -> SurahInfo(i + 1, en, ar, counts[i + 1] ?: 0) }
    }
    fun ayahs(context: Context, r: HifzRange): List<HifzAyah> =
        OfflineQuranAssets.loadCore(context).ayahs.filter { it.surah == r.surah && it.ayah in r.from..r.to }
            .map { HifzAyah(it.surah, it.ayah, ArabicNormalizer.stripBismillah(it.surah, it.ayah, it.arabic)) }
    fun clamp(r: HifzRange, count: Int): HifzRange {
        val from = r.from.coerceIn(1, count); val to = r.to.coerceIn(from, count)
        return HifzRange(r.surah, from, to)
    }
}

/** Popular starting points for memorisation. */
val HIFZ_QUICK_PICKS = listOf(
    "Al-Fatiha" to HifzRange(1, 1, 7), "Ayat al-Kursi" to HifzRange(2, 255, 255), "Al-Ikhlas" to HifzRange(112, 1, 4),
    "Al-Falaq" to HifzRange(113, 1, 5), "An-Nas" to HifzRange(114, 1, 6), "Al-Mulk 1–5" to HifzRange(67, 1, 5),
    "Al-Baqarah 285–286" to HifzRange(2, 285, 286), "Al-Asr" to HifzRange(103, 1, 3),
)

data class DueGroup(val surah: Int, val ayahs: List<Int>) {
    /** Contiguous ranges, e.g. [1,2,3,7] -> "1–3, 7". */
    val label: String get() {
        val out = mutableListOf<String>(); var start = ayahs.first(); var prev = start
        for (a in ayahs.drop(1) + Int.MAX_VALUE) {
            if (a == prev + 1) { prev = a; continue }
            out += if (start == prev) "$start" else "$start–$prev"; start = a; prev = a
        }
        return out.joinToString(", ")
    }
}

data class HifzPlan(val due: List<DueGroup>, val dueCount: Int, val learning: Int, val memorised: Int, val forecast: List<Int>) {
    val totalPractised get() = learning + memorised
}

object HifzPlanner {
    /** Due today (incl. overdue), counts, and how many ayahs come due on each of the next 7 days. */
    fun plan(cards: Map<Pair<Int, Int>, HifzCard>, today: Long): HifzPlan {
        val due = cards.filter { it.value.due <= today }.keys.groupBy({ it.first }, { it.second })
            .toSortedMap().map { (s, a) -> DueGroup(s, a.sorted()) }
        val memorised = cards.values.count { HifzSchedule.memorised(it) }
        val forecast = (1..7).map { d -> cards.values.count { it.due == today + d } }
        return HifzPlan(due, due.sumOf { it.ayahs.size }, cards.size - memorised, memorised, forecast)
    }
}

/** Prompt for optional Noor AI memorisation tips (no personal data; transcript summary only). */
object HifzAiPrompt {
    fun tips(surahName: String, range: HifzRange, result: RecitationResult?): String = buildString {
        append("I am memorising Surah $surahName, ayahs ${range.from} to ${range.to}. ")
        if (result != null) append("In my last practice I recalled ${result.correct} of ${result.words.size} words; I missed ${result.missed} and mixed up ${result.wrong}. ")
        append("Give me 4 short, practical, encouraging tips for memorising and revising these ayahs (repetition, linking ayahs, using them in salah, revision timing). ")
        append("Keep it under 120 words, no rulings, and remind me to check my recitation with a qualified teacher.")
    }
}

/** Thin wrapper over Android's free on-device/Google speech recogniser for Arabic. Never throws. */
class HifzRecognizer(private val context: Context) {
    private var sr: SpeechRecognizer? = null

    val available: Boolean get() = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    fun start(onPartial: (String) -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit) {
        stop()
        val r = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull() ?: return onError(UNAVAILABLE)
        sr = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(b: Bundle?) { b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial) }
            override fun onResults(b: Bundle?) {
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t.isNullOrBlank()) onError(NOTHING_HEARD) else onResult(t)
            }
            override fun onError(error: Int) {
                onError(when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NOTHING_HEARD
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed to check your recitation."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Arabic speech recognition needs the Arabic offline pack or a connection on this phone."
                    else -> UNAVAILABLE
                })
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        runCatching { r.startListening(intent) }.onFailure { onError(UNAVAILABLE) }
    }

    fun stop() { runCatching { sr?.destroy() }; sr = null }

    companion object {
        const val UNAVAILABLE = "Speech recognition isn't available on this phone. You can still practise and grade yourself."
        const val NOTHING_HEARD = "We didn't catch that. Recite clearly, close to the phone, and try again."
    }
}
