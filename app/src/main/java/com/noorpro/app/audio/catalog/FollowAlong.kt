package com.noorpro.app.audio.catalog

/**
 * Pure JVM helpers for ayah-accurate tarjuma/tafsir playback.
 * Never invents per-ayah timing: a multi-ayah clip is one group.
 */
object FollowAlong {

    enum class Kind { ARABIC, TARJUMA, TAFSIR }

    enum class Highlight { PER_AYAH, GROUP }

    data class Span(
        val surah: Int,
        val start: Int,
        val end: Int,
        val kind: Kind,
        val highlight: Highlight,
        val id: String = "",
        val url: String = "",
        val title: String = "",
        val artist: String = "",
    ) {
        val isSingle: Boolean get() = start == end
        fun covers(ayah: Int): Boolean = ayah in start..end
    }

    private val RANGE = Regex("""(?i)(?:ayat|ayahs|ayah|verses?)\s+(\d+)\s*[-\u2013\u2014\u2212-]+\s*(\d+)""")
    private val SINGLE = Regex("""(?i)(?:ayah|ayat|verse)\s+(\d+)""")

    /** Parse "Ayat 6-7", "Āyāt 6–7", "Ayah 25". */
    fun parseAyahLabel(label: String): Pair<Int, Int>? {
        val t = label.trim()
        RANGE.find(t)?.let { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            if (a <= 0 || b <= 0) return null
            return if (a <= b) a to b else b to a
        }
        SINGLE.find(t)?.let { m ->
            val a = m.groupValues[1].toInt()
            if (a <= 0) return null
            return a to a
        }
        return null
    }

    fun endAyah(label: String, start: Int, nextStart: Int?, surahCount: Int): Int {
        val parsed = parseAyahLabel(label)
        if (parsed != null) {
            val end = if (parsed.first == start || start <= 0) parsed.second else parsed.second
            return end.coerceIn(start.coerceAtLeast(1), surahCount.coerceAtLeast(start))
        }
        if (nextStart != null && nextStart > start) {
            return (nextStart - 1).coerceIn(start, surahCount.coerceAtLeast(start))
        }
        // Last unlabeled clip of a surah runs through the final ayah.
        return surahCount.coerceAtLeast(start.coerceAtLeast(1))
    }

    fun ayahAudioUrl(prefix: String, surah: Int, ayah: Int): String {
        val p = if (prefix.endsWith("/")) prefix else "$prefix/"
        return p + "%03d%03d.mp3".format(java.util.Locale.US, surah, ayah)
    }

    /**
     * Verse-by-verse tarjuma: Arabic recitation then translation for each ayah, from [fromAyah].
     * Full-surah editions (no timing): a single GROUP span for the whole surah — no fake highlights.
     */
    fun tarjumaQueue(
        surah: Int,
        ayahCount: Int,
        verseByVerse: Boolean,
        fromAyah: Int,
        tarjumaId: (Int) -> String,
        tarjumaUrl: (Int) -> String,
        arabicPrefix: String,
        reciterName: String,
        editionTitle: String,
        plusNextSurah: Boolean = false,
        nextSurah: Int? = null,
        nextAyahCount: Int = 0,
        nextTarjumaId: ((Int) -> String)? = null,
        nextTarjumaUrl: ((Int) -> String)? = null,
    ): List<Span> {
        fun one(s: Int, count: Int, from: Int, idOf: (Int) -> String, urlOf: (Int) -> String): List<Span> {
            if (!verseByVerse) {
                val start = 1
                return listOf(
                    Span(s, start, count, Kind.TARJUMA, Highlight.GROUP, id = idOf(s), url = urlOf(s),
                        title = editionTitle, artist = editionTitle),
                )
            }
            val out = ArrayList<Span>((count - from + 1) * 2)
            for (a in from.coerceAtLeast(1)..count) {
                out += Span(s, a, a, Kind.ARABIC, Highlight.PER_AYAH,
                    id = "free-ar-$s-$a", url = ayahAudioUrl(arabicPrefix, s, a),
                    title = "Ayah $a", artist = reciterName)
                out += Span(s, a, a, Kind.TARJUMA, Highlight.PER_AYAH,
                    id = idOf(a), url = urlOf(a), title = "Ayah $a", artist = editionTitle)
            }
            return out
        }
        val q = one(surah, ayahCount, fromAyah, tarjumaId, tarjumaUrl).toMutableList()
        if (plusNextSurah && nextSurah != null && nextAyahCount > 0 && nextTarjumaId != null && nextTarjumaUrl != null) {
            q += one(nextSurah, nextAyahCount, 1, nextTarjumaId, nextTarjumaUrl)
        }
        return q
    }

    data class TafsirClip(val id: String, val surah: Int, val start: Int, val end: Int, val url: String, val title: String, val artist: String)

    /**
     * For a tafsir clip covering [start,end] with no intra-clip timing:
     * play each ayah's Arabic recitation, then the group commentary clip.
     * Continue with later clips of the same (and optional next) surah.
     */
    fun tafsirQueue(
        clips: List<TafsirClip>,
        startIndex: Int,
        arabicPrefix: String,
        reciterName: String,
    ): List<Span> {
        val out = ArrayList<Span>()
        val from = startIndex.coerceAtLeast(0)
        for (i in from until clips.size) {
            val c = clips[i]
            for (a in c.start..c.end) {
                out += Span(c.surah, a, a, Kind.ARABIC, Highlight.PER_AYAH,
                    id = "free-ar-${c.surah}-$a", url = ayahAudioUrl(arabicPrefix, c.surah, a),
                    title = "Ayah $a", artist = reciterName)
            }
            out += Span(c.surah, c.start, c.end, Kind.TAFSIR, Highlight.GROUP,
                id = c.id, url = c.url, title = c.title, artist = c.artist)
        }
        return out
    }

    /** Index of the track to play for [ayah] (prefer Arabic, else the covering clip). */
    fun indexForAyah(queue: List<Span>, surah: Int, ayah: Int, prefer: Kind? = Kind.ARABIC): Int? {
        val hits = queue.indices.filter { i -> queue[i].surah == surah && queue[i].covers(ayah) }
        if (hits.isEmpty()) return null
        if (prefer != null) hits.firstOrNull { queue[it].kind == prefer }?.let { return it }
        return hits.first()
    }

    /** Next/previous ayah control (skips companion audio of the same ayah). */
    fun stepAyah(queue: List<Span>, index: Int, direction: Int): Int {
        if (queue.isEmpty()) return 0
        val i = index.coerceIn(0, queue.lastIndex)
        val cur = queue[i]
        val targetAyah = if (direction >= 0) cur.end + 1 else cur.start - 1
        val targetSurah = cur.surah
        indexForAyah(queue, targetSurah, targetAyah)?.let { return it }
        // crossed a surah boundary
        return if (direction >= 0) {
            queue.indices.firstOrNull { it > i && (queue[it].surah != targetSurah || queue[it].start >= targetAyah) } ?: i
        } else {
            queue.indices.lastOrNull { it < i && (queue[it].surah != targetSurah || queue[it].end <= targetAyah) } ?: i
        }
    }

    fun translationKeyFor(language: String, editionKey: String): Pair<String, String> = when {
        editionKey.contains("walk", ignoreCase = true) || editionKey.contains("sahih", ignoreCase = true) ->
            "en.sahih" to "Sahih International"
        language.equals("Urdu", true) || editionKey.startsWith("ur-") ->
            "ur.kanzuliman" to "Urdu translation (Kanz-ul-Iman)"
        language.equals("English", true) || editionKey.startsWith("en-") ->
            "en.sahih" to "Sahih International"
        else -> "en.ahmedraza" to "English translation"
    }
}
