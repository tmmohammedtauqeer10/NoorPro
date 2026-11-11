package com.noorpro.app.ui.islamic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.noorpro.app.data.IslamicData
import com.noorpro.app.islamic.audio.*
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* 1.1.6 Offline Quran manager (quran_offline_enabled): per-reciter downloads, storage, listening tools. */

data class OfflineDownloadState(val surah: Int, val fraction: Float, val queued: Boolean = false)

data class OfflineQuranUi(
    val storages: List<ReciterStorage> = emptyList(),
    val selectedId: Int = IslamicData.reciters.first().id,
    val playbackId: Int = IslamicData.reciters.first().id,
    val freeBytes: Long = 0L,
    val wifiOnly: Boolean = true,
    val download: OfflineDownloadState? = null,
    val repeatTimes: Int = 1,
    val sleepEndsIn: String? = null,
    val sleepMinutes: Int? = null,
    val sleepAtSurahEnd: Boolean = false,
    val loading: Boolean = true,
) {
    val selected: ReciterStorage? get() = storages.firstOrNull { it.reciter.id == selectedId }
    val totalBytes get() = storages.sumOf { it.bytes }
}

class OfflineQuranActions(
    val onBack: () -> Unit = {},
    val onReciter: (Int) -> Unit = {},
    val onUseForPlayback: () -> Unit = {},
    val onPack: (DownloadPack) -> Unit = {},
    val onCancelDownload: () -> Unit = {},
    val onResume: (Int) -> Unit = {},
    val onDeleteSurah: (Int) -> Unit = {},
    val onDeleteReciter: (Int) -> Unit = {},
    val onWifiOnly: (Boolean) -> Unit = {},
    val onRepeat: (Int) -> Unit = {},
    val onSleep: (Int?) -> Unit = {},
)

@Composable
fun OfflineQuranScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { OfflinePrefs(ctx) }
    val playback by viewModel.selectedReciter.collectAsState()
    val repeat by viewModel.ayahRepeatTimes.collectAsState()
    val sleepEnds by viewModel.sleepTimerEndsAt.collectAsState()
    val sleepEnd by viewModel.sleepAtSurahEnd.collectAsState()
    var sleepMinutes by remember { mutableStateOf<Int?>(null) }
    var ui by remember { mutableStateOf(OfflineQuranUi(selectedId = playback.id, playbackId = playback.id, wifiOnly = prefs.wifiOnly)) }
    var tick by remember { mutableIntStateOf(0) }
    fun refresh() = scope.launch {
        val (s, free) = withContext(Dispatchers.IO) { OfflineQuranStore.all(ctx) to OfflineQuranStore.freeBytes(ctx) }
        ui = ui.copy(storages = s, freeBytes = free, loading = false)
    }
    LaunchedEffect(Unit) { refresh() }
    // Watch the selected reciter's download job; refresh storage while it runs.
    LaunchedEffect(ui.selectedId, tick) {
        val r = IslamicData.reciters.firstOrNull { it.id == ui.selectedId } ?: return@LaunchedEffect
        while (true) {
            val info = withContext(Dispatchers.IO) { runCatching { WorkManager.getInstance(ctx).getWorkInfosForUniqueWork(OfflineDownloads.workName(r)).get() }.getOrNull() }
                ?.firstOrNull { !it.state.isFinished }
            val state = info?.let { OfflineDownloadState(it.progress.getInt("surah", 0), it.progress.getFloat("fraction", 0f), it.state != WorkInfo.State.RUNNING) }
            val was = ui.download
            ui = ui.copy(download = state)
            if (state != null || was != null) refresh()
            if (state == null) break
            delay(1500)
        }
    }
    val now = System.currentTimeMillis()
    LaunchedEffect(sleepEnds) { while (sleepEnds != null) { delay(1000); ui = ui.copy(sleepEndsIn = ListeningLogic.remaining(sleepEnds, System.currentTimeMillis())) } }
    val shown = ui.copy(playbackId = playback.id, repeatTimes = repeat, sleepAtSurahEnd = sleepEnd,
        sleepMinutes = if (sleepEnds == null) null else sleepMinutes, sleepEndsIn = if (sleepEnds == null) null else ui.sleepEndsIn ?: ListeningLogic.remaining(sleepEnds, now))
    val reciter = IslamicData.reciters.firstOrNull { it.id == ui.selectedId } ?: IslamicData.reciters.first()
    fun enqueue(surahs: List<Int>) {
        OfflineDownloads.enqueue(ctx, reciter, surahs, prefs.wifiOnly)
        ui = ui.copy(download = OfflineDownloadState(surahs.first(), 0f, queued = true)); tick++
    }

    OfflineQuranContent(shown, OfflineQuranActions(
        onBack = { viewModel.goBack() },
        onReciter = { ui = ui.copy(selectedId = it) },
        onUseForPlayback = { viewModel.selectReciter(reciter) },
        onPack = { p -> ui.selected?.let { s -> OfflineQuranLogic.missing(p.surahs, s.completeSurahs).takeIf { it.isNotEmpty() }?.let(::enqueue) } },
        onCancelDownload = { OfflineDownloads.cancel(ctx, reciter); ui = ui.copy(download = null); refresh() },
        onResume = { enqueue(listOf(it)) },
        onDeleteSurah = { s -> scope.launch { withContext(Dispatchers.IO) { com.noorpro.app.audio.QuranAudioCache.deleteSurah(ctx, reciter.prefixUrl, s) }; refresh() } },
        onDeleteReciter = { id -> val r = IslamicData.reciters.first { it.id == id }
            OfflineDownloads.cancel(ctx, r)
            scope.launch { withContext(Dispatchers.IO) { com.noorpro.app.audio.QuranAudioCache.deleteReciter(ctx, r.prefixUrl) }; refresh() } },
        onWifiOnly = { prefs.wifiOnly = it; ui = ui.copy(wifiOnly = it) },
        onRepeat = { viewModel.setAyahRepeatTimes(it) },
        onSleep = { m ->
            when (m) {
                null -> { viewModel.setSleepTimer(null); viewModel.setSleepAtSurahEnd(false); sleepMinutes = null }
                -1 -> { viewModel.setSleepAtSurahEnd(true); sleepMinutes = null }
                else -> { viewModel.setSleepTimer(m); sleepMinutes = m }
            }
        },
    ))
}

private const val COLLAPSED_ROWS = 6

@Composable
fun OfflineQuranContent(ui: OfflineQuranUi, actions: OfflineQuranActions, modifier: Modifier = Modifier) {
    var confirmDelete by remember { mutableStateOf<Int?>(null) }
    var showAll by remember(ui.selectedId) { mutableStateOf(false) }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("offline_screen")) {
        IkHeader("Offline Quran", "Recitations saved on this phone", actions.onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val sel = ui.selected
            IkHero {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SAVED FOR ${(sel?.reciter?.name ?: "").uppercase()}", color = IkGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text("${sel?.surahCount ?: 0} of 114 surahs", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.testTag("offline_count"))
                        Spacer(Modifier.height(4.dp))
                        Text("${OfflineQuranLogic.formatBytes(sel?.bytes ?: 0)} for this reciter · ${OfflineQuranLogic.formatBytes(ui.totalBytes)} in total",
                            color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("${OfflineQuranLogic.formatBytes(ui.freeBytes)} free on this phone", color = IkCream.copy(alpha = 0.65f), fontSize = 11.sp)
                    }
                    IkRing((sel?.surahCount ?: 0) / 114f, size = 84.dp, stroke = 8.dp) {
                        Icon(Icons.Outlined.DownloadDone, null, tint = IkCream, modifier = Modifier.size(28.dp))
                    }
                }
            }

            IkSection("Reciter")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.storages.forEach { s ->
                    IkPill(OfflineQuranLogic.shortName(s.reciter) +
                        if (s.surahCount > 0) " · ${s.surahCount}" else "", s.reciter.id == ui.selectedId, tag = "offline_reciter_${s.reciter.id}") { actions.onReciter(s.reciter.id) }
                }
            }
            if (sel != null) {
                val lic = OfflineQuranLogic.license(sel.reciter)
                IkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IkIconTile(Icons.Outlined.RecordVoiceOver)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sel.reciter.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(sel.reciter.description, color = stitchMutedText(), fontSize = 12.sp)
                        }
                        if (ui.playbackId == sel.reciter.id) IkTag("Playing voice")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Verified, null, tint = ikAccentText(), modifier = Modifier.size(16.dp).padding(top = 1.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${lic.source} · ${lic.note}", color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.testTag("offline_license"))
                    }
                    if (ui.playbackId != sel.reciter.id) {
                        Spacer(Modifier.height(10.dp))
                        IkButton("Use this voice for playback", Modifier.fillMaxWidth(), Icons.Outlined.PlayCircle, primary = false, tag = "offline_use") { actions.onUseForPlayback() }
                    }
                }

                ui.download?.let { d ->
                    IkCard(Modifier.testTag("offline_progress")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (d.queued) "Waiting to start" else "Saving ${OfflineQuranLogic.surahName(d.surah)}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (d.queued && ui.wifiOnly) "Starts when you're on Wi-Fi" else "${(d.fraction * 100).toInt()}% · keeps going if you leave this screen", color = stitchMutedText(), fontSize = 12.sp)
                            }
                            TextButton(actions.onCancelDownload, Modifier.testTag("offline_cancel")) { Text("Cancel", color = IkRed) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(stitchLine())) {
                            Box(Modifier.fillMaxWidth(d.fraction.coerceIn(0.02f, 1f)).fillMaxHeight().background(IkGreen))
                        }
                    }
                }

                if (OfflineQuranLogic.canDownload(sel.reciter)) {
                    IkSection("Save for offline")
                    OfflineQuranLogic.PACKS.forEach { p ->
                        val missing = OfflineQuranLogic.missing(p.surahs, sel.completeSurahs)
                        val est = OfflineQuranLogic.estimateBytes(sel.reciter, missing)
                        val enough = OfflineQuranLogic.enoughSpace(est, ui.freeBytes)
                        IkCard(padding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IkIconTile(if (missing.isEmpty()) Icons.Outlined.DownloadDone else Icons.Outlined.Download, size = 38.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(p.subtitle, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(when {
                                        missing.isEmpty() -> "All ${p.surahs.size} saved"
                                        !enough -> "Needs ≈ ${OfflineQuranLogic.formatBytes(est)} · not enough space"
                                        else -> "${p.surahs.size - missing.size} of ${p.surahs.size} saved · ≈ ${OfflineQuranLogic.formatBytes(est)} to go"
                                    }, color = if (missing.isNotEmpty() && !enough) IkAmber else ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (missing.isNotEmpty()) IkPill("Save", false, icon = Icons.Outlined.Download, tag = "offline_pack_${p.id}") { if (enough && ui.download == null) actions.onPack(p) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Download on Wi-Fi only", color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Saves your mobile data", color = stitchMutedText(), fontSize = 12.sp)
                        }
                        Switch(ui.wifiOnly, actions.onWifiOnly, Modifier.testTag("offline_wifi"), colors = SwitchDefaults.colors(checkedTrackColor = IkGreen))
                    }
                } else {
                    IkNotice("Streaming only", OfflineQuranLogic.license(sel.reciter).note, tag = "offline_stream_only")
                }

                IkSection("Saved surahs", trailing = { if (sel.bytes > 0) TextButton({ confirmDelete = sel.reciter.id }, Modifier.testTag("offline_delete_all")) { Text("Remove all", color = IkRed, fontSize = 12.sp) } })
                if (sel.completeSurahs.isEmpty() && sel.partialSurahs.isEmpty()) {
                    Text(if (ui.loading) "Checking storage…" else "Nothing saved for this reciter yet. Saved surahs play without internet, from the Quran player as usual.",
                        color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.testTag("offline_empty"))
                } else IkCard(padding = 4.dp) {
                    val saved = (sel.partialSurahs.sorted() + sel.completeSurahs.sorted())
                    val rows = if (showAll) saved else saved.take(COLLAPSED_ROWS)
                    rows.forEachIndexed { i, s ->
                        val partial = s in sel.partialSurahs
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("offline_surah_$s"), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(ikSoft()), contentAlignment = Alignment.Center) {
                                Text("$s", color = ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(OfflineQuranLogic.surahName(s), color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (partial) "Partly saved" else "${OfflineQuranLogic.ayahs(s)} ayahs · offline", color = if (partial) IkAmber else stitchMutedText(), fontSize = 11.sp)
                            }
                            if (partial) IkPill("Resume", false, tag = "offline_resume_$s") { actions.onResume(s) }
                            Icon(Icons.Outlined.DeleteOutline, "Remove", tint = stitchMutedText(), modifier = Modifier.padding(start = 6.dp).size(20.dp).clickable { actions.onDeleteSurah(s) }.testTag("offline_del_$s"))
                        }
                        if (i < rows.size - 1) Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(stitchLine()))
                    }
                    if (saved.size > COLLAPSED_ROWS) TextButton({ showAll = !showAll }, Modifier.fillMaxWidth().testTag("offline_show_all")) {
                        Text(if (showAll) "Show less" else "Show all ${saved.size}", color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            IkSection("Listening tools")
            IkCard {
                Text("Repeat each ayah", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Helps with memorising: each ayah plays this many times before moving on.", color = stitchMutedText(), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ListeningLogic.REPEAT_CHOICES.forEach { n -> IkPill(ListeningLogic.repeatLabel(n), ui.repeatTimes == n, tag = "offline_repeat_$n") { actions.onRepeat(n) } }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sleep timer", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    val status = when { ui.sleepAtSurahEnd -> "Stops after this surah"; ui.sleepEndsIn != null -> "Stops in ${ui.sleepEndsIn}"; else -> "Off" }
                    Text(status, color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("offline_sleep_status"))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val off = !ui.sleepAtSurahEnd && ui.sleepEndsIn == null
                    IkPill("Off", off, tag = "offline_sleep_off") { actions.onSleep(null) }
                    ListeningLogic.SLEEP_CHOICES.forEach { m ->
                        val on = if (m == -1) ui.sleepAtSurahEnd else ui.sleepMinutes == m && ui.sleepEndsIn != null
                        IkPill(ListeningLogic.sleepLabel(m), on, icon = if (m == -1) Icons.Outlined.Bedtime else null, tag = "offline_sleep_$m") { actions.onSleep(m) }
                    }
                }
            }

            IkSection("Storage by reciter")
            IkCard(padding = 4.dp) {
                val used = ui.storages.filter { it.bytes > 0 }
                if (used.isEmpty()) Text("No recitations saved yet.", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                used.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp).testTag("offline_storage_${s.reciter.id}"), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.reciter.name, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${s.surahCount} surahs · ${OfflineQuranLogic.formatBytes(s.bytes)}", color = stitchMutedText(), fontSize = 11.sp)
                        }
                        TextButton({ confirmDelete = s.reciter.id }) { Text("Remove", color = IkRed, fontSize = 12.sp) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    confirmDelete?.let { id ->
        val name = ui.storages.firstOrNull { it.reciter.id == id }?.reciter?.name ?: ""
        AlertDialog(onDismissRequest = { confirmDelete = null },
            title = { Text("Remove saved audio?") },
            text = { Text("Every saved surah by $name will be removed from this phone. You can stream or save them again any time.") },
            confirmButton = { TextButton({ confirmDelete = null; actions.onDeleteReciter(id) }, Modifier.testTag("offline_delete_confirm")) { Text("Remove", color = IkRed) } },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("Cancel") } })
    }
}
