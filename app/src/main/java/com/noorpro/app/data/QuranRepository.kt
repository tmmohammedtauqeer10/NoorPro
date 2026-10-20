package com.noorpro.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QuranRepository(private val context: Context) {
    private val db by lazy { AppDatabase.getDatabase(context) }
    private val quranDao by lazy { db.quranDao() }
    @Volatile private var indoPakAyahCache: Map<Int, Map<Int, String>>? = null
    @Volatile private var bundledSeeded = false
    private val seedMutex = kotlinx.coroutines.sync.Mutex()

    /** Cleans Uthmani ayah text (e.g. strips the leading Bismillah); set by the ViewModel. */
    var arabicCleaner: (String, Int, Int) -> String = { text, _, _ -> text.replace("\uFEFF", "").trim() }
    var htmlUnescaper: (String) -> String = { it }

    /**
     * Seeds Room from the bundled compressed Quran (Arabic + Urdu + English) so that reading
     * never needs the network. Network fetches remain only as a fallback if this fails.
     */
    suspend fun ensureBundledQuranSeeded(): Boolean = withContext(Dispatchers.IO) {
        if (bundledSeeded) return@withContext true
        seedMutex.lock()
        try {
            if (bundledSeeded) return@withContext true
            val headers = quranDao.getSurahHeaders()
            val complete = headers.size == 114 && headers.sumOf { it.versesCount } >= OfflineQuranAssets.TOTAL_AYAHS
            if (!complete) {
                val core = OfflineQuranAssets.loadCore(context)
                val verses = core.ayahs.map { a ->
                    val arabic = arabicCleaner(a.arabic, a.surah, a.ayah)
                    val names = core.surahNames[a.surah - 1]
                    QuranVerse(
                        id = a.surah * 1000 + a.ayah,
                        jozz = a.juz,
                        sora = a.surah,
                        soraNameEn = names.first,
                        soraNameAr = names.second,
                        page = a.page,
                        lineStart = 1,
                        lineEnd = 1,
                        ayaNo = a.ayah,
                        ayaText = arabic,
                        ayaTextEmlaey = arabic,
                        maanyAya = "معاني الكلمات",
                        earabQuran = "إعراب الآية",
                        reasonsOfVerses = "سبب النزول",
                        tafseerSaadi = htmlUnescaper(a.urdu),
                        tafseerMoysar = htmlUnescaper(a.english),
                        tafseerBughiu = "تفسير البغوي",
                        ayaTextTashkil = arabic
                    )
                }
                verses.chunked(500).forEach { quranDao.insertVerses(it) }
            }
            bundledSeeded = true
            true
        } catch (e: Exception) {
            com.noorpro.app.utils.CrashReporter.report(e)
            false
        } finally {
            seedMutex.unlock()
        }
    }

    suspend fun getSurahList(
        translationKey: String,
        translationManager: TranslationManager
    ): List<Surah> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Surah>()
        for (i in 1..114) {
            val soraId = i
            val realTotalVerses = IslamicData.surahAyahsCount[soraId] ?: 0
            
            val existingSurah = IslamicData.surahs.find { it.id == soraId }
            val englishName = existingSurah?.nameEnglish ?: QuranMetaData.surahNamesEn.getOrNull(i - 1) ?: "Surah $i"
            val arabicName = existingSurah?.nameArabic ?: QuranMetaData.surahNamesAr.getOrNull(i - 1) ?: "سورة $i"
            val translation = existingSurah?.englishTranslation ?: QuranMetaData.surahTranslations.getOrNull(i - 1) ?: "Translation"
            val type = existingSurah?.type ?: QuranMetaData.surahTypes.getOrNull(i - 1) ?: "Meccan"
            
            result.add(Surah(
                id = soraId,
                nameEnglish = englishName,
                nameArabic = arabicName,
                englishTranslation = translation,
                versesCount = realTotalVerses,
                type = type,
                verses = emptyList() // Lazy load verses
            ))
        }
        result
    }

    suspend fun searchQuran(query: String): List<QuranVerse> = withContext(Dispatchers.IO) {
        ensureBundledQuranSeeded()
        quranDao.searchQuran(query)
    }

    suspend fun getVersesForSurah(surahId: Int): List<QuranVerse> = withContext(Dispatchers.IO) {
        ensureBundledQuranSeeded()
        quranDao.getVersesForSurah(surahId)
    }

    suspend fun insertVerses(verses: List<QuranVerse>) = withContext(Dispatchers.IO) {
        quranDao.insertVerses(verses)
    }

    suspend fun deleteVersesForSurah(surahId: Int) = withContext(Dispatchers.IO) {
        quranDao.deleteVersesForSurah(surahId)
    }

    suspend fun deleteVersesForJuz(juzId: Int) = withContext(Dispatchers.IO) {
        quranDao.deleteVersesForJuz(juzId)
    }

    suspend fun getVersesForJuz(juzId: Int): List<QuranVerse> = withContext(Dispatchers.IO) {
        ensureBundledQuranSeeded()
        quranDao.getVersesForJuz(juzId)
    }

    suspend fun getIndoPakAyahsForSurah(surahId: Int): Map<Int, String> = withContext(Dispatchers.IO) {
        loadIndoPakAyahs()[surahId].orEmpty()
    }

    suspend fun getIndoPakAyahsForVerses(verses: List<QuranVerse>): Map<Pair<Int, Int>, String> = withContext(Dispatchers.IO) {
        if (verses.isEmpty()) return@withContext emptyMap()
        val cache = loadIndoPakAyahs()
        verses.mapNotNull { verse ->
            cache[verse.sora]?.get(verse.ayaNo)?.let { text -> (verse.sora to verse.ayaNo) to text }
        }.toMap()
    }

    private fun loadIndoPakAyahs(): Map<Int, Map<Int, String>> {
        indoPakAyahCache?.let { return it }
        return synchronized(this) {
            indoPakAyahCache ?: readIndoPakAyahsFromAssets().also { indoPakAyahCache = it }
        }
    }

    private fun readIndoPakAyahsFromAssets(): Map<Int, Map<Int, String>> {
        val bySurah = mutableMapOf<Int, MutableMap<Int, MutableList<Pair<Int, String>>>>()
        return try {
            OfflineQuranAssets.forEachIndoPakWord(context) { surah, ayah, word, rawText ->
                val text = normalizeIndoPakWord(rawText)
                if (text.isNotBlank() && !text.isAyahMarker()) {
                    bySurah
                        .getOrPut(surah) { mutableMapOf() }
                        .getOrPut(ayah) { mutableListOf() }
                        .add(word to text)
                }
            }
            bySurah.mapValues { (_, ayahs) ->
                ayahs.mapValues { (_, words) ->
                    words.sortedBy { it.first }.joinToString(" ") { it.second }
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun normalizeIndoPakWord(value: String): String =
        value.replace("\u200E", "")
            .replace("\u200F", "")
            .trim()

    private fun String.isAyahMarker(): Boolean =
        isNotBlank() && all { ch ->
            ch in '\u0660'..'\u0669' || ch in '\u06F0'..'\u06F9'
        }

    fun getSurahType(id: Int): String {
        // List of Medinan surahs by standard Islamic consensus
        val medinanSet = setOf(
            2, 3, 4, 5, 8, 9, 22, 24, 33, 47, 48, 49, 57, 58, 59, 
            60, 61, 62, 63, 64, 65, 66, 76, 98, 110
        )
        return if (medinanSet.contains(id)) "Medinan" else "Meccan"
    }

    fun getTitleTranslation(id: Int): String {
        val translations = mapOf(
            1 to "The Opening", 2 to "The Cow", 3 to "Family of Imran", 4 to "The Women", 5 to "The Table Spread",
            6 to "The Cattle", 7 to "The Heights", 8 to "The Spoils of War", 9 to "The Repentance", 10 to "Jonah",
            11 to "Hud", 12 to "Joseph", 13 to "The Thunder", 14 to "Abraham", 15 to "The Rocky Tract",
            16 to "The Bee", 17 to "The Night Journey", 18 to "The Cave", 19 to "Mary", 20 to "Ta-Ha",
            21 to "The Prophets", 22 to "The Pilgrimage", 23 to "The Believers", 24 to "The Light", 25 to "The Criterion",
            26 to "The Poets", 27 to "The Ant", 28 to "The Stories", 29 to "The Spider", 30 to "The Romans",
            31 to "Luqman", 32 to "The Prostration", 33 to "The Combined Forces", 34 to "Sheba", 35 to "Originator",
            36 to "Ya Seen", 37 to "Those who set the Ranks", 38 to "The Letter Sad", 39 to "The Troops", 40 to "The Forgiver",
            41 to "Explained in Detail", 42 to "The Consultation", 43 to "The Ornaments of Gold", 44 to "The Smoke", 45 to "The Crouching",
            46 to "The Wind-Curved Sandhills", 47 to "Muhammad", 48 to "The Victory", 49 to "The Rooms", 50 to "The Letter Qaf",
            51 to "The Winnowing Winds", 52 to "The Mount", 53 to "The Star", 54 to "The Moon", 55 to "The Beneficent",
            56 to "The Inevitable", 57 to "The Iron", 58 to "The Pleading Woman", 59 to "The Exile", 60 to "She that is to be examined",
            61 to "The Ranks", 62 to "The Congregation", 63 to "The Hypocrites", 64 to "The Mutual Disillusion", 65 to "The Divorce",
            66 to "The Prohibition", 67 to "The Sovereignty", 68 to "The Pen", 69 to "The Reality", 70 to "The Ascending Stairways",
            71 to "Noah", 72 to "The Jinn", 73 to "The Enshrouded One", 74 to "The Cloaked One", 75 to "The Resurrection",
            76 to "Man", 77 to "The Emissaries", 78 to "The Announcement", 79 to "Those who drag forth", 80 to "He Frowned",
            81 to "The Overthrowing", 82 to "The Cleaving", 83 to "The Defrauders", 84 to "The Sundering", 85 to "The Mansions of the Stars",
            86 to "The Nightcomer", 87 to "The Most High", 88 to "The Overwhelming", 89 to "The Dawn", 90 to "The City",
            91 to "The Sun", 92 to "The Night", 93 to "The Morning Hours", 94 to "The Relief", 95 to "The Fig",
            96 to "The Clot", 97 to "The Power", 98 to "The Clear Proof", 99 to "The Earthquake", 100 to "The Courser",
            101 to "The Calamity", 102 to "The Rivalry in World Increase", 103 to "The Declining Day", 104 to "The Traducer", 105 to "The Elephant",
            106 to "Quraysh", 107 to "The Small Kindnesses", 108 to "The Abundance", 109 to "The Disbelievers", 110 to "The Divine Support",
            111 to "The Palm Fiber", 112 to "The Sincerity", 113 to "The Daybreak", 114 to "Mankind"
        )
        return translations[id] ?: "Holy Surah"
    }
}
