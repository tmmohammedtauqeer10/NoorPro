package com.noorpro.app.islamic.audio

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.noorpro.app.audio.podcast.Episode
import com.noorpro.app.audio.podcast.Podcast
import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.Reciter
import com.noorpro.app.islamic.IslamicFlags
import java.util.Locale

/* 1.1.6 Car mode (car_mode_enabled): a large-button in-app driving screen plus an Android Auto media
 * browser ([com.noorpro.app.audio.session.NoorCarLibraryService]). The browse tree below is plain data so
 * it is unit-tested without Media3; the service only turns nodes into MediaItems. */

/** One entry in the Android Auto browse tree. */
data class CarNode(val id: String, val title: String, val subtitle: String, val playable: Boolean)

sealed class CarTarget {
    data class Surah(val surah: Int) : CarTarget()
    data class Routine(val id: String) : CarTarget()
    data class Episode(val podcastId: String, val trackId: String) : CarTarget()
}

object CarModeLogic {
    const val ROOT = "car_root"
    const val QURAN = "car_quran"
    const val ROUTINES = "car_routines"
    const val BAYANS = "car_bayans"
    const val CONTINUE = "car_continue"
    private const val SURAH = "car_surah:"
    private const val ROUTINE = "car_routine:"
    private const val SHOW = "car_show:"
    private const val EP = "car_ep:"

    /** Sunnah / most-asked surahs offered first while driving. */
    val QUICK_SURAHS = listOf(18, 36, 55, 56, 67, 1)
    /** Android Auto shows short lists best; keep every level small. */
    const val MAX_ITEMS = 40

    fun surahId(n: Int) = "$SURAH$n"
    fun routineId(id: String) = "$ROUTINE$id"
    fun showId(id: String) = "$SHOW$id"
    fun episodeId(podcastId: String, trackId: String) = "$EP$podcastId|$trackId"

    fun parse(mediaId: String): CarTarget? = when {
        mediaId.startsWith(SURAH) -> mediaId.removePrefix(SURAH).toIntOrNull()?.takeIf { it in 1..114 }?.let { CarTarget.Surah(it) }
        mediaId.startsWith(ROUTINE) -> mediaId.removePrefix(ROUTINE).takeIf { it.isNotBlank() }?.let { CarTarget.Routine(it) }
        mediaId.startsWith(EP) -> mediaId.removePrefix(EP).split('|', limit = 2).takeIf { it.size == 2 && it.all(String::isNotBlank) }?.let { CarTarget.Episode(it[0], it[1]) }
        else -> null
    }

    fun showIdOf(mediaId: String): String? = mediaId.takeIf { it.startsWith(SHOW) }?.removePrefix(SHOW)

    fun root(bayansOn: Boolean, routines: Int, inProgress: Int): List<CarNode> = buildList {
        if (bayansOn && inProgress > 0) add(CarNode(CONTINUE, "Continue listening", "$inProgress in progress", false))
        add(CarNode(QURAN, "Quran", "Recitation · saved surahs play offline", false))
        if (routines > 0) add(CarNode(ROUTINES, "My routines", "$routines saved", false))
        if (bayansOn) add(CarNode(BAYANS, "Bayans & lectures", "Publisher podcasts", false))
    }

    /** Saved surahs first (they play without signal), then the sunnah set, then Juz 'Amma. */
    fun quran(saved: Set<Int>): List<CarNode> {
        val order = (saved.sorted() + QUICK_SURAHS + (78..114)).distinct().take(MAX_ITEMS)
        return order.map { n ->
            CarNode(surahId(n), "$n. ${OfflineQuranLogic.surahName(n)}", if (n in saved) "Saved · plays offline" else "${OfflineQuranLogic.ayahs(n)} ayahs · streams", true)
        }
    }

    fun routines(list: List<AudioRoutine>): List<CarNode> =
        list.take(MAX_ITEMS).map { CarNode(routineId(it.id), it.title, RoutineLogic.surahsLabel(it), true) }

    fun shows(shows: List<Podcast>, cachedCounts: Map<String, Int>): List<CarNode> =
        shows.filter { (cachedCounts[it.id] ?: 0) > 0 }.take(MAX_ITEMS).map { CarNode(showId(it.id), it.name, "${it.publisher} · ${it.language}", false) }

    fun episodes(p: Podcast, eps: List<Episode>, resume: Map<String, ResumePoint>): List<CarNode> = eps.take(20).map { e ->
        val tid = BayanLogic.trackId(p, e)
        val r = resume[tid]
        val sub = if (r != null && r.positionMs >= BayanLogic.MIN_RESUME_MS) BayanLogic.remainingLabel(r.positionMs, r.durationMs.takeIf { it > 0 } ?: e.durationMs)
            else e.durationMs.takeIf { it > 0 }?.let { "${it / 60_000} min" } ?: p.name
        CarNode(episodeId(p.id, tid), e.title, sub, true)
    }

    fun continueNodes(points: List<ResumePoint>): List<CarNode> = BayanLogic.inProgress(points).take(10).map {
        CarNode(episodeId(it.podcastId, it.trackId), it.title, BayanLogic.remainingLabel(it.positionMs, it.durationMs), true)
    }

    /** Reciter for the car: the one with most saved surahs (works without signal), else the app default. */
    fun reciter(storage: List<ReciterStorage>): Reciter =
        storage.filter { it.surahCount > 0 }.maxByOrNull { it.surahCount }?.reciter
            ?: IslamicData.reciters.firstOrNull { it.id == IslamicData.DEFAULT_RECITER_ID } ?: IslamicData.reciters.first()

    /** Per-ayah sources for a surah: the saved file when present, otherwise the reciter's stream. */
    fun ayahSources(prefixUrl: String, surah: Int, cached: (Int) -> String?): List<String> =
        (1..OfflineQuranLogic.ayahs(surah)).map { a -> cached(a) ?: prefixUrl + String.format(Locale.US, "%03d%03d.mp3", surah, a) }

    // ---------------------------------------------------------------- in-app car screen
    /** "Good evening" style greeting for the big header (local hour). */
    fun greeting(hour: Int) = when (hour) { in 4..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..20 -> "Good evening"; else -> "Safe journey" }

    /** Minimum touch target for every car-mode control (Android Auto guidance is ≥ 76dp). */
    const val MIN_TARGET_DP = 76
}

/** Android Auto discovery follows the flag: the library service component is disabled unless car mode is on. */
object CarComponents {
    private const val SERVICE = "com.noorpro.app.audio.session.NoorCarLibraryService"

    fun sync(ctx: Context) = runCatching {
        val on = IslamicFlags.isOn(IslamicFlags.CAR_MODE)
        val cn = ComponentName(ctx.packageName, SERVICE)
        val pm = ctx.packageManager
        val want = if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        if (pm.getComponentEnabledSetting(cn) != want) pm.setComponentEnabledSetting(cn, want, PackageManager.DONT_KILL_APP)
    }
}
