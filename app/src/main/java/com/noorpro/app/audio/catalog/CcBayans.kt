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

    // 1.1.9: all Creative Commons bayans removed (owner request: no CC content).
    val channels: List<BayanChannel> = emptyList()

    fun channel(key: String): BayanChannel = channels.first { it.key == key }

    val all: List<CcBayan> = emptyList()

    fun byChannel(key: String): List<CcBayan> = all.filter { it.channelKey == key }
}
