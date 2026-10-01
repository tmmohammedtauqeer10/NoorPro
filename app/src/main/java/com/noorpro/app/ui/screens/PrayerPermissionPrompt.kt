package com.noorpro.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noorpro.app.prayer.PrayerPrefs
import com.noorpro.app.prayer.PrayerScheduler

/** What the prayer reminders still need from the user. */
internal enum class PrayerPermissionGap { NOTIFICATIONS, EXACT_ALARM, NONE }

internal object PrayerPermissions {
    fun notificationsGranted(context: Context): Boolean {
        val runtimeOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return runtimeOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun gap(context: Context): PrayerPermissionGap = when {
        !notificationsGranted(context) -> PrayerPermissionGap.NOTIFICATIONS
        !PrayerScheduler.canScheduleExact(context) -> PrayerPermissionGap.EXACT_ALARM
        else -> PrayerPermissionGap.NONE
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }.onFailure {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
    }
}

/**
 * Explains and requests the two things prayer reminders cannot work without: the notification
 * permission (Android 13+ runtime prompt, or the app notification settings when it was denied
 * permanently / blocked) and the "Alarms & reminders" special access (Android 12+). Re-checks every
 * time the app returns to the foreground, and can be dismissed ("Not now") for three days.
 */
@Composable
fun PrayerPermissionPrompt() {
    val context = LocalContext.current
    val prefs = remember { PrayerPrefs(context) }
    var tick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tick++
                PrayerScheduler.rescheduleAsync(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) prefs.notificationAskCount = prefs.notificationAskCount + 1
        tick++
    }

    @Suppress("UNUSED_EXPRESSION") tick
    val gap = PrayerPermissions.gap(context)
    val snoozed = System.currentTimeMillis() < prefs.permissionPromptSnoozeUntil
    if (gap == PrayerPermissionGap.NONE || snoozed) return

    val snooze: () -> Unit = {
        prefs.permissionPromptSnoozeUntil = System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000
        tick++
    }
    when (gap) {
        PrayerPermissionGap.NOTIFICATIONS -> AlertDialog(
            onDismissRequest = snooze,
            title = { Text("Allow prayer notifications") },
            text = {
                Text(
                    "Noor Pro needs notification permission to show the adhan pop-up at each prayer time and " +
                        "the next-prayer reminder. Without it you will not be alerted."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val activity = context as? Activity
                    val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED &&
                        (prefs.notificationAskCount < 2 ||
                            activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true)
                    if (canAsk) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else PrayerPermissions.openAppNotificationSettings(context)
                }) { Text("Allow") }
            },
            dismissButton = { TextButton(onClick = snooze) { Text("Not now") } }
        )
        PrayerPermissionGap.EXACT_ALARM -> AlertDialog(
            onDismissRequest = snooze,
            title = { Text("Allow exact prayer alarms") },
            text = {
                Text(
                    "To sound the adhan at the exact prayer minute, Android needs the \"Alarms & reminders\" " +
                        "permission. On the next screen, switch it on for Noor Pro. Without it, reminders may " +
                        "arrive several minutes late."
                )
            },
            confirmButton = {
                TextButton(onClick = { PrayerPermissions.openExactAlarmSettings(context) }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = snooze) { Text("Not now") } }
        )
        PrayerPermissionGap.NONE -> Unit
    }
}
