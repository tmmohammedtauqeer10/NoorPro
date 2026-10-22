package com.noorpro.app.audio.catalog

import android.content.Context
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import com.noorpro.app.data.QuranMetaData

/**
 * Free, legally usable Dua / Tarjuma / Tafsir audio for the Al-Noor Audio screen (1.0.68).
 * Streaming only — every URL was verified (HTTP 200, audio/mpeg) before shipping.
 * Each source records its license / terms URL; see [sources] and docs/AL_NOOR_AUDIO.md.
 */
object FreeAudioCatalog {

    data class AudioSource(
        val key: String,
        val name: String,
        val by: String,
        val license: LicenseType,
        val licenseLabel: String,
        val licenseUrl: String,
        val sourceUrl: String,
    )

    data class Group(val title: String, val subtitle: String, val tracks: List<Track>)

    /** A Quran translation edition; tracks are built per surah on demand. */
    data class Edition(
        val key: String,
        val language: String,
        val title: String,
        val subtitle: String,
        val verseByVerse: Boolean,
        val source: AudioSource,
        private val urlFor: (surah: Int, globalAyah: Int) -> String,
    ) {
        fun surahTracks(surah: Int): List<Track> {
            val name = QuranMetaData.surahNamesEn.getOrElse(surah - 1) { "Surah $surah" }
            if (!verseByVerse) return listOf(
                track("$key-$surah", "$surah. $name", title, urlFor(surah, 0), source, language),
            )
            val first = AYAH_COUNTS.take(surah - 1).sum()
            return (1..AYAH_COUNTS[surah - 1]).map { a ->
                track("$key-$surah-$a", "$name · Ayah $a", title, urlFor(surah, first + a), source, language)
            }
        }
    }

    data class Scholar(val name: String, val language: String, val source: AudioSource, val surahs: List<Group>)

    private const val ISLAMIC_NETWORK_TERMS = "https://alquran.cloud/terms-and-conditions"
    private const val PD_MARK = "https://creativecommons.org/publicdomain/mark/1.0/"

    val islamicNetwork = AudioSource(
        "islamic_network", "Islamic Network CDN (AlQuran Cloud)", "Reciters & their estates via Islamic Network",
        LicenseType.SOURCE_TERMS, "Free streaming permitted by source terms (non-commercial redistribution; copyright remains with reciters)",
        ISLAMIC_NETWORK_TERMS, "https://alquran.cloud/cdn",
    )
    val hisn = AudioSource(
        "hisn_duraihim", "Hisn al-Muslim Azkar — Internet Archive", "Hamad Al-Duraihim",
        LicenseType.PUBLIC_DOMAIN, "Public Domain Mark 1.0", PD_MARK, "https://archive.org/details/mp3-smart-muzakir",
    )
    val quranicDuas = AudioSource(
        "quranic_duas", "Quranic Duas (Arabic + Urdu, Kanzul Eemaan) — Internet Archive", "Hajveri Apps",
        LicenseType.PUBLIC_DOMAIN, "Public Domain Mark 1.0", PD_MARK, "https://archive.org/details/quranic-duas",
    )
    val ubaid = AudioSource(
        "ubaid", "Tafseer-ul-Quran (Urdu) — Internet Archive", "Dr. Ubaid ur Rahman (ISLAMFORT / MIRC)",
        LicenseType.CC_BY, "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/",
        "https://archive.org/search?query=uploader%3Awebsites%40mirc.org.pk",
    )
    val wahid = AudioSource(
        "wahiduddin", "English Commentary on Al-Baqarah — Internet Archive", "Maulana Wahiduddin Khan",
        LicenseType.PUBLIC_DOMAIN, "Public Domain Mark 1.0", PD_MARK, "https://archive.org/details/Eng_Commentary_AlBaqarah1-286",
    )

    val sources = listOf(hisn, quranicDuas, islamicNetwork, ubaid, wahid)

    private fun track(id: String, title: String, artist: String, url: String, src: AudioSource, lang: String?, durMs: Long = 0L) = Track(
        id = "free-$id", title = title, artistId = src.key, artistName = artist, audioUrl = url, durationMs = durMs,
        license = src.license, attributionText = "${src.by} · ${src.licenseLabel}", sourceUrl = src.sourceUrl,
        licenseUrl = src.licenseUrl, language = lang,
    )

    private fun cdnSurah(edition: String) = { s: Int, _: Int -> "https://cdn.islamic.network/quran/audio-surah/128/$edition/$s.mp3" }
    private fun cdnAyah(bitrate: Int, edition: String) = { _: Int, g: Int -> "https://cdn.islamic.network/quran/audio/$bitrate/$edition/$g.mp3" }

    /** Tarjuma editions, sorted by language. Hindi / Bengali: no clearly licensed source found yet. */
    val editions = listOf(
        Edition("ur-khan", "Urdu", "Shamshad Ali Khan", "Urdu translation · verse by verse", true, islamicNetwork, cdnAyah(64, "ur.khan")),
        Edition("en-walk", "English", "Ibrahim Walk", "Saheeh International · verse by verse", true, islamicNetwork, cdnAyah(192, "en.walk")),
        Edition("en-alafasy-walk", "English", "Alafasy + Ibrahim Walk", "Arabic with Saheeh International · full surah", false, islamicNetwork,
            cdnSurah("en.misharyrashidalafasyenglishtranslationsaheehibrahimwalk")),
        Edition("en-ayyub-waters", "English", "Muhammad Ayyub + Mikaal Waters", "Arabic with Muhsin Khan · full surah", false, islamicNetwork,
            cdnSurah("en.muhammadayyubenglishtranslationmuhsinkhanmikaalwaters")),
        Edition("en-sudais-pickthall", "English", "Sudais & Shuraym + Aslam Athar", "Arabic with Pickthall · full surah", false, islamicNetwork,
            cdnSurah("en.sudaisandshuraymenglishtranslationpickthallaslamathar")),
        Edition("en-sudais-naeem", "English", "Sudais & Shuraym + Naeem Sultan", "Arabic with English (Naeem Sultan) · full surah", false, islamicNetwork,
            cdnSurah("ar.sudaisshuraymnaeemsultan")),
    )
    val tarjumaLanguages = listOf("Urdu", "English")
    const val UNAVAILABLE_LANGUAGES_NOTE = "Hindi, Bengali and other languages will be added once a clearly licensed audio source is available."

    private var rows: List<List<String>>? = null
    private fun rows(ctx: Context): List<List<String>> = rows ?: runCatching {
        ctx.applicationContext.assets.open("al_noor_free_audio.txt").bufferedReader(Charsets.UTF_8).readLines()
            .filter { it.isNotBlank() }.map { it.split('|') }
    }.getOrDefault(emptyList()).also { rows = it }

    /** Duas grouped by occasion, in a fixed daily order. */
    fun duaGroups(ctx: Context): List<Group> {
        val r = rows(ctx).filter { it[0] == "H" || it[0] == "Q" }
        return r.groupBy { it[1] }.map { (occasion, items) ->
            val src = if (items.first()[0] == "H") hisn else quranicDuas
            Group(occasion, "${items.size} duas · ${src.by}", items.mapIndexed { i, it ->
                track("dua-${it[0]}-$occasion-$i", if (it[3].isNotBlank()) it[3] + " …" else it[2], src.by, it[4], src, if (it[0] == "Q") "ar,ur" else "ar", it[5].toLongOrNull() ?: 0L)
            })
        }
    }

    /** Tafsir by scholar, then surah, then part (by starting ayah). */
    fun scholars(ctx: Context): List<Scholar> {
        val r = rows(ctx)
        fun build(tag: String, src: AudioSource, lang: String): List<Group> = r.filter { it[0] == tag }
            .groupBy { it[1].toInt() }.toSortedMap().map { (s, parts) ->
                val name = QuranMetaData.surahNamesEn.getOrElse(s - 1) { "Surah $s" }
                val sorted = parts.sortedBy { it[3].toIntOrNull() ?: 0 }
                Group("$s. $name", "${sorted.size} parts", sorted.mapIndexed { i, it ->
                    track("tafsir-$tag-$s-$i", "$name · ${it[2]}", src.by, it[4], src, lang, it[5].toLongOrNull() ?: 0L)
                })
            }
        return listOf(
            Scholar("Dr. Ubaid ur Rahman", "Urdu", ubaid, build("U", ubaid, "ur")),
            Scholar("Maulana Wahiduddin Khan", "English", wahid, build("W", wahid, "en")),
        )
    }

    val AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
    ).toList()
}
