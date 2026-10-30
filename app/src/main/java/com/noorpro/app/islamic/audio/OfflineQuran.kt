package com.noorpro.app.islamic.audio

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.noorpro.app.audio.QuranAudioCache
import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.QuranMetaData
import com.noorpro.app.data.Reciter
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.community.LocalNotice
import java.io.File
import java.util.Locale

/* 1.1.6 Offline Quran manager (quran_offline_enabled). Builds on QuranAudioCache: same folders, same
 * file names, so anything saved here is what the existing player already prefers over streaming. */

/** What a reciter's source allows the app to do with its files. */
enum class OfflinePolicy {
    /** Files may be kept privately on this device for personal offline listening (never re-shared). */
    PERSONAL_CACHE,
    /** Streaming only: download buttons are hidden and the reason is shown. */
    STREAM_ONLY,
}

data class ReciterLicense(val policy: OfflinePolicy, val source: String, val note: String)

data class ReciterStorage(val reciter: Reciter, val bytes: Long, val completeSurahs: Set<Int>, val partialSurahs: Set<Int>) {
    val surahCount get() = completeSurahs.size
}

/** A named set of surahs the user can save in one tap. */
data class DownloadPack(val id: String, val title: String, val subtitle: String, val surahs: List<Int>)

object OfflineQuranLogic {
    const val TOTAL_AYAHS = 6236
    private const val AVG_SECONDS_PER_AYAH = 8.7

    /**
     * Every bundled reciter streams from EveryAyah.com, which publishes these recitations for free
     * listening; NoorPro already offered per-surah saving before 1.1.6. Files stay in app-private
     * storage, are never exported/shared, and are removed on uninstall. A reciter can be switched
     * to STREAM_ONLY here if a source asks for that.
     */
    fun license(r: Reciter): ReciterLicense = when {
        r.prefixUrl.contains("everyayah.com") -> ReciterLicense(OfflinePolicy.PERSONAL_CACHE, "EveryAyah.com",
            "Free recitation audio. Saved privately on this phone for your own listening; never shared from the app.")
        else -> ReciterLicense(OfflinePolicy.STREAM_ONLY, "Unknown source", "Streaming only until the source's terms allow offline copies.")
    }

    fun canDownload(r: Reciter) = license(r).policy == OfflinePolicy.PERSONAL_CACHE

    val PACKS = listOf(
        DownloadPack("juz30", "Juz 'Amma", "Surahs 78–114 · daily salah", (78..114).toList()),
        DownloadPack("juz29", "Juz Tabarak", "Surahs 67–77", (67..77).toList()),
        DownloadPack("sunnah", "Weekly sunnah surahs", "Al-Kahf, Yasin, Ar-Rahman, Al-Waqi'ah, Al-Mulk", listOf(18, 36, 55, 56, 67)),
        DownloadPack("all", "Complete Quran", "All 114 surahs", (1..114).toList()),
    )

    fun ayahs(surah: Int) = IslamicData.surahAyahsCount[surah] ?: 0
    fun ayahs(surahs: Collection<Int>) = surahs.sumOf { ayahs(it) }

    /** "Husary_128kbps" → 128; defaults to 128 when the folder has no bitrate. */
    fun kbps(r: Reciter): Int = Regex("(\\d{2,3})kbps").find(r.prefixUrl)?.groupValues?.get(1)?.toIntOrNull() ?: 128

    /** Rough size before downloading (mujawwad recitations are about 1.8× longer). */
    fun estimateBytes(r: Reciter, surahs: Collection<Int>): Long {
        val slow = if (r.prefixUrl.contains("Mujawwad", ignoreCase = true)) 1.8 else 1.0
        return (ayahs(surahs) * AVG_SECONDS_PER_AYAH * slow * kbps(r) * 125).toLong()
    }

    fun formatBytes(b: Long): String = when {
        b <= 0 -> "0 MB"
        b < 1_000_000 -> "${(b / 1000).coerceAtLeast(1)} KB"
        b < 1_000_000_000 -> String.format(Locale.US, "%.0f MB", b / 1e6)
        else -> String.format(Locale.US, "%.1f GB", b / 1e9)
    }

    /** Surahs from [pack] that still need downloading. */
    fun missing(pack: List<Int>, complete: Set<Int>) = pack.filter { it !in complete }

    /** Parses SSSAAA.mp3 names into surah → saved ayah count. */
    fun countByFile(names: List<String>): Map<Int, Int> = names.mapNotNull { n ->
        Regex("^(\\d{3})(\\d{3})\\.mp3$").find(n)?.groupValues?.get(1)?.toIntOrNull()
    }.groupingBy { it }.eachCount()

    fun classify(counts: Map<Int, Int>): Pair<Set<Int>, Set<Int>> {
        val complete = counts.filter { (s, c) -> ayahs(s) in 1..c }.keys
        val partial = counts.keys - complete
        return complete to partial
    }

    /** "Mahmoud Khaleel Al-Husary" → "Al-Husary"; "Abdul Basit (Murattal)" → "Abdul Basit · Murattal". */
    fun shortName(r: Reciter): String = if (r.name.contains("(")) r.name.replace(Regex("\\s*\\((.+)\\)"), " · $1") else r.name.substringAfterLast(' ')

    fun surahName(id: Int) = QuranMetaData.surahNamesEn.getOrElse(id - 1) { "Surah $id" }

    fun enoughSpace(needBytes: Long, freeBytes: Long, reserve: Long = 200_000_000L) = freeBytes - needBytes >= reserve
}

/** Reads what is saved on disk. Call off the main thread. */
object OfflineQuranStore {
    fun storage(ctx: Context, r: Reciter): ReciterStorage {
        val dir = QuranAudioCache.reciterDir(ctx, r.prefixUrl)
        val files = dir.listFiles()?.filter { it.isFile && it.length() > 0 && it.name.endsWith(".mp3") }.orEmpty()
        val (complete, partial) = OfflineQuranLogic.classify(OfflineQuranLogic.countByFile(files.map { it.name }))
        return ReciterStorage(r, files.sumOf { it.length() }, complete, partial)
    }

    fun all(ctx: Context) = IslamicData.reciters.map { storage(ctx, it) }
    fun freeBytes(ctx: Context): Long = runCatching { File(ctx.filesDir.absolutePath).usableSpace }.getOrDefault(0L)
}

class OfflinePrefs(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("quran_offline", Context.MODE_PRIVATE)
    var wifiOnly: Boolean
        get() = p.getBoolean("wifi_only", true)
        set(v) { p.edit().putBoolean("wifi_only", v).apply() }
}

/** Queues surah downloads for one reciter through WorkManager so they survive leaving the screen. */
object OfflineDownloads {
    const val NOTIFICATION_ID = 74_610
    fun workName(r: Reciter) = "quran_offline_${r.id}"

    fun enqueue(ctx: Context, r: Reciter, surahs: List<Int>, wifiOnly: Boolean) {
        if (!OfflineQuranLogic.canDownload(r) || surahs.isEmpty()) return
        val req = OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
            .setInputData(workDataOf("reciter" to r.id, "surahs" to surahs.joinToString(",")))
            .addTag("quran_offline").build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(workName(r), ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    fun cancel(ctx: Context, r: Reciter) = WorkManager.getInstance(ctx).cancelUniqueWork(workName(r))
}

class OfflineDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val r = IslamicData.reciters.firstOrNull { it.id == inputData.getInt("reciter", -1) } ?: return Result.failure()
        if (!OfflineQuranLogic.canDownload(r)) return Result.failure()
        val surahs = inputData.getString("surahs").orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..114 }
        val total = OfflineQuranLogic.ayahs(surahs).coerceAtLeast(1)
        var done = 0
        var failed = 0
        for (s in surahs) {
            if (isStopped) return Result.success()
            val count = OfflineQuranLogic.ayahs(s)
            val ok = QuranAudioCache.downloadSurah(applicationContext, r.prefixUrl, s, count) { f ->
                setProgressAsync(workDataOf("surah" to s, "fraction" to (done + f * count) / total))
            }
            if (!ok) failed++
            done += count
        }
        if (IslamicFlags.isOn(IslamicFlags.QURAN_OFFLINE)) {
            val body = if (failed == 0) "${surahs.size} surahs by ${r.name} are ready to play offline."
            else "${surahs.size - failed} of ${surahs.size} surahs saved. Open Offline Quran to retry the rest."
            LocalNotice.post(applicationContext, OfflineDownloads.NOTIFICATION_ID, "Offline Quran", body, "QURAN_OFFLINE")
        }
        return if (failed == 0 || runAttemptCount >= 2) Result.success() else Result.retry()
    }
}

/** Listening tools shared by the Quran player: ayah repeat counts and sleep timer choices. */
object ListeningLogic {
    /** 1 = play once, 0 = repeat forever. */
    val REPEAT_CHOICES = listOf(1, 3, 5, 10, 0)
    fun repeatLabel(n: Int) = when (n) { 0 -> "∞"; 1 -> "Off"; else -> "${n}×" }

    /** Returns true while the same ayah should play again after finishing play number [played] (1-based). */
    fun shouldRepeat(times: Int, played: Int) = times == 0 || played < times

    /** Minutes; -1 means "at the end of this surah". */
    val SLEEP_CHOICES = listOf(15, 30, 45, 60, -1)
    fun sleepLabel(m: Int) = if (m == -1) "End of surah" else "$m min"

    fun remaining(endsAt: Long?, now: Long): String? {
        if (endsAt == null) return null
        val s = ((endsAt - now) / 1000).coerceAtLeast(0)
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }
}
