package com.example.audio.session

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Notification channel for Al Noor Audio playback controls.
 * Must stay separate from prayer adhan / soft / ongoing channels.
 */
object AlNoorPlaybackChannels {
    const val PLAYBACK = "al_noor_playback"
    const val PLAYBACK_NOTIFICATION_ID = 72001

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                PLAYBACK,
                "Al Noor Audio",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Nasheed/naat playback controls (not prayer adhan)"
                setShowBadge(false)
                setSound(null, null)
            },
        )
    }
}
