package com.noorpro.app.prayer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 2x2 next-prayer home-screen widget. */
class NextPrayerWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    override fun onEnabled(context: Context) {
        refresh(context)
    }

    /** Re-layout (e.g. hide/show the Hijri line) when the user resizes the widget. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refresh(context)
    }

    private fun refresh(context: Context) {
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerWidgetUpdater.refreshAll(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
