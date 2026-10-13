package com.noorpro.app.prayer

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.noorpro.app.prayer.ongoing.NextPrayerOngoingUpdater
import com.noorpro.app.prayer.widget.PrayerWidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Safety net that runs about every 15 minutes regardless of which notification toggles are on:
 * re-fetches today's/tomorrow's timings when online, re-arms every prayer alarm and refreshes the
 * widgets + ongoing notification. This is what keeps times correct after date changes, Doze,
 * force-stops and OEM battery killers.
 */
class PrayerRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        PrayerScheduler.rescheduleAll(applicationContext, allowNetwork = true)
        NextPrayerOngoingUpdater.update(applicationContext)
        PrayerWidgetUpdater.refreshAll(applicationContext)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        private const val UNIQUE = "prayer_refresh_v1"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PrayerRefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
