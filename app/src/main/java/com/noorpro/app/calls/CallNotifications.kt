package com.noorpro.app.calls

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.noorpro.app.R
import com.noorpro.app.data.ChatRealtime

object CallNotifications {
    const val CHANNEL = "noor_calls"
    fun notificationId(callId: String) = 70_000 + (callId.hashCode() and 0xFFFF)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Incoming voice and video calls"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
            )
            enableVibration(true)
        })
    }

    fun showIncoming(context: Context, callId: String, callerUid: String, callerName: String, video: Boolean) {
        if (CallActivity.activeCallId == callId) return
        ensureChannel(context)
        val intent = CallActivity.incomingIntent(context, callId, callerUid, callerName, video)
        val pi = PendingIntent.getActivity(context, notificationId(callId), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText(if (video) "Incoming video call" else "Incoming voice call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(true)
            .setTimeoutAfter(45_000)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java)?.notify(notificationId(callId), n) }
    }
}

/** FCM: incoming-call pushes from the notifyIncomingCall Cloud Function. */
class NoorMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { ChatRealtime.registerPushToken() }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        if (d["kind"] != "call") return
        val callId = d["callId"].orEmpty()
        if (callId.isBlank()) return
        CallNotifications.showIncoming(applicationContext, callId, d["callerUid"].orEmpty(), d["callerName"].orEmpty().ifBlank { "Noor member" }, d["video"] == "1")
    }
}
