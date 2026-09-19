package com.noorpro.app.prayer.ongoing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.noorpro.app.prayer.widget.PrayerWidgetUpdater
import java.util.concurrent.TimeUnit

class NextPrayerOngoingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val remainingMs = NextPrayerOngoingUpdater.update(applicationContext)
            PrayerWidgetUpdater.refreshAll(applicationContext)
            // Schedule a precise near-prayer refresh so the shade/widget advance promptly.
            NextPrayerOngoingScheduler.scheduleNearPrayer(applicationContext, remainingMs)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object NextPrayerOngoingScheduler {
    private const val UNIQUE_PERIODIC = "next_prayer_ongoing_v1"
    private const val UNIQUE_NEAR = "next_prayer_ongoing_near_v1"

    fun schedule(context: Context) {
        if (!NextPrayerOngoingUpdater.isEnabled(context)) {
            cancel(context)
            return
        }
        // 15 min periodic baseline (WorkManager minimum for PeriodicWork is 15 min).
        val request = PeriodicWorkRequestBuilder<NextPrayerOngoingWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        // Immediate kick so toggle / boot shows content without waiting.
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<NextPrayerOngoingWorker>().build()
        )
    }

    /**
     * One-shot refresh shortly before / at next prayer so countdown and "current"
     * highlight do not lag the 15-minute period.
     */
    fun scheduleNearPrayer(context: Context, remainingMs: Long) {
        if (!NextPrayerOngoingUpdater.isEnabled(context) || remainingMs <= 0L) return
        // Fire ~30s after the prayer time (or sooner if already close) plus a mid-window tick
        // when more than 20 minutes remain.
        val delayMs = when {
            remainingMs <= 60_000L -> remainingMs + 5_000L
            remainingMs <= 20 * 60_000L -> (remainingMs / 2).coerceAtLeast(60_000L)
            else -> remainingMs - 5 * 60_000L // 5 min before prayer
        }.coerceAtLeast(30_000L)

        val near = OneTimeWorkRequestBuilder<NextPrayerOngoingWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NEAR,
            ExistingWorkPolicy.REPLACE,
            near,
        )
    }

    fun cancel(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(UNIQUE_PERIODIC)
        wm.cancelUniqueWork(UNIQUE_NEAR)
    }
}
