package com.noorpro.app.audio.catalog

import android.content.Context
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.Track
import org.json.JSONObject

/**
 * 1.2.0 (117): Barakah Melodies nasheeds, licensed to NoorPro by Safar Media (full catalogue, free, for closed
 * testing and 6 months after launch). Terms from the licence agreement, built into the app:
 *  - Credit "Barakah Melodies" + the track title on every track ([credit]) and every Reel that uses one
 *    ([reelCredit]); the nasheed section links to the artist on Spotify ([SPOTIFY_URL]).
 *  - Reels may use a clip of up to 30 s ([REEL_CLIP_MAX_MS]); a Reel with Barakah audio is never exported or
 *    shared outside NoorPro with the audio ([canShareOutside]).
 *  - Streams in the app only: no downloadable files and no unencrypted cache ([OFFLINE_ALLOWED] = false until an
 *    encrypted in-app store exists). No ad plays during or over a Barakah track (see LicensedAudioGate).
 *  - Never sent to Noor AI or any AI pipeline: no transcription, training, voice imitation or generation ([aiMayProcess]).
 *  - The track list is REMOTE (Firebase Remote Config key [REMOTE_KEY]), never hard-coded, so any track can be
 *    removed within 7 days (Remote Config refreshes at least every 12 h; a catalogue not refreshed for
 *    [MAX_STALE_DAYS] days is hidden). Deleting the file from NoorPro storage removes it instantly.
 * Audio files come from Safar Media directly (never from Spotify or YouTube). Until they are delivered the
 * catalogue is empty and the section shows a "coming soon" state with the credit and Spotify link.
 */
object BarakahMelodies {
    const val ARTIST_ID = "barakah-melodies"
    const val ARTIST = "Barakah Melodies"
    const val LICENSOR = "Safar Media"
    const val CREDIT = "Barakah Melodies · licensed by Safar Media"
    const val SPOTIFY_URL = "https://open.spotify.com/artist/048kFRuX8hS3DJSbayEKWt"
    const val REEL_CLIP_MAX_MS = 30_000L
    const val ALLOW_EXTERNAL_SHARE = false
    const val OFFLINE_ALLOWED = false
    const val REMOTE_KEY = "barakah_catalog_json"
    const val MAX_STALE_DAYS = 7
    private const val PREFS = "barakah_catalog"
    const val TERMS = "Licensed to NoorPro by Safar Media. Streams inside NoorPro only — no downloads and no ads while a track plays. " +
        "A clip of up to 30 seconds may be used in your NoorPro Reels with the credit; those Reels can't be shared outside NoorPro."

    /** One delivered file from Safar Media (an https URL on NoorPro's own storage/CDN). */
    data class Entry(val key: String, val title: String, val audioUrl: String, val durationMs: Long = 0, val coverUrl: String? = null, val language: String? = null)

    /** A parsed remote catalogue. [removed] keys are dropped even if still listed. */
    data class Catalog(val version: Int, val entries: List<Entry>, val removed: Set<String>, val fetchedAtMs: Long) {
        companion object { val EMPTY = Catalog(0, emptyList(), emptySet(), 0L) }
    }

    @Volatile private var catalog: Catalog = Catalog.EMPTY
    /** Tests / previews can force a catalogue. */
    @Volatile var override: Catalog? = null
    /** Clock for staleness (tests). */
    @Volatile internal var now: () -> Long = { System.currentTimeMillis() }

    private val current: Catalog get() = override ?: catalog

    /** Live entries: from the remote catalogue, minus removed keys and unsafe sources, hidden when stale. */
    val entries: List<Entry> get() = current.let { c -> if (isStale(c)) emptyList() else c.entries.filter { it.key !in c.removed && isAllowedSource(it.audioUrl) } }

    val tracks: List<Track> get() = entries.map(::toTrack)

    val available: Boolean get() = tracks.isNotEmpty()

    fun isStale(c: Catalog): Boolean = c.fetchedAtMs > 0 && now() - c.fetchedAtMs > MAX_STALE_DAYS * 86_400_000L

    /**
     * JSON in Remote Config:
     * {"version":1,"tracks":[{"key":"zikr-of-the-heart","title":"Zikr of the Heart","url":"https://…","durationMs":215000,"cover":"https://…","lang":"en"}],"removed":["key"]}
     */
    fun parse(json: String?, fetchedAtMs: Long): Catalog? = runCatching {
        if (json.isNullOrBlank()) return null
        val o = JSONObject(json)
        val removed = o.optJSONArray("removed")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }.toSet() }.orEmpty()
        val arr = o.optJSONArray("tracks")
        val list = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
            val t = arr!!.optJSONObject(i) ?: return@mapNotNull null
            val key = t.optString("key").trim(); val title = t.optString("title").trim(); val url = t.optString("url").trim()
            if (key.isBlank() || title.isBlank() || !isAllowedSource(url)) null
            else Entry(key, title.take(80), url, t.optLong("durationMs", 0L), t.optString("cover").takeIf { it.startsWith("https://") }, t.optString("lang").ifBlank { null })
        }.distinctBy { it.key }
        Catalog(o.optInt("version", 1), list, removed, fetchedAtMs)
    }.getOrNull()

    /** Loads the last cached catalogue, then follows Remote Config (call before Reels2Flags.load()). */
    fun init(context: Context) {
        val app = context.applicationContext
        runCatching {
            val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            parse(p.getString("json", null), p.getLong("at", 0L))?.let { catalog = it }
        }
        runCatching { com.noorpro.app.ui.screens.Reels2Flags.onApplied { refresh(app) } }
    }

    private fun refresh(app: Context) {
        runCatching {
            if (!com.noorpro.app.data.FirebaseGate.ready) return
            val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val v = rc.getValue(REMOTE_KEY)
            if (v.source == com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return
            val at = rc.info.fetchTimeMillis.takeIf { it > 0 } ?: now()
            val parsed = parse(v.asString(), at) ?: return
            catalog = parsed
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("json", v.asString()).putLong("at", at).apply()
        }
    }

    fun toTrack(e: Entry) = Track(
        id = "bm-${e.key}", title = e.title, artistId = ARTIST_ID, artistName = ARTIST, audioUrl = e.audioUrl, durationMs = e.durationMs,
        coverUrl = e.coverUrl, license = LicenseType.PERMISSION_LETTER, attributionText = "$ARTIST · ${e.title} · licensed by $LICENSOR", sourceUrl = SPOTIFY_URL,
        tags = listOf("nasheed", "barakah-melodies", "no-share", "no-ai", "no-download", "no-ads"), language = e.language,
    )

    /** Only our own hosting: https, and never Spotify / YouTube / third-party rip sites. */
    fun isAllowedSource(url: String): Boolean {
        val u = url.trim().lowercase()
        if (!u.startsWith("https://")) return false
        val blocked = listOf("spotify.com", "scdn.co", "youtube.com", "youtu.be", "googlevideo.com", "mp3quran", "soundcloud.com")
        return blocked.none { it in u }
    }

    fun isBarakah(t: Track): Boolean = t.artistId == ARTIST_ID || t.id.startsWith("bm-")
    fun isBarakahId(trackId: String?): Boolean = trackId?.startsWith("bm-") == true

    /** A Barakah track removed from the remote catalogue (or a stale one) can no longer be played, even from a saved queue. */
    fun isPlayable(t: Track): Boolean = !isBarakah(t) || entries.any { "bm-${it.key}" == t.id }

    /** The clip length a Reel may use from this track (null = not limited by this licence). */
    fun reelClipLimitMs(t: Track): Long? = if (isBarakah(t)) REEL_CLIP_MAX_MS else null

    /** False for Barakah Melodies tracks: no export or sharing of the audio outside the app. */
    fun canShareOutside(t: Track): Boolean = !isBarakah(t) || ALLOW_EXTERNAL_SHARE
    fun canShareOutside(soundTrackId: String?): Boolean = !isBarakahId(soundTrackId) || ALLOW_EXTERNAL_SHARE

    /** No downloads / offline files for Barakah tracks (no encrypted store yet). */
    fun canDownload(t: Track): Boolean = !isBarakah(t) || OFFLINE_ALLOWED

    /** Never hand Barakah audio to Noor AI or any AI pipeline. */
    fun aiMayProcess(t: Track?): Boolean = t == null || !isBarakah(t)
    fun aiMayProcess(soundTrackId: String?): Boolean = !isBarakahId(soundTrackId)

    /** Credit line for a track: "Barakah Melodies · <title>" (shown on every row and the player). */
    fun credit(t: Track): String? = if (isBarakah(t)) "$ARTIST · ${t.title}" else null

    /** Credit printed on a Reel that uses a Barakah clip. */
    fun reelCredit(title: String): String = "♪ $ARTIST · $title (licensed by $LICENSOR)"
}
