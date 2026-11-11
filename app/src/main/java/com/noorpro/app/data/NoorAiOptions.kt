package com.noorpro.app.data

/**
 * 1.2.0 (114): Noor AI answer settings. Pure Kotlin (no Android types) so plain JVM tests can use it.
 *  - madhhab: fiqh answers follow the chosen school (default Hanafi) or compare all four. Aqidah is always
 *    Ash'ari/Maturidi and the path is Ahl as-Sunnah wal-Jama'ah with Tasawwuf, whatever school is chosen.
 *  - tier: Fast (quickest accurate route), Balanced (default) or Research (multi-step, more sources, citations).
 */
object NoorAiOptions {
    const val HANAFI = "hanafi"
    const val SHAFII = "shafii"
    const val MALIKI = "maliki"
    const val HANBALI = "hanbali"
    const val COMPARE = "compare"

    const val FAST = "fast"
    const val BALANCED = "balanced"
    const val RESEARCH = "research"

    data class Choice(val code: String, val label: String, val detail: String)

    val MADHHABS = listOf(
        Choice(HANAFI, "Hanafi", "Imam Abu Hanifa · South Asia, Turkey, Central Asia"),
        Choice(SHAFII, "Shafi'i", "Imam al-Shafi'i · Kerala, Konkan, Egypt, Yemen, SE Asia"),
        Choice(MALIKI, "Maliki", "Imam Malik · North & West Africa"),
        Choice(HANBALI, "Hanbali", "Imam Ahmad · parts of Arabia"),
        Choice(COMPARE, "All four · compare", "Shows each madhhab's view side by side"),
    )

    val TIERS = listOf(
        Choice(FAST, "Fast", "Quick, short answers"),
        Choice(BALANCED, "Balanced", "Detailed answers · default"),
        Choice(RESEARCH, "Research", "Deep, multi-step answer with more sources and citations (slower)"),
    )

    /** Accepts loose spellings ("Shafi", "shafi'i", "all") and falls back to Hanafi. */
    fun normMadhhab(raw: String?): String {
        val m = raw.orEmpty().lowercase().replace(Regex("[^a-z]"), "")
        return when {
            m.startsWith("shaf") -> SHAFII
            m.startsWith("mali") -> MALIKI
            m.startsWith("hanb") -> HANBALI
            m == "compare" || m == "all" || m == "allfour" || m == "four" -> COMPARE
            else -> HANAFI
        }
    }

    fun normTier(raw: String?): String = when (raw.orEmpty().lowercase().trim()) {
        FAST, "quick" -> FAST
        RESEARCH, "deep" -> RESEARCH
        else -> BALANCED
    }

    fun madhhabLabel(code: String): String = MADHHABS.firstOrNull { it.code == normMadhhab(code) }?.label ?: "Hanafi"
    fun tierLabel(code: String): String = TIERS.firstOrNull { it.code == normTier(code) }?.label ?: "Balanced"

    /** Research is only used for real study questions (not App Help / Dua drafting). */
    fun effectiveTier(tier: String, mode: NoorAiMode): String =
        if (normTier(tier) == RESEARCH && (mode == NoorAiMode.AppGuide || mode == NoorAiMode.DuaGenerator)) BALANCED else normTier(tier)

    /** Payload of one Server-Sent Events line ("data: {...}"), or null for comments / blank lines. */
    fun sseData(line: String): String? {
        val l = line.trimEnd('\r')
        if (!l.startsWith("data:")) return null
        return l.removePrefix("data:").trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Splits an answer into chunks for cloud speech: sentence boundaries (also ۔ । ؟ ! ?), each at most [max]
     * characters, small first chunk so playback starts quickly. Markdown, [K#] tags and the Sources list are removed.
     */
    fun speechChunks(text: String, max: Int = 450, first: Int = 220): List<String> {
        val body = text.substringBefore("\nSources:").substringBefore("\nFOLLOWUPS:").substringBefore("\nNOORMETA:")
            .replace(Regex("\\[K\\d+]"), "").replace(Regex("[*_#>`|]"), "").replace(Regex("\\s+"), " ").trim()
        if (body.isEmpty()) return emptyList()
        val sentences = Regex("[^.!?؟۔।\\n]+[.!?؟۔।]*").findAll(body).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()
        val out = mutableListOf<StringBuilder>()
        for (s in sentences.flatMap { hardSplit(it, max) }) {
            val limit = if (out.size <= 1) first.coerceAtMost(max) else max
            if (out.isEmpty() || out.last().length + s.length + 1 > limit) out += StringBuilder(s)
            else out.last().append(' ').append(s)
        }
        return out.map { it.toString() }
    }

    private fun hardSplit(s: String, max: Int): List<String> {
        if (s.length <= max) return listOf(s)
        val words = s.split(' ')
        val out = mutableListOf(StringBuilder())
        for (w in words) {
            if (out.last().length + w.length + 1 > max && out.last().isNotEmpty()) out += StringBuilder()
            if (out.last().isNotEmpty()) out.last().append(' ')
            out.last().append(w.take(max))
        }
        return out.map { it.toString() }.filter { it.isNotBlank() }
    }
}
