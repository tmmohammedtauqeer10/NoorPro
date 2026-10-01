package com.noorpro.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noorpro.app.prayer.PrayerDisplay
import com.noorpro.app.prayer.PrayerPrefs
import com.noorpro.app.prayer.PrayerScheduler
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

private val Green = Color(0xFF0E8C73)

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * Reminder options shown at the top of the prayer settings: permission status, Hijri date, Qibla
 * shortcut, pre-prayer reminder (10 min by default), Jumu'ah, Ramadan and the daily ayah/dua.
 */
@Composable
fun PrayerReminderExtrasCard(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val prefs = remember { PrayerPrefs(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    @Suppress("UNUSED_EXPRESSION") refresh
    var pre by remember { mutableStateOf(prefs.preReminderEnabled) }
    var preMin by remember { mutableIntStateOf(prefs.preReminderMinutes) }
    var jummah by remember { mutableStateOf(prefs.jummahEnabled) }
    var ramadan by remember { mutableStateOf(prefs.ramadanEnabled) }
    var daily by remember { mutableStateOf(prefs.dailyAyahEnabled) }
    var dailyMin by remember { mutableIntStateOf(prefs.dailyAyahMinuteOfDay) }
    val replan = { PrayerScheduler.rescheduleAsync(context) }
    val gap = PrayerPermissions.gap(context)
    val hijri = remember(refresh) { PrayerDisplay.hijriLabel(context, short = false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Green.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Green.copy(alpha = 0.27f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (hijri.isNotBlank()) {
                Text("Today: $hijri", color = Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }

            // Permission status
            when (gap) {
                com.noorpro.app.ui.screens.PrayerPermissionGap.NOTIFICATIONS -> {
                    Text("Notifications are off - no adhan pop-up will appear.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    Button(
                        onClick = { PrayerPermissions.openAppNotificationSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.padding(top = 6.dp)
                    ) { Text("Allow notifications") }
                }
                com.noorpro.app.ui.screens.PrayerPermissionGap.EXACT_ALARM -> {
                    Text("Exact alarms are off - the adhan may be a few minutes late.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    Button(
                        onClick = { PrayerPermissions.openExactAlarmSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.padding(top = 6.dp)
                    ) { Text("Allow exact alarms") }
                }
                else -> Text("Notifications and exact alarms are allowed.", color = Green, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.navigateTo(DeenScreen.QIBLA_MORE) }) { Text("Open Qibla compass") }
            Spacer(Modifier.height(8.dp))

            ToggleRow("Reminder before prayer", "Get a heads-up $preMin minutes before each enabled prayer", pre) {
                pre = it; prefs.preReminderEnabled = it; replan()
            }
            if (pre) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20, 30).forEach { m ->
                        FilterChip(selected = preMin == m, onClick = { preMin = m; prefs.preReminderMinutes = m; replan() }, label = { Text("$m min") })
                    }
                }
            }
            ToggleRow("Jumu'ah reminder", "Friday, 30 minutes before Dhuhr", jummah) {
                jummah = it; prefs.jummahEnabled = it; replan()
            }
            ToggleRow("Ramadan reminders", "Suhoor and Iftar reminders during Ramadan", ramadan) {
                ramadan = it; prefs.ramadanEnabled = it; replan()
            }
            ToggleRow("Daily ayah / dua", "One short reminder from the Quran every day", daily) {
                daily = it; prefs.dailyAyahEnabled = it; replan()
            }
            if (daily) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6 * 60 to "6:00 AM", 8 * 60 to "8:00 AM", 12 * 60 to "12:00 PM", 18 * 60 to "6:00 PM", 21 * 60 to "9:00 PM").forEach { (min, label) ->
                        FilterChip(selected = dailyMin == min, onClick = { dailyMin = min; prefs.dailyAyahMinuteOfDay = min; replan() }, label = { Text(label) })
                    }
                }
            }
        }
    }
}
