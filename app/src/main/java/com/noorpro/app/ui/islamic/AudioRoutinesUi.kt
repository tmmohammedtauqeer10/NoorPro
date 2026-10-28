package com.noorpro.app.ui.islamic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.islamic.audio.*
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel

/* 1.1.6 Audio routines (audio_routines_enabled): local, recurring listening habits. */

data class RoutineDraft(
    val id: String? = null,
    val title: String = "",
    val surahs: List<Int> = emptyList(),
    val days: Set<Int> = RoutineLogic.EVERY_DAY,
    val minuteOfDay: Int = 21 * 60,
    val query: String = "",
    val error: String? = null,
)

data class AudioRoutinesUi(
    val routines: List<AudioRoutine> = emptyList(),
    val pending: AudioRoutine? = null,
    val now: Long = System.currentTimeMillis(),
    val draft: RoutineDraft? = null,
)

class AudioRoutinesActions(
    val onBack: () -> Unit = {},
    val onPlay: (AudioRoutine) -> Unit = {},
    val onDismissPending: () -> Unit = {},
    val onToggle: (AudioRoutine, Boolean) -> Unit = { _, _ -> },
    val onAddPreset: (AudioRoutine) -> Unit = {},
    val onEdit: (AudioRoutine?) -> Unit = {},
    val onDraft: (RoutineDraft) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onCloseEditor: () -> Unit = {},
)

@Composable
fun AudioRoutinesScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val store = remember { RoutineStore(ctx) }
    fun load(d: RoutineDraft? = null) = AudioRoutinesUi(store.all(), store.all().firstOrNull { it.id == store.pending }, System.currentTimeMillis(), d)
    var ui by remember { mutableStateOf(load()) }
    LaunchedEffect(Unit) { RoutineScheduler.syncAll(ctx) }
    fun commit(list: List<AudioRoutine>) { store.save(list); RoutineScheduler.syncAll(ctx); ui = load() }

    AudioRoutinesContent(ui, AudioRoutinesActions(
        onBack = { if (ui.draft != null) ui = ui.copy(draft = null) else viewModel.goBack() },
        onPlay = { r -> store.pending = null; ui = load(); viewModel.playQueue(RoutineLogic.queue(r), 0) },
        onDismissPending = { store.pending = null; ui = load() },
        onToggle = { r, on -> commit(store.all().map { if (it.id == r.id) it.copy(enabled = on) else it }) },
        onAddPreset = { p -> if (store.all().size < RoutineLogic.MAX_ROUTINES) commit(store.all() + p) },
        onEdit = { r -> ui = ui.copy(draft = r?.let { RoutineDraft(it.id, it.title, it.surahs, it.days, it.minuteOfDay) } ?: RoutineDraft()) },
        onDraft = { ui = ui.copy(draft = it) },
        onSave = {
            val d = ui.draft ?: return@AudioRoutinesActions
            val err = RoutineLogic.validate(d.title, d.surahs, d.days)
                ?: if (d.id == null && store.all().size >= RoutineLogic.MAX_ROUTINES) "Up to ${RoutineLogic.MAX_ROUTINES} routines." else null
            if (err != null) { ui = ui.copy(draft = d.copy(error = err)); return@AudioRoutinesActions }
            val old = store.all().firstOrNull { it.id == d.id }
            val r = AudioRoutine(d.id ?: RoutineLogic.newId(System.currentTimeMillis()), d.title.trim(), d.surahs, d.days, d.minuteOfDay, old?.enabled ?: true, old?.why ?: "")
            commit(store.all().filterNot { it.id == r.id } + r)
        },
        onDelete = { id -> RoutineScheduler.cancel(ctx, id); commit(store.all().filterNot { it.id == id }) },
        onCloseEditor = { ui = ui.copy(draft = null) },
    ))
}

@Composable
fun AudioRoutinesContent(ui: AudioRoutinesUi, actions: AudioRoutinesActions, modifier: Modifier = Modifier) {
    val d = ui.draft
    if (d != null) { RoutineEditorContent(d, actions, modifier); return }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("routines_screen")) {
        IkHeader("Audio routines", "Listening habits, on your schedule", actions.onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val pending = ui.pending
            val next = RoutineLogic.next(ui.routines, ui.now)
            IkHero {
                if (pending != null) {
                    Text("READY WHEN YOU ARE", color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(pending.title, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(RoutineLogic.surahsLabel(pending), color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IkGoldButton("Play now", Icons.Outlined.PlayArrow, "routines_play_pending") { actions.onPlay(pending) }
                        TextButton(actions.onDismissPending) { Text("Not now", color = IkCream) }
                    }
                } else {
                    Text(if (next != null) "NEXT ROUTINE" else "SMALL, STEADY LISTENING", color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(next?.first?.title ?: "Make the Quran part of your day", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        modifier = Modifier.testTag("routines_next"))
                    Text(next?.let { "${RoutineLogic.whenLabel(it.second, ui.now)} · ${RoutineLogic.surahsLabel(it.first)}" }
                        ?: "Pick a sunnah routine below or make your own. You'll get a gentle reminder and start listening with one tap.",
                        color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }

            IkSection("Your routines", trailing = { TextButton({ actions.onEdit(null) }, Modifier.testTag("routines_new")) { Text("+ New", color = ikAccentText(), fontWeight = FontWeight.Bold) } })
            if (ui.routines.isEmpty()) Text("No routines yet.", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.testTag("routines_empty"))
            ui.routines.forEach { r ->
                IkCard(Modifier.testTag("routine_${r.id}"), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(if (r.enabled) IkGreen else ikSoft()).clickable { actions.onPlay(r) }.testTag("routine_play_${r.id}"),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.PlayArrow, "Play", tint = if (r.enabled) Color.White else ikAccentText())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).clickable { actions.onEdit(r) }) {
                            Text(r.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(RoutineLogic.summary(r), color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(RoutineLogic.surahsLabel(r), color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(r.enabled, { actions.onToggle(r, it) }, Modifier.testTag("routine_switch_${r.id}"), colors = SwitchDefaults.colors(checkedTrackColor = IkGreen))
                    }
                }
            }

            val unused = RoutineLogic.PRESETS.filter { p -> ui.routines.none { it.id == p.id } }
            if (unused.isNotEmpty()) {
                IkSection("From the sunnah")
                unused.forEach { p ->
                    IkCard(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IkIconTile(if (p.minuteOfDay >= 20 * 60) Icons.Outlined.Bedtime else Icons.Outlined.WbSunny, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(RoutineLogic.summary(p), color = ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                if (p.why.isNotBlank()) Text(p.why, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp)
                            }
                            IkPill("Add", false, icon = Icons.Outlined.Add, tag = "routines_add_${p.id}") { actions.onAddPreset(p) }
                        }
                    }
                }
            }
            IkNotice("Stays on your phone", "Routines are scheduled on this device only. At the time you choose you'll get a quiet reminder; nothing plays until you tap it.", tag = "routines_notice")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IkGoldButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, onClick: () -> Unit) {
    Row(Modifier.height(42.dp).clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = onClick).padding(horizontal = 16.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = IkGreenDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
        Text(text, color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private val TIME_PRESETS = listOf(5 * 60 + 30 to "After Fajr", 9 * 60 to "Morning", 13 * 60 + 30 to "After Dhuhr", 19 * 60 to "After Maghrib", 22 * 60 to "Before sleep")

@Composable
fun RoutineEditorContent(d: RoutineDraft, actions: AudioRoutinesActions, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("routine_editor")) {
        IkHeader(if (d.id == null) "New routine" else "Edit routine", "What, which days, and when", actions.onCloseEditor)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IkField(d.title, { actions.onDraft(d.copy(title = it, error = null)) }, "Name", Modifier.fillMaxWidth(), tag = "routine_title", maxLen = 40)

            IkSection("Surahs")
            if (d.surahs.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                d.surahs.forEach { s -> IkPill("${OfflineQuranLogic.surahName(s)}  ✕", true, tag = "routine_surah_$s") { actions.onDraft(d.copy(surahs = d.surahs - s, error = null)) } }
            }
            IkField(d.query, { actions.onDraft(d.copy(query = it)) }, "Add a surah: name or number", Modifier.fillMaxWidth(), tag = "routine_query", maxLen = 30)
            val found = RoutineLogic.searchSurah(d.query).filter { it !in d.surahs }
            if (found.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                found.forEach { s -> IkPill("$s · ${OfflineQuranLogic.surahName(s)}", false, icon = Icons.Outlined.Add, tag = "routine_found_$s") {
                    if (d.surahs.size < RoutineLogic.MAX_SURAHS) actions.onDraft(d.copy(surahs = d.surahs + s, query = "", error = null))
                } }
            }

            IkSection("Days")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { day ->
                    val on = day in d.days
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) IkGreen else ikSoft())
                        .clickable { actions.onDraft(d.copy(days = if (on) d.days - day else d.days + day, error = null)) }.testTag("routine_day_$day"),
                        contentAlignment = Alignment.Center) {
                        Text(RoutineLogic.dayLabel(day).take(2), color = if (on) Color.White else ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Text(RoutineLogic.daysLabel(d.days), color = stitchMutedText(), fontSize = 12.sp)

            IkSection("Time")
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", "routine_time_minus") { actions.onDraft(d.copy(minuteOfDay = (d.minuteOfDay - 15 + 1440) % 1440)) }
                Text(RoutineLogic.timeLabel(d.minuteOfDay), color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    modifier = Modifier.weight(1f).testTag("routine_time"), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                StepButton("+", "routine_time_plus") { actions.onDraft(d.copy(minuteOfDay = (d.minuteOfDay + 15) % 1440)) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TIME_PRESETS.forEach { (m, label) -> IkPill(label, d.minuteOfDay == m, tag = "routine_at_$m") { actions.onDraft(d.copy(minuteOfDay = m)) } }
            }
            Text("Prayer times move through the year, so pick a time a little after the prayer.", color = stitchMutedText(), fontSize = 11.sp)

            d.error?.let { Text(it, color = IkRed, fontSize = 12.sp, modifier = Modifier.testTag("routine_error")) }
            IkButton("Save routine", Modifier.fillMaxWidth(), Icons.Outlined.Alarm, tag = "routine_save") { actions.onSave() }
            if (d.id != null) IkButton("Delete routine", Modifier.fillMaxWidth(), Icons.Outlined.DeleteOutline, primary = false, tag = "routine_delete") { actions.onDelete(d.id) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepButton(label: String, tag: String, onClick: () -> Unit) {
    Box(Modifier.size(46.dp).clip(CircleShape).background(ikSoft()).clickable(onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        Text(label, color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}
