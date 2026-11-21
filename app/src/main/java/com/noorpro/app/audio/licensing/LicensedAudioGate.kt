package com.noorpro.app.audio.licensing

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.noorpro.app.audio.catalog.BarakahMelodies
import com.noorpro.app.audio.models.Track
import java.util.Calendar
import kotlinx.coroutines.tasks.await

/**
 * 1.2.0 (117): live licence rules for the Al-Noor player (Barakah Melodies / Safar Media agreement).
 *  - [barakahPlaying] is Compose state: every ad surface (InlineAdCard, SponsoredAdMedia) renders nothing while it
 *    is true, so no ad plays during or over a Barakah track.
 *  - Each new play of a Barakah track is counted once ([onPlayer]) for the quarterly report ([BarakahStats]).
 */
object LicensedAudioGate {
    var barakahPlaying by mutableStateOf(false)
        private set
    private var lastCounted: String? = null

    /** Ads are allowed only when no licensed-no-ads track is playing. */
    val adsAllowed: Boolean get() = !barakahPlaying

    /**
     * Called by AlNoorPlayer whenever playback or the current track changes. Returns the track id counted as a new
     * play (null if none), so the caller can record it.
     */
    fun onPlayer(track: Track?, audible: Boolean): String? {
        val bm = track != null && BarakahMelodies.isBarakah(track)
        barakahPlaying = bm && audible
        if (!audible || track == null) { if (track == null || !bm) lastCounted = null; return null }
        if (!bm) { lastCounted = null; return null }
        if (lastCounted == track.id) return null
        lastCounted = track.id
        return track.id
    }

    internal fun resetForTests() { barakahPlaying = false; lastCounted = null }
}

/**
 * Quarterly reporting hook for Safar Media: Listener count (users who played at least one Barakah track in a
 * month) and per-track play counts. Writes, best effort and only when signed in:
 *   licensed_audio_stats/barakah-melodies/months/{yyyy-MM}               { plays: { <trackKey>: n }, total: n }
 *   licensed_audio_stats/barakah-melodies/months/{yyyy-MM}/listeners/{uid} { at }   (one doc per listener)
 * Needs the additive Firestore rule in docs/CONTENT_1.2.0_117.md (not deployed yet); until then writes fail silently.
 * No audio, no listening history beyond these counters, and nothing about the track is sent to any AI.
 */
object BarakahStats {
    const val ROOT = "licensed_audio_stats"
    const val CATALOG = "barakah-melodies"

    fun monthKey(c: Calendar): String = "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    fun monthKey(year: Int, month: Int): String = "%04d-%02d".format(year, month)

    /** Months of a calendar quarter (q = 1..4), e.g. 2027 Q1 → 2027-01, 2027-02, 2027-03. */
    fun quarterMonths(year: Int, q: Int): List<String> { require(q in 1..4); return (1..3).map { monthKey(year, (q - 1) * 3 + it) } }

    fun trackKey(trackId: String): String = trackId.removePrefix("bm-")

    data class MonthReport(val month: String, val listeners: Long, val plays: Map<String, Long>) { val total: Long get() = plays.values.sum() }

    /** CSV for the quarterly email to Safar Media: month,listeners,track,plays. */
    fun toCsv(reports: List<MonthReport>): String = buildString {
        append("month,listeners,track,plays\n")
        reports.forEach { r ->
            if (r.plays.isEmpty()) append("${r.month},${r.listeners},,0\n")
            r.plays.toSortedMap().forEach { (k, v) -> append("${r.month},${r.listeners},${k.replace(",", " ")},$v\n") }
        }
    }

    /** Local per-month tally (on this device only), kept for diagnostics and to avoid duplicate listener writes. */
    fun recordPlay(context: Context, trackId: String, cal: Calendar = Calendar.getInstance()) {
        val month = monthKey(cal); val key = trackKey(trackId)
        val p = context.applicationContext.getSharedPreferences("barakah_stats", Context.MODE_PRIVATE)
        val firstThisMonth = p.getString("listener_month", null) != month
        val e = p.edit().putInt("$month:$key", p.getInt("$month:$key", 0) + 1)
        if (firstThisMonth) e.putString("listener_month", month)
        e.apply()
        runCatching {
            if (!com.noorpro.app.data.FirebaseGate.ready) return
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val doc = db.collection(ROOT).document(CATALOG).collection("months").document(month)
            val inc = com.google.firebase.firestore.FieldValue.increment(1)
            doc.set(mapOf("plays" to mapOf(key to inc), "total" to inc), com.google.firebase.firestore.SetOptions.merge())
            if (firstThisMonth) doc.collection("listeners").document(uid).set(mapOf("at" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
        }
    }

    /** Admin hook: one month's numbers (listeners via a count() aggregate). Admin-only read in the proposed rules. */
    suspend fun monthReport(month: String): MonthReport? = runCatching {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val doc = db.collection(ROOT).document(CATALOG).collection("months").document(month)
        val snap = doc.get().await()
        @Suppress("UNCHECKED_CAST")
        val plays = (snap.get("plays") as? Map<String, Any?>).orEmpty().mapValues { (it.value as? Number)?.toLong() ?: 0L }
        val agg = doc.collection("listeners").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await()
        MonthReport(month, agg.count, plays)
    }.getOrNull()

    /** Admin hook: the quarter as CSV (null if any month could not be read). */
    suspend fun quarterCsv(year: Int, q: Int): String? {
        val reports = quarterMonths(year, q).map { monthReport(it) ?: return null }
        return toCsv(reports)
    }
}
