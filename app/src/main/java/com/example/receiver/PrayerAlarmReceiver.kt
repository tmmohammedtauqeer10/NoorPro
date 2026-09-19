package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.UserPreferencesRepository
import com.example.ui.viewmodel.PrayerSettingsController
import com.example.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer time"
        val alarmSoundPref = intent.getStringExtra("ALARM_SOUND")
            ?: context.getSharedPreferences("prayer_settings_prefs", Context.MODE_PRIVATE)
                .getString("alarm_sound", "Mecca Adhan")
            ?: "Mecca Adhan"
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showNotification(context, prayerName, alarmSoundPref, intent.getIntExtra("REMINDER_OFFSET", 0))
            } catch (e: Exception) {
                CrashReporter.report(e, "Prayer notification failed")
            } finally {
                try {
                    val appContext = context.applicationContext
                    val location = UserPreferencesRepository(appContext).locationFlow.first()
                    if (!location.isAvailable) return@launch
                    val tomorrow = Calendar.getInstance().apply {
                        timeInMillis = maxOf(intent.getLongExtra("PRAYER_DATE", 0L), System.currentTimeMillis())
                        add(Calendar.DAY_OF_YEAR, 1)
                    }.time
                    val controller = PrayerSettingsController(appContext)
                    val nextPrayer = controller.calculatePrayers(
                        date = tomorrow, latitude = location.latitude,
                        longitude = location.longitude, schedule = false
                    ).firstOrNull { it.name == prayerName }
                    if (nextPrayer != null) controller.scheduleAlarms(listOf(nextPrayer), tomorrow)
                } catch (e: Exception) {
                    CrashReporter.report(e, "Unable to schedule next prayer reminder")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, prayerName: String, alarmSoundPref: String, offsetMinutes: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reminderType = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("reminder_type", "Notification") ?: "Notification"
        val isAlarm = reminderType == "Alarm" || !alarmSoundPref.equals("System Sound", ignoreCase = true)
        val channelId = if (isAlarm) "prayer_adhan_channel_v2" else "prayer_notification_channel_v2"
        val soundType = if (isAlarm) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        val selectedSound: Uri = RingtoneManager.getDefaultUri(soundType)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (isAlarm) "Prayer Adhan" else "Prayer Notifications",
                if (isAlarm) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for prayer (Adhan) times · $alarmSoundPref"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 250, 350)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(selectedSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PRAYER", true)
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, prayerName.hashCode(), openIntent, pendingFlags
        )

        val arabicCall = "حَيَّ عَلَى الصَّلَاة"
        val reminderText = if (offsetMinutes > 0) "$prayerName prayer is in $offsetMinutes minutes."
            else "It's time for $prayerName prayer."
        val bigText = "$reminderText\n$arabicCall  •  Hayya 'alas-salah\n" +
            "Hasten to the prayer. May Allah accept it from you."

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_adhan_notification)
            .setContentTitle("$prayerName • Prayer reminder")
            .setContentText(reminderText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setColor(0xFF0E8C73.toInt())
            .setColorized(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_adhan_notification, "Open App", contentPendingIntent)
            .setPriority(if (isAlarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(selectedSound)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        if (isAlarm) {
            builder.setFullScreenIntent(contentPendingIntent, true)
            builder.setOngoing(true)
        }

        notificationManager.notify(prayerName.hashCode(), builder.build())
    }
}
