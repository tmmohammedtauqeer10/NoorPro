package com.noorpro.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.noorpro.app.MainActivity
import com.noorpro.app.R
import com.noorpro.app.data.BookmarkDatabase
import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.QuranLastRead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 1.0.98 Continue Quran 2x2 widget: last-read surah + ayah with a Continue button that opens the
 * Quran reader at that position. Falls back to the newest bookmark, then to Al-Fatihah ("Start reading").
 */
object ContinueQuranWidget {
    data class Target(val surahId: Int, val ayah: Int, val name: String, val label: String, val button: String)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingRefresh: Job? = null
    private const val MIN_HEIGHT_FOR_BOOK_DP = 150

    /** Debounced refresh, safe to call on every last-read change (scrolling the reader). */
    fun requestRefresh(context: Context) {
        val app = context.applicationContext
        pendingRefresh?.cancel()
        pendingRefresh = scope.launch {
            delay(1_500)
            refreshAll(app)
        }
    }

    suspend fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, ContinueQuranWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        val target = resolveTarget(context)
        ids.forEach { id ->
            val h = runCatching { mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) }.getOrDefault(0)
            mgr.updateAppWidget(id, buildViews(context, target, h))
        }
    }

    suspend fun resolveTarget(context: Context): Target {
        QuranLastRead.load(context)?.let { e ->
            val name = e.nameEnglish.ifBlank { surahName(e.surahId) }
            return Target(e.surahId, e.ayah, name, "Continue Quran", "Continue \u2192")
        }
        val bookmark = runCatching {
            BookmarkDatabase.getDatabase(context).bookmarkDao().getAllBookmarksSync().maxByOrNull { it.timestamp }
        }.getOrNull()
        if (bookmark != null) {
            val name = bookmark.surahNameEng.ifBlank { surahName(bookmark.surahId) }
            return Target(bookmark.surahId, bookmark.ayahNumber, name, "Continue Quran", "Continue \u2192")
        }
        return Target(1, 1, surahName(1), "Read Quran", "Start reading \u2192")
    }

    private fun surahName(id: Int): String =
        if (id < 0) "Juz ${-id}" else IslamicData.surahs.firstOrNull { it.id == id }?.nameEnglish ?: "Al-Fatihah"

    fun buildViews(context: Context, t: Target, heightDp: Int = 0): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_continue_quran_2x2)
        views.setTextViewText(R.id.widget_cq_label, t.label)
        views.setTextViewText(R.id.widget_cq_surah, t.name)
        views.setTextViewText(R.id.widget_cq_ayah, "Ayah ${t.ayah}")
        views.setTextViewText(R.id.widget_cq_button, t.button)
        views.setViewVisibility(R.id.widget_cq_book, if (heightDp in 1 until MIN_HEIGHT_FOR_BOOK_DP) View.GONE else View.VISIBLE)
        val open = openReaderPending(context, t)
        views.setOnClickPendingIntent(R.id.widget_cq_button, open)
        views.setOnClickPendingIntent(android.R.id.background, open)
        return views
    }

    fun openReaderPending(context: Context, t: Target): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "com.noorpro.app.widget.CONTINUE_QURAN"
            putExtra(MainActivity.EXTRA_QURAN_RESUME, true)
            putExtra(MainActivity.EXTRA_QURAN_SURAH_ID, t.surahId)
            putExtra(MainActivity.EXTRA_QURAN_AYAH, t.ayah)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, 72_301, intent, flags)
    }
}

class ContinueQuranWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)
    override fun onEnabled(context: Context) = refresh(context)
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refresh(context)
    }

    private fun refresh(context: Context) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ContinueQuranWidget.refreshAll(app) } finally { pending.finish() }
        }
    }
}
