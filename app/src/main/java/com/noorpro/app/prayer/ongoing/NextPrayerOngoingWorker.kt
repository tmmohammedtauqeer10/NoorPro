package com.noorpro.app.prayer.ongoing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
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
            NextPrayerOngoingUpdater.update(applicationContext)
            PrayerWidgetUpdater.refreshAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object NextPrayerOngoingScheduler {
    private const val UNIQUE = "next_prayer_ongoing_v1"

    fun schedule(context: Context) {
        if (!NextPrayerOngoingUpdater.isEnabled(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE)
            return
        }
        val request = PeriodicWorkRequestBuilder<NextPrayerOngoingWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        // Kick an immediate one-shot via the same worker class through a short delay enqueue
        WorkManager.getInstance(context).enqueue(
            androidx.work.OneTimeWorkRequestBuilder<NextPrayerOngoingWorker>().build()
        )
    }
}
