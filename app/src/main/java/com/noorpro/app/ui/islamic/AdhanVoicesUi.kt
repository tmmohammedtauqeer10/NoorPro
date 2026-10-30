package com.noorpro.app.ui.islamic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.islamic.audio.AdhanPreview
import com.noorpro.app.islamic.audio.AdhanVoiceLogic
import com.noorpro.app.islamic.audio.AdhanVoiceOption
import com.noorpro.app.prayer.AdhanSound
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.delay

/* 1.1.6 Adhan voices (adhan_voices_enabled): preview and apply the prayer-time sound. */

data class AdhanVoicesUi(val applied: AdhanSound = AdhanSound.ALARM, val selected: AdhanSound = applied, val previewing: AdhanSound? = null)

class AdhanVoicesActions(
    val onBack: () -> Unit = {},
    val onSelect: (AdhanSound) -> Unit = {},
    val onPreview: (AdhanVoiceOption) -> Unit = {},
    val onApply: () -> Unit = {},
)

@Composable
fun AdhanVoicesScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val controller = viewModel.prayerSettingsController
    val current by controller.alarmSound.collectAsState()
    val applied = AdhanVoiceLogic.current(current).sound
    var selected by remember { mutableStateOf(applied) }
    var previewing by remember { mutableStateOf<AdhanSound?>(null) }
    val preview = remember { AdhanPreview(ctx) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }
    LaunchedEffect(previewing) { if (previewing != null) { delay(AdhanVoiceLogic.PREVIEW_MS); preview.stop(); previewing = null } }
    AdhanVoicesContent(AdhanVoicesUi(applied, selected, previewing), AdhanVoicesActions(
        onBack = { preview.stop(); viewModel.goBack() },
        onSelect = { selected = it },
        onPreview = { o -> if (previewing == o.sound) { preview.stop(); previewing = null } else { preview.play(o); previewing = o.sound; selected = o.sound } },
        onApply = { preview.stop(); previewing = null; controller.updateAlarmSound(AdhanVoiceLogic.OPTIONS.first { it.sound == selected }.pref)
            com.noorpro.app.prayer.PrayerScheduler.rescheduleAsync(ctx) },
    ))
}

@Composable
fun AdhanVoicesContent(ui: AdhanVoicesUi, actions: AdhanVoicesActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("adhan_screen")) {
        IkHeader("Adhan sound", "What plays at each prayer time", actions.onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val now = AdhanVoiceLogic.OPTIONS.first { it.sound == ui.applied }
            IkHero {
                Text("IN USE NOW", color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(now.title, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.testTag("adhan_applied"))
                Text("Used for Fajr, Dhuhr, Asr, Maghrib and Isha. Turn single prayers on or off in Prayer settings.", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp)
            }
            IkSection("Choose a sound")
            AdhanVoiceLogic.OPTIONS.forEach { o ->
                val sel = ui.selected == o.sound
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikCard())
                    .border(if (sel) 2.dp else 1.dp, if (sel) IkGreen else stitchLine(), RoundedCornerShape(20.dp))
                    .clickable { actions.onSelect(o.sound) }.padding(14.dp).testTag("adhan_option_${o.sound.name}"), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (sel) IkGreen else stitchLine(), CircleShape), contentAlignment = Alignment.Center) {
                        if (sel) Box(Modifier.size(11.dp).clip(CircleShape).background(IkGreen))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(o.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (o.sound == ui.applied) { Spacer(Modifier.width(8.dp)); IkTag("In use") }
                        }
                        Text(o.description, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    val playing = ui.previewing == o.sound
                    Box(Modifier.size(40.dp).clip(CircleShape).background(if (playing) IkGold else ikSoft()).clickable { actions.onPreview(o) }.testTag("adhan_preview_${o.sound.name}"),
                        contentAlignment = Alignment.Center) {
                        Icon(when { playing -> Icons.Outlined.Stop; o.sound == AdhanSound.SILENT -> Icons.Outlined.Vibration; else -> Icons.Outlined.PlayArrow },
                            if (playing) "Stop preview" else "Preview", tint = if (playing) IkGreenDeep else ikAccentText())
                    }
                }
            }
            IkButton(if (ui.selected == ui.applied) "In use" else "Use ${AdhanVoiceLogic.OPTIONS.first { it.sound == ui.selected }.title.lowercase()}",
                Modifier.fillMaxWidth(), Icons.Outlined.Check, enabled = ui.selected != ui.applied, tag = "adhan_apply") { actions.onApply() }
            IkNotice("Recorded adhan voices", "NoorPro doesn't include recorded adhans yet: we'll only add recordings we have permission to share. Until then your phone's own tones are used, so the adhan always arrives on time.",
                tag = "adhan_no_recordings")
            Spacer(Modifier.height(24.dp))
        }
    }
}
