package com.noorpro.app.prayer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 2×2 next-prayer home-screen widget. */
class NextPrayerWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            PrayerWidgetUpdater.refreshAll(context)
        }
    }

    override fun onEnabled(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            PrayerWidgetUpdater.refreshAll(context)
        }
    }
}
