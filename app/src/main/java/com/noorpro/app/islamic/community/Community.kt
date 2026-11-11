package com.noorpro.app.islamic.community

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestoreException
import com.noorpro.app.MainActivity
import com.noorpro.app.data.halal.HalalTextFilter
import com.noorpro.app.notify.NoorNotify
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import java.util.concurrent.TimeUnit

/**
 * Shared plumbing for the 1.1.6 community features (dua requests, halaqa rooms, Ask a Scholar,
 * challenges): calm error states, the existing Halal Guard text filter, the existing report
 * pipeline (ummah_reports → Moderator panel) and local notifications/reminders.
 */
enum class CommunityError(val title: String, val body: String) {
    OFFLINE_BACKEND("Coming soon", "This part of the Ummah isn't switched on yet. Please check back soon."),
    SIGN_IN("Sign in to continue", "Posting and joining need a NoorPro account so the community stays safe."),
    NETWORK("You're offline", "We'll refresh when you're back online."),
    BLOCKED_TEXT("Please edit your words", "This breaks the Halal community guidelines."),
    NOT_FOUND("Not found", "This may have been removed."),
    FULL("This is full", "There's no space left right now."),
    UNKNOWN("Something went wrong", "Please try again in a moment.");

    companion object {
        fun from(t: Throwable): CommunityError {
            generateSequence(t) { it.cause }.firstOrNull { it is CommunityException }?.let { return (it as CommunityException).error }
            val f = generateSequence(t) { it.cause }.firstOrNull { it is FirebaseFirestoreException } as? FirebaseFirestoreException
            return when (f?.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> OFFLINE_BACKEND
                FirebaseFirestoreException.Code.UNAVAILABLE -> NETWORK
                FirebaseFirestoreException.Code.NOT_FOUND -> NOT_FOUND
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> SIGN_IN
                else -> UNKNOWN
            }
        }
    }
}

class CommunityException(val error: CommunityError, val detail: String? = null) : Exception(detail ?: error.title)

/** Result of running user text through Halal Guard before it is posted. */
data class CleanText(val text: String, val review: Boolean, val message: String)

object CommunityText {
    /** Trims, caps length and applies the on-device Halal Guard filter: severe → throws, mild → masked. */
    fun prepare(raw: String, max: Int): CleanText {
        val t = raw.replace(Regex("\\s+\\n"), "\n").replace(Regex("[ \\t]+"), " ").trim().take(max)
        val r = HalalTextFilter.check(t)
        return when (r.action) {
            HalalTextFilter.Action.BLOCK -> throw CommunityException(CommunityError.BLOCKED_TEXT, r.message)
            HalalTextFilter.Action.MASK -> CleanText(HalalTextFilter.mask(t, r), false, r.message)
            HalalTextFilter.Action.QUEUE -> CleanText(t, true, r.message)
            HalalTextFilter.Action.ALLOW -> CleanText(t, false, "")
        }
    }
    /** Live hint under a text field (empty when fine). */
    fun hint(raw: String): String = if (raw.isBlank()) "" else HalalTextFilter.check(raw).message

    fun ago(ms: Long, now: Long = System.currentTimeMillis()): String {
        if (ms <= 0) return "just now"
        val m = (now - ms) / 60_000
        return when {
            m < 1 -> "just now"
            m < 60 -> "${m}m"
            m < 60 * 24 -> "${m / 60}h"
            m < 60 * 24 * 7 -> "${m / (60 * 24)}d"
            else -> "${m / (60 * 24 * 7)}w"
        }
    }

    /** Shown for anonymous posts; never derived from the uid. */
    const val ANON_NAME = "A brother or sister"
}

/** Existing Halal Guard report pipeline (ummah_reports, reviewed in the Moderator panel). */
object CommunityReports {
    val REASONS = listOf("Inappropriate or offensive", "Spam or scam", "Sectarian or hateful", "Private information", "Something else")
    fun report(type: String, targetId: String, targetUid: String, reason: String, details: String = "", onResult: (Boolean) -> Unit) =
        com.noorpro.app.data.UmmahRepository().reportContent(type, targetId, targetUid, reason, details, onResult)
}

/** Local notifications on the existing "Daily ayah & dua" channel; tapping opens [screen]. */
object LocalNotice {
    fun intent(ctx: Context, requestCode: Int, screen: String): PendingIntent =
        PendingIntent.getActivity(ctx, requestCode, Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", screen)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    @android.annotation.SuppressLint("MissingPermission")
    fun post(ctx: Context, id: Int, title: String, body: String, screen: String) {
        runCatching {
            PrayerNotificationChannels.ensureAll(ctx)
            if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return
            val n = NotificationCompat.Builder(ctx, PrayerNotificationChannels.DAILY)
                .setSmallIcon(NoorNotify.SMALL_ICON).setColor(NoorNotify.accent(ctx))
                .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(intent(ctx, id, screen)).setAutoCancel(true).setOnlyAlertOnce(true)
                .build()
            NotificationManagerCompat.from(ctx).notify(id, n)
        }
    }
}

/** One-off local reminder through WorkManager (same scheduler as the prayer refresh). */
object LocalReminder {
    fun schedule(ctx: Context, key: String, atMs: Long, title: String, body: String, screen: String, now: Long = System.currentTimeMillis()) {
        val delay = atMs - now
        if (delay <= 0) return cancel(ctx, key)
        val req = OneTimeWorkRequestBuilder<LocalReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("key" to key, "title" to title, "body" to body, "screen" to screen)).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("np_local_$key", ExistingWorkPolicy.REPLACE, req)
    }
    fun cancel(ctx: Context, key: String) { WorkManager.getInstance(ctx).cancelUniqueWork("np_local_$key") }
    fun notificationId(key: String) = 74_000 + (key.hashCode() and 0xFFF)
}

class LocalReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val key = inputData.getString("key") ?: return Result.success()
        LocalNotice.post(applicationContext, LocalReminder.notificationId(key), inputData.getString("title").orEmpty(),
            inputData.getString("body").orEmpty(), inputData.getString("screen") ?: "DASHBOARD")
        return Result.success()
    }
}
