package com.noorpro.app.islamic.audio

import android.content.Context
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.podcast.Episode
import com.noorpro.app.audio.podcast.EpisodeChapter
import com.noorpro.app.audio.podcast.Podcast
import com.noorpro.app.audio.podcast.PodcastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/* 1.1.6 Bayan podcasts (bayan_podcasts_enabled). Builds on the existing Al-Noor Audio podcast reader
 * (PodcastRepository): the same publisher RSS feeds, streamed from the publisher's own enclosure URL and
 * never rehosted. Adds: a bayan/lecture-only shelf, per-episode resume, chapters (show-note timestamps,
 * Podlove inline chapters or Podcasting 2.0 JSON) and a hard exclusion list for sources that asked not
 * to be hosted (Idara Islah-e-Muashra: audio bayans are not permissible — link-out only, never here). */

/** Saved listening position for one episode (keyed by the Al-Noor track id "pod-<show>-<hash>"). */
data class ResumePoint(
    val trackId: String,
    val podcastId: String,
    val title: String,
    val audioUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val artwork: String? = null,
)

object BayanLogic {
    /** Only sources whose licence allows in-app streaming. Publisher RSS = the publisher's own public podcast feed. */
    val ALLOWED_LICENSES = setOf(LicenseType.PUBLISHER_RSS)

    /**
     * Never shown as podcasts, whatever a feed is called. Idara Islah-e-Muashra (Muhammad Ajmal Raza Qadri)
     * told us audio bayans are not permissible; they keep their official-channel link-out card only.
     */
    val EXCLUDED_TERMS = listOf("islahemuashra", "islahimuashra", "islahemuashara", "ajmalrazaqadri", "ajmalraza")

    private fun squash(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    fun isExcluded(p: Podcast): Boolean {
        val hay = squash(listOf(p.id, p.name, p.publisher, p.feedUrl, p.website, p.note).joinToString(" "))
        return EXCLUDED_TERMS.any { hay.contains(it) }
    }

    /** Talk/lecture categories that belong on the bayan shelf (nasheeds and recitation-only feeds stay in Al-Noor Audio). */
    val BAYAN_CATEGORIES = setOf("Bayan", "Ahl-e-Sunnat (Barelvi) Bayan", "Tafsir", "Seerah", "Lectures", "Reminders", "Spirituality", "General", "Quran")

    fun shows(all: List<Podcast> = PodcastRepository.podcasts): List<Podcast> =
        all.filter { it.kind == "podcast" && it.category in BAYAN_CATEGORIES && !isExcluded(it) && !isRecitationOnly(it) }

    /** Quran-translation recitation feeds are not talks. */
    private fun isRecitationOnly(p: Podcast) = p.id.startsWith("qc-") || p.id.startsWith("hindi-") || p.name.contains("translation", true) && p.category == "Quran"

    fun languages(shows: List<Podcast>): List<String> = shows.map { it.language }.distinct()

    fun filter(shows: List<Podcast>, language: String?, query: String): List<Podcast> {
        val q = squash(query)
        return shows.filter { (language == null || it.language == language) && (q.isEmpty() || squash("${it.name} ${it.publisher} ${it.category}").contains(q)) }
    }

    fun trackId(p: Podcast, e: Episode) = "pod-${p.id}-${e.guid.hashCode()}"

    // ---------------------------------------------------------------- resume
    const val MIN_RESUME_MS = 30_000L
    const val FINISHED_TAIL_MS = 60_000L
    const val REWIND_MS = 5_000L

    fun isFinished(pos: Long, dur: Long) = dur > 0 && pos >= dur - FINISHED_TAIL_MS

    /** Where playback should start: a few seconds before the saved point, or 0 when barely started / finished. */
    fun resumeFrom(pos: Long, dur: Long): Long = when {
        pos < MIN_RESUME_MS -> 0L
        isFinished(pos, dur) -> 0L
        else -> (pos - REWIND_MS).coerceAtLeast(0L)
    }

    fun progress(pos: Long, dur: Long): Float = if (dur <= 0) 0f else (pos.toFloat() / dur).coerceIn(0f, 1f)

    fun remainingLabel(pos: Long, dur: Long): String = when {
        dur <= 0 -> "Resume at ${clock(pos)}"
        isFinished(pos, dur) -> "Finished"
        else -> "${((dur - pos) / 60_000L).coerceAtLeast(1)} min left"
    }

    fun inProgress(points: List<ResumePoint>): List<ResumePoint> =
        points.filter { it.positionMs >= MIN_RESUME_MS && !isFinished(it.positionMs, it.durationMs) }.sortedByDescending { it.updatedAt }

    // ---------------------------------------------------------------- chapters
    private val STAMP = Regex("^[\\[(]?((?:\\d{1,2}:)?\\d{1,2}:\\d{2})[\\])]?\\s*[-–—:.|)]?\\s*(.{2,})$")

    fun parseClock(s: String): Long? {
        val parts = s.trim().split(':').map { it.toIntOrNull() ?: return null }
        if (parts.size !in 2..3 || parts.drop(1).any { it > 59 }) return null
        return parts.fold(0L) { acc, v -> acc * 60 + v } * 1000
    }

    /** "00:00 Intro / 12:30 Patience…" lines in show notes → chapters. Needs ≥2 ascending stamps. */
    fun chaptersFromNotes(notes: String, durationMs: Long = 0L): List<EpisodeChapter> {
        val out = ArrayList<EpisodeChapter>()
        for (line in notes.lines()) {
            val m = STAMP.find(line.trim()) ?: continue
            val ms = parseClock(m.groupValues[1]) ?: continue
            if (durationMs > 0 && ms >= durationMs) continue
            if (out.isNotEmpty() && ms <= out.last().startMs) continue
            out += EpisodeChapter(ms, m.groupValues[2].trim().trimEnd('.', ',').take(80))
        }
        return if (out.size >= 2) out else emptyList()
    }

    /** Podcasting 2.0 chapters JSON: {"chapters":[{"startTime":0,"title":"…"}]}. Hidden (toc=false) entries skipped. */
    fun chaptersFromJson(json: String): List<EpisodeChapter> = runCatching {
        val arr = JSONObject(json).optJSONArray("chapters") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val t = o.optString("title").trim()
            if (t.isEmpty() || (o.has("toc") && !o.optBoolean("toc", true))) null else EpisodeChapter((o.optDouble("startTime", 0.0) * 1000).toLong(), t)
        }.sortedBy { it.startMs }
    }.getOrDefault(emptyList())

    /** Best chapters an episode offers without the network: inline Podlove first, then show-note stamps. */
    fun offlineChapters(e: Episode): List<EpisodeChapter> = e.chapters.ifEmpty { chaptersFromNotes(e.notes, e.durationMs) }

    fun chapterAt(chapters: List<EpisodeChapter>, posMs: Long): Int = chapters.indexOfLast { it.startMs <= posMs }

    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
    }

    // ---------------------------------------------------------------- json
    fun toJson(r: ResumePoint): JSONObject = JSONObject().put("id", r.trackId).put("p", r.podcastId).put("t", r.title).put("u", r.audioUrl)
        .put("pos", r.positionMs).put("dur", r.durationMs).put("at", r.updatedAt).put("art", r.artwork ?: "")

    fun fromJson(o: JSONObject) = ResumePoint(o.getString("id"), o.optString("p"), o.optString("t"), o.optString("u"),
        o.optLong("pos"), o.optLong("dur"), o.optLong("at"), o.optString("art").ifBlank { null })
}

/** Local, private resume positions (prefs "bayan_resume"); newest 60 kept. */
class BayanResumeStore(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("bayan_resume", Context.MODE_PRIVATE)

    fun all(): List<ResumePoint> = runCatching {
        val a = JSONArray(p.getString("points", "[]"))
        (0 until a.length()).map { BayanLogic.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList()).sortedByDescending { it.updatedAt }

    fun get(trackId: String) = all().firstOrNull { it.trackId == trackId }

    fun save(r: ResumePoint) {
        val list = (listOf(r) + all().filterNot { it.trackId == r.trackId }).take(60)
        p.edit().putString("points", JSONArray().apply { list.forEach { put(BayanLogic.toJson(it)) } }.toString()).apply()
    }

    fun remove(trackId: String) {
        val list = all().filterNot { it.trackId == trackId }
        p.edit().putString("points", JSONArray().apply { list.forEach { put(BayanLogic.toJson(it)) } }.toString()).apply()
    }
}

/** Podcasting 2.0 chapter JSON, fetched once per episode when it is opened (memory cache). */
object BayanChapters {
    private val mem = HashMap<String, List<EpisodeChapter>>()
    @Volatile var preview: Map<String, List<EpisodeChapter>>? = null

    suspend fun load(e: Episode): List<EpisodeChapter> {
        preview?.let { return it[e.guid] ?: BayanLogic.offlineChapters(e) }
        BayanLogic.offlineChapters(e).takeIf { it.isNotEmpty() }?.let { return it }
        val url = e.chaptersUrl ?: return emptyList()
        synchronized(mem) { mem[url] }?.let { return it }
        val list = withContext(Dispatchers.IO) {
            runCatching {
                val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 15_000; setRequestProperty("User-Agent", "NoorPro/1.1 (podcast chapters)") }
                try { if (c.responseCode == 200) BayanLogic.chaptersFromJson(c.inputStream.bufferedReader().readText()) else emptyList() } finally { c.disconnect() }
            }.getOrDefault(emptyList())
        }
        synchronized(mem) { mem[url] = list }
        return list
    }
}

/**
 * Saves the position of whichever podcast episode the Al-Noor player is on, every few seconds while it
 * plays and on pause. Started from the Bayans screen and Car mode; idempotent.
 */
object BayanResumeTracker {
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start(ctx: Context) {
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        runCatching { AlNoorAudioSession.init(app) }.onFailure { return }
        val store = BayanResumeStore(app)
        val player = AlNoorAudioSession.player
        var lastSaved = 0L
        job = scope.launch {
            player.playback.collectLatest { pb ->
                val t = player.queue.value.currentTrack ?: return@collectLatest
                if (!t.id.startsWith("pod-")) return@collectLatest
                val now = System.currentTimeMillis()
                if (pb.isPlaying && now - lastSaved < 5_000) return@collectLatest
                if (pb.positionMs <= 0) return@collectLatest
                lastSaved = now
                val podId = t.artistId.removePrefix("podcast-")
                store.save(ResumePoint(t.id, podId, t.title, t.audioUrl, pb.positionMs, pb.durationMs.takeIf { it > 0 } ?: t.durationMs, now, t.coverUrl))
            }
        }
    }
}
