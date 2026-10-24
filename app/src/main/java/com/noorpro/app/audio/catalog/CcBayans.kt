package com.noorpro.app.audio.catalog

/**
 * 1.0.95: hand-picked bayans that the channels themselves publish under YouTube's
 * "Creative Commons Attribution license (reuse allowed)" (CC BY). Each video's licence was checked
 * one by one on its YouTube watch page on 24 Oct 2026 (re-checked for 1.0.95, all 24 still CC BY + embeddable).
 *
 * Audio-first switch: if an owner later grants reuse of an OFFICIAL audio file for an item, fill in
 * [CcBayan.officialAudio] (URL + licence + evidence). That item then plays in NoorPro's normal audio
 * player (background play, media notification) with the same credit and source link, and the
 * YouTube player is no longer used for it. Never point it at audio taken from YouTube.
 *
 * Playback: only YouTube's official embedded player, which stays visible while it plays. We never
 * download, rip or extract audio. Every item shows the channel credit, links to the original video
 * and has a "Subscribe on YouTube" button. Thumbnails come from YouTube's official i.ytimg.com URLs.
 *
 * Curation rule: calm, non-polemical topics only (seerat, akhlaq, namaz, Ramadan, tawbah,
 * parents, Qur'an, Auliya). Leave out anything that attacks other groups or scholars, and anything
 * political. To add a video, check that its watch page shows the CC BY licence first.
 */
data class BayanChannel(
    val key: String,
    val name: String,
    /** Credit line shown on every item. */
    val credit: String,
    val channelUrl: String,
    val speaker: String,
    val keywords: List<String> = emptyList(),
) {
    val subscribeUrl: String get() = "$channelUrl?sub_confirmation=1"
}

/** Owner-published audio for a bayan, only when the owner's licence/permission allows reuse. */
data class BayanAudio(
    val url: String,
    val licence: com.noorpro.app.audio.models.LicenseType,
    /** Short licence label shown in the credit line, e.g. "CC BY" or "Used with permission". */
    val licenceLabel: String,
    /** Where the permission/licence is documented (licence page, permission letter reference). */
    val evidence: String,
    val licenceUrl: String? = null,
)

data class CcBayan(
    val videoId: String,
    val title: String,
    val channelKey: String,
    val topic: String,
    val durationSec: Int,
    val language: String = "Urdu",
    val keywords: List<String> = emptyList(),
    /** Audio-first: set only with owner permission; null = YouTube's official embedded player. */
    val officialAudio: BayanAudio? = null,
) {
    val channel: BayanChannel get() = CcBayans.channel(channelKey)
    val isAudioFirst: Boolean get() = officialAudio?.let { it.url.startsWith("https://") && it.evidence.isNotBlank() } == true
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$videoId"
    val thumbUrl: String get() = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    val durationText: String get() {
        val h = durationSec / 3600; val m = (durationSec % 3600) / 60; val s = durationSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
    val licence: String get() = if (isAudioFirst) officialAudio!!.licenceLabel else "CC BY"
    /** Credit line shown on the item. */
    val creditLine: String get() = "${channel.name} • $licence"

    /** Track for NoorPro's audio player (audio-first items only). */
    fun toTrack(): com.noorpro.app.audio.models.Track? {
        val a = officialAudio?.takeIf { isAudioFirst } ?: return null
        return com.noorpro.app.audio.models.Track(
            id = "bayan-$videoId", title = title, artistId = "bayan-${channel.key}", artistName = channel.speaker,
            audioUrl = a.url, durationMs = durationSec * 1000L, coverUrl = thumbUrl, license = a.licence,
            attributionText = creditLine, sourceUrl = watchUrl, licenseUrl = a.licenceUrl,
            tags = listOf("bayan", topic), language = language,
        )
    }
    /** Search fields: title, scholar/channel names, topic, keywords. */
    val searchFields: List<String> get() =
        listOf(title, channel.speaker, channel.name, topic, "bayan", "bayans", "speech", language) + channel.keywords + keywords
}

object CcBayans {
    const val VERIFIED_ON = "2026-10-24"

    val channels: List<BayanChannel> = listOf(
        BayanChannel(
            key = "jalali", name = "Dr Muhammad Ashraf Asif Jalali",
            credit = "Dr Muhammad Ashraf Asif Jalali • Official channel • CC BY",
            channelUrl = "https://www.youtube.com/@TheDrJalali", speaker = "Dr Muhammad Ashraf Asif Jalali",
            keywords = listOf("Jalali", "Dr Jalali", "Ashraf Asif Jalali", "Ashraf Jalali", "Asif Jalali", "ڈاکٹر محمد اشرف آصف جلالی", "اشرف آصف جلالی", "جلالی"),
        ),
        BayanChannel(
            key = "madani", name = "Madani Channel (Dawat-e-Islami)",
            credit = "Madani Channel (Dawat-e-Islami) • CC BY",
            channelUrl = "https://www.youtube.com/@MadaniChannelOfficial", speaker = "Maulana Ilyas Qadri",
            keywords = listOf("Madani Channel", "Madani", "Dawat-e-Islami", "Dawateislami", "Dawat e Islami", "Ilyas Qadri", "Ilyas Attar Qadri",
                "Ameer-e-Ahle Sunnat", "مدنی چینل", "دعوت اسلامی", "الیاس قادری"),
        ),
    )

    fun channel(key: String): BayanChannel = channels.first { it.key == key }

    val all: List<CcBayan> = listOf(
        // Dr Muhammad Ashraf Asif Jalali — youtube.com/@TheDrJalali (UCiQoMsK_mCObTvPjuKBcPzg)
        CcBayan("9bKDpu61zu8", "Seerat-un-Nabi ﷺ · Episode 1", "jalali", "Seerat", 2971, keywords = listOf("seerat", "sirat", "seerah", "prophet", "mehboob e khuda")),
        CcBayan("8zrNDJfP_xo", "Seerat-un-Nabi ﷺ · Episode 2", "jalali", "Seerat", 3410, keywords = listOf("seerat", "sirat", "seerah", "prophet")),
        CcBayan("bloh5NIXCJY", "Seerat-un-Nabi ﷺ · Episode 3", "jalali", "Seerat", 3374, keywords = listOf("seerat", "sirat", "seerah", "prophet")),
        CcBayan("sKTtpeeYytU", "Seerat-un-Nabi ﷺ · Episode 5", "jalali", "Seerat", 3414, keywords = listOf("seerat", "sirat", "seerah", "prophet")),
        CcBayan("DNzogqzIIY0", "Fazilat-e-Ramzan ul Mubarak", "jalali", "Ramadan", 1626, keywords = listOf("ramzan", "ramadan", "fazilat", "sahaba")),
        CcBayan("9tCZoyTMraw", "Nawazishat-e-Ramadan", "jalali", "Ramadan", 644, keywords = listOf("ramzan", "ramadan", "blessings")),
        CcBayan("cQ7V1--VZOk", "Imtiazat-e-Namaz · Fahm-e-Deen Lecture 4", "jalali", "Namaz", 4433, keywords = listOf("namaz", "salah", "salat", "prayer", "fahm e deen")),
        CcBayan("NfB4KV-Lxlw", "Walideen ki wafat ke baad 5 haqooq", "jalali", "Parents", 387, keywords = listOf("walidain", "walideen", "parents", "isal e sawab", "haqooq")),
        CcBayan("GbPX9fnVNfI", "Al-Nabi ut-Tauba Conference 2025", "jalali", "Tawbah", 4298, keywords = listOf("tauba", "tawbah", "toba", "repentance", "istighfar")),
        CcBayan("U3c4xzxB_9k", "Khatam-e-Quran ki dastan", "jalali", "Quran", 501, keywords = listOf("quran", "khatam", "hafiz", "hifz")),
        CcBayan("d5SL180aTPc", "Roza-e-Rasool ﷺ par hazri ke adaab", "jalali", "Akhlaq", 1656, keywords = listOf("adab", "adaab", "madina", "akhlaq", "ziyarat")),
        CcBayan("Ckq0QjWNKmM", "Maqam-e-Ibrahim aur Hajr-e-Aswad ki shan", "jalali", "Hajj", 292, keywords = listOf("hajj", "umrah", "kaaba", "maqam e ibrahim", "hajr e aswad")),

        // Madani Channel (Dawat-e-Islami) — youtube.com/@MadaniChannelOfficial (UCfSmrkM_OGTFMR4kkFxrNDQ)
        CcBayan("-3NlJrfVrFQ", "Sudharnay ka tareeqa", "madani", "Tawbah", 3706, keywords = listOf("islah", "tauba", "tawbah", "self improvement")),
        CcBayan("0Xq86kmFqH8", "Companionship of pious people · Madani Guldasta", "madani", "Auliya", 312, keywords = listOf("auliya", "pious", "suhbat", "saleheen", "madani guldasta")),
        CcBayan("09OFX69SKwU", "Jannati mehal ka soda", "madani", "Akhlaq", 2843, keywords = listOf("jannat", "akhlaq", "charity")),
        CcBayan("woqcR5d6GcI", "Nafs kisey kehtay hain", "madani", "Akhlaq", 3270, keywords = listOf("nafs", "akhlaq", "self")),
        CcBayan("QCMSWt0Qoe0", "Shaitan ke 4 kamiyab waar", "madani", "Tawbah", 3698, keywords = listOf("shaitan", "tauba", "tawbah", "english subtitles")),
        CcBayan("rNMyuigf0oo", "Istiqbal-e-Ramadan", "madani", "Ramadan", 2894, keywords = listOf("ramzan", "ramadan", "istiqbal")),
        CcBayan("AOIl8U9jWUE", "Fazail-e-Lailatul Qadr · Part 1", "madani", "Ramadan", 2386, keywords = listOf("laylatul qadr", "lailatul qadr", "shab e qadr", "ramzan", "ramadan")),
        CcBayan("-bs-0gJuBa0", "Meraj-un-Nabi ﷺ · Madani Guldasta", "madani", "Seerat", 495, keywords = listOf("meraj", "miraj", "seerat", "prophet")),
        CcBayan("eZVC5dyQEMo", "Muaf karnay ke fazail", "madani", "Akhlaq", 3022, keywords = listOf("forgiveness", "maaf", "muaf", "akhlaq", "english subtitles")),
        CcBayan("xhpPTRk5hCU", "Ikhlas · Madani Muzakra", "madani", "Akhlaq", 3521, keywords = listOf("ikhlas", "sincerity", "madani muzakra", "akhlaq")),
        CcBayan("CzK_W-j6RRM", "Namaz-e-Janaza ka tareeqa", "madani", "Namaz", 3336, keywords = listOf("namaz", "janaza", "salah", "funeral prayer")),
        CcBayan("YmjzhMTGTM0", "Aulad ki tarbiyat · Madani Muzakra", "madani", "Parents", 262, keywords = listOf("aulad", "children", "tarbiyat", "parents", "walidain")),
    )

    fun byChannel(key: String): List<CcBayan> = all.filter { it.channelKey == key }
}
