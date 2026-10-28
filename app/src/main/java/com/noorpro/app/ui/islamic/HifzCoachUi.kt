package com.noorpro.app.ui.islamic

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.islamic.hifz.*
import com.noorpro.app.quran.HifzStore
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

/* 1.1.6 Hifz coach (hifz_coach_enabled). */

const val HIFZ_HELPER_NOTE = "Recitation check is a practice helper, not a teacher or scholar. Phone speech recognition can mishear — confirm your recitation and tajweed with a qualified teacher."

data class HifzHomeUi(
    val plan: HifzPlan,
    val streak: Int,
    val best: Int,
    val range: HifzRange,
    val surah: SurahInfo,
    val target: Int = 5,
)

data class HifzPracticeUi(
    val surah: SurahInfo,
    val range: HifzRange,
    val ayahs: List<HifzAyah>,
    val index: Int = 0,
    val level: Int = 0,
    val repeats: Int = 0,
    val target: Int = 5,
    val revealed: Set<Int> = emptySet(),
    val listening: Boolean = false,
    val partial: String = "",
    val result: RecitationResult? = null,
    val message: String? = null,
    val micAvailable: Boolean = true,
    val aiTips: String? = null,
    val aiLoading: Boolean = false,
    val graded: Boolean? = null,
) {
    val ayah get() = ayahs.getOrNull(index)
}

class HifzActions(
    val onBack: () -> Unit = {},
    val onLevel: (Int) -> Unit = {},
    val onRepeat: () -> Unit = {},
    val onReveal: (Int) -> Unit = {},
    val onAyah: (Int) -> Unit = {},
    val onMic: () -> Unit = {},
    val onGrade: (Boolean) -> Unit = {},
    val onTips: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

@Composable
fun HifzCoachScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { HifzStore(ctx) }
    val surahs = remember { runCatching { HifzQuran.surahs(ctx) }.getOrDefault(emptyList()) }
    val recognizer = remember { HifzRecognizer(ctx) }
    DisposableEffect(Unit) { onDispose { recognizer.stop() } }
    var range by remember { mutableStateOf(HifzRange(112, 1, 4)) }
    var target by remember { mutableIntStateOf(5) }
    var tick by remember { mutableIntStateOf(0) }
    var practice by remember { mutableStateOf<HifzPracticeUi?>(null) }
    if (surahs.isEmpty()) { IkNotice("Qur'an text is still loading", "Please try again in a moment.", Modifier.padding(16.dp)); return }
    val home = remember(range, target, tick) {
        HifzHomeUi(HifzPlanner.plan(store.allCards(), store.todayEpochDay()), store.streak(), store.bestStreak(), range, surahs[range.surah - 1], target)
    }
    fun startPractice(r: HifzRange) {
        val s = surahs[r.surah - 1]
        val rr = HifzQuran.clamp(r, s.ayahs)
        practice = HifzPracticeUi(s, rr, HifzQuran.ayahs(ctx, rr), target = target, micAvailable = recognizer.available)
    }
    var pendingMic by remember { mutableStateOf(false) }
    fun listen() {
        val p = practice ?: return
        val ayah = p.ayah ?: return
        if (p.listening) { recognizer.stop(); practice = p.copy(listening = false); return }
        practice = p.copy(listening = true, partial = "", result = null, message = null, graded = null)
        recognizer.start(
            onPartial = { t -> practice = practice?.copy(partial = t) },
            onResult = { t ->
                val res = RecitationChecker.check(ayah.text, t)
                store.grade(ayah.surah, ayah.ayah, res.passed)
                practice = practice?.copy(listening = false, result = res, partial = "", graded = res.passed, repeats = (practice?.repeats ?: 0) + 1)
                tick++
            },
            onError = { m -> practice = practice?.copy(listening = false, message = m) },
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok && pendingMic) listen() else if (!ok) practice = practice?.copy(message = "Microphone permission is needed to check your recitation. You can still grade yourself.")
        pendingMic = false
    }
    val p = practice
    if (p == null) {
        HifzHomeContent(home, surahs, onBack = { viewModel.goBack() }, onRange = { range = it }, onTarget = { target = it }, onStart = { startPractice(range) },
            onQuick = { range = it; startPractice(it) }, onReview = { g -> startPractice(HifzRange(g.surah, g.ayahs.first(), g.ayahs.last())) })
        return
    }
    BackHandler { recognizer.stop(); practice = null; tick++ }
    HifzPracticeContent(p, HifzActions(
        onBack = { recognizer.stop(); practice = null; tick++ },
        onLevel = { practice = p.copy(level = it, revealed = emptySet()) },
        onRepeat = { practice = p.copy(repeats = p.repeats + 1).let { n -> if (n.repeats >= n.target && n.level < HifzHide.MAX_LEVEL) n.copy(level = n.level + 1, repeats = 0, revealed = emptySet()) else n } },
        onReveal = { i -> practice = p.copy(revealed = p.revealed + i) },
        onAyah = { i -> recognizer.stop(); practice = p.copy(index = i, repeats = 0, revealed = emptySet(), result = null, listening = false, message = null, graded = null) },
        onMic = {
            if (!p.micAvailable) practice = p.copy(message = HifzRecognizer.UNAVAILABLE)
            else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
            else { pendingMic = true; permission.launch(Manifest.permission.RECORD_AUDIO) }
        },
        onGrade = { ok -> p.ayah?.let { store.grade(it.surah, it.ayah, ok) }; practice = p.copy(graded = ok); tick++ },
        onTips = {
            practice = p.copy(aiLoading = true)
            scope.launch {
                val raw = runCatching { AiFeatureService.generateResponse(HifzAiPrompt.tips(p.surah.name, p.range, p.result), NoorAiMode.AskNoor) }.getOrNull()
                val ok = !raw.isNullOrBlank() && raw != AiFeatureService.UNAVAILABLE_MESSAGE
                practice = practice?.copy(aiLoading = false, aiTips = if (ok) raw!!.trim() else "Noor AI isn't reachable right now. Tip: repeat each ayah 5 times while reading, then 5 times from memory, and recite it in your next salah.")
            }
        },
        onRetry = { practice = p.copy(result = null, message = null, graded = null) },
    ))
}

@Composable
fun HifzHomeContent(
    ui: HifzHomeUi, surahs: List<SurahInfo>, onBack: () -> Unit, onRange: (HifzRange) -> Unit, onTarget: (Int) -> Unit,
    onStart: () -> Unit, onQuick: (HifzRange) -> Unit, onReview: (DueGroup) -> Unit, modifier: Modifier = Modifier,
) {
    val plan = ui.plan
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("hifz_home")) {
        IkHeader("Hifz Coach", "Memorise, revise, recite", onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag("hifz_hero")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY'S REVISION", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text(if (plan.dueCount == 0) "All caught up" else "${plan.dueCount} ayah" + (if (plan.dueCount == 1) "" else "s") + " due",
                        color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${plan.memorised} memorised · ${plan.learning} in progress", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = IkGold, modifier = Modifier.size(22.dp))
                    Text("${ui.streak}", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("day streak", color = IkCream.copy(alpha = 0.75f), fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            // Next 7 days forecast.
            val max = (plan.forecast.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                plan.forecast.forEachIndexed { i, c ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height((6 + 30f * c / max).dp).clip(RoundedCornerShape(6.dp)).background(if (c > 0) IkGold.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.16f)))
                        Text("+${i + 1}d", color = IkCream.copy(alpha = 0.7f), fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            Text("Coming up in the next 7 days · best streak ${ui.best}", color = IkCream.copy(alpha = 0.65f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (plan.due.isNotEmpty()) {
            IkSection("Due for revision", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            plan.due.take(6).forEach { g ->
                val s = surahs.getOrNull(g.surah - 1)
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(16.dp))
                    .clickable { onReview(g) }.padding(12.dp).testTag("hifz_due_${g.surah}"), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(ikSoft()), contentAlignment = Alignment.Center) {
                        Text("${g.surah}", color = ikAccentText(), fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s?.name ?: "Surah ${g.surah}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ayah ${g.label}", color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("Revise", color = IkGoldDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        IkSection("New practice", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("hifz_range"), padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "hifz_surah_prev") {
                    val s = surahs[(ui.range.surah - 2 + 114) % 114]; onRange(HifzRange(s.id, 1, minOf(s.ayahs, 5)))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ui.surah.arabic.removePrefix("سُورَةُ ").trim(), color = ikAccentText(), fontFamily = QuranArabicFont, fontSize = 20.sp, maxLines = 1)
                    Text("${ui.surah.id}. ${ui.surah.name} · ${ui.surah.ayahs} ayahs", color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Stepper(Icons.AutoMirrored.Filled.KeyboardArrowRight, "hifz_surah_next") {
                    val s = surahs[ui.range.surah % 114]; onRange(HifzRange(s.id, 1, minOf(s.ayahs, 5)))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberBox("From ayah", ui.range.from, Modifier.weight(1f), "hifz_from",
                    { onRange(HifzQuran.clamp(ui.range.copy(from = ui.range.from - 1), ui.surah.ayahs)) }, { onRange(HifzQuran.clamp(ui.range.copy(from = ui.range.from + 1, to = maxOf(ui.range.to, ui.range.from + 1)), ui.surah.ayahs)) })
                NumberBox("To ayah", ui.range.to, Modifier.weight(1f), "hifz_to",
                    { onRange(HifzQuran.clamp(ui.range.copy(to = ui.range.to - 1), ui.surah.ayahs)) }, { onRange(HifzQuran.clamp(ui.range.copy(to = ui.range.to + 1), ui.surah.ayahs)) })
            }
            Spacer(Modifier.height(12.dp))
            Text("Repeat each ayah", color = stitchMutedText(), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 7, 10, 20).forEach { n -> IkPill("${n}×", n == ui.target, tag = "hifz_target_$n") { onTarget(n) } }
            }
            Spacer(Modifier.height(14.dp))
            IkButton("Start practice · ${ui.range.size} ayah" + if (ui.range.size == 1) "" else "s", Modifier.fillMaxWidth(), tag = "hifz_start", onClick = onStart)
        }
        IkSection("Quick start", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HIFZ_QUICK_PICKS.forEach { (label, r) -> IkPill(label, false, tag = "hifz_quick_${r.surah}_${r.from}") { onQuick(r) } }
        }
        Text(HIFZ_HELPER_NOTE, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).testTag("hifz_note"))
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Stepper(icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(ikSoft()).clickable(onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = ikAccentText())
    }
}

@Composable
private fun NumberBox(label: String, value: Int, modifier: Modifier, tag: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(ikSoft()).padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = stitchMutedText(), fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("−", color = ikAccentText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable(onClick = onMinus).padding(horizontal = 12.dp).testTag("${tag}_minus"))
            Text("$value", color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 40.dp))
            Text("+", color = ikAccentText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable(onClick = onPlus).padding(horizontal = 12.dp).testTag("${tag}_plus"))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HifzPracticeContent(ui: HifzPracticeUi, a: HifzActions, modifier: Modifier = Modifier) {
    val ayah = ui.ayah
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("hifz_practice")) {
        IkHeader(ui.surah.name, "Ayah ${ui.range.from}–${ui.range.to} · ${HifzHide.labels[ui.level]}", a.onBack)
        // Hide-level selector (segmented, always fits).
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ikSoft()).padding(3.dp)) {
            HifzHide.labels.forEachIndexed { i, l ->
                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(11.dp)).background(if (i == ui.level) IkGreen else Color.Transparent)
                    .clickable { a.onLevel(i) }.testTag("hifz_level_$i"), contentAlignment = Alignment.Center) {
                    Text(l, color = if (i == ui.level) Color.White else ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        if (ayah != null) {
            val result = ui.result
            IkCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("hifz_ayah"), padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${ayah.surah}:${ayah.ayah}", color = IkGoldDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${ui.index + 1} of ${ui.ayahs.size}", color = stitchMutedText(), fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val words = ayah.words
                        words.forEachIndexed { i, w ->
                            val status = result?.words?.getOrNull(i)?.status
                            val hidden = result == null && HifzHide.hidden(i, ui.level) && i !in ui.revealed
                            if (hidden) HiddenWord(w.length, Modifier.clickable { a.onReveal(i) }.testTag("hifz_hidden_$i"))
                            else {
                                val color = when (status) {
                                    WordStatus.CORRECT -> ikAccentText()
                                    WordStatus.WRONG -> IkRed
                                    WordStatus.MISSED -> IkAmber
                                    null -> stitchText()
                                }
                                val bg = when (status) {
                                    WordStatus.WRONG -> IkRed.copy(alpha = 0.10f)
                                    WordStatus.MISSED -> IkAmber.copy(alpha = 0.13f)
                                    else -> Color.Transparent
                                }
                                Text(w, color = color, fontFamily = QuranArabicFont, fontSize = 26.sp, lineHeight = 46.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 3.dp).testTag("hifz_word_$i"))
                            }
                        }
                    }
                }
            }
            // Repeat counter.
            IkCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("hifz_repeat"), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkRing(ui.repeats / ui.target.toFloat(), size = 56.dp, stroke = 6.dp, track = stitchLine(), colors = listOf(IkGreen, Color(0xFF3FA27A))) {
                        Text("${ui.repeats}/${ui.target}", color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Repetitions", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (ui.level < HifzHide.MAX_LEVEL) "After ${ui.target}, more words are hidden" else "Reciting fully from memory", color = stitchMutedText(), fontSize = 12.sp)
                    }
                    IkButton("+1", tag = "hifz_repeat_add", onClick = a.onRepeat)
                }
            }
            // Recitation check.
            if (result != null) ResultCard(result, ui.graded, a, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            else Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(if (ui.listening) IkRed else IkGreen).clickable(onClick = a.onMic).testTag("hifz_mic"), contentAlignment = Alignment.Center) {
                    Icon(if (ui.listening) Icons.Default.Stop else Icons.Default.Mic, if (ui.listening) "Stop" else "Check my recitation", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(when {
                    ui.listening && ui.partial.isNotBlank() -> ui.partial
                    ui.listening -> "Listening… recite the ayah"
                    else -> "Tap to check your recitation"
                }, color = if (ui.listening) ikAccentText() else stitchMutedText(), fontSize = 13.sp, textAlign = TextAlign.Center,
                    fontFamily = if (ui.listening && ui.partial.isNotBlank()) QuranArabicFont else null, modifier = Modifier.padding(horizontal = 24.dp))
                if (ui.listening) Unit else if (ui.graded == null) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IkPill("I knew it", false, icon = Icons.Default.Check, tag = "hifz_self_good") { a.onGrade(true) }
                    IkPill("Needs work", false, icon = Icons.Default.Replay, tag = "hifz_self_again") { a.onGrade(false) }
                } else Text(if (ui.graded) "Saved · next review scheduled" else "Saved · we'll bring it back tomorrow", color = IkGoldDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            }
            ui.message?.let { IkNotice("Recitation check", it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), tag = "hifz_message") }
            // Ayah navigation.
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IkButton("Previous", primary = false, enabled = ui.index > 0, tag = "hifz_prev") { a.onAyah(ui.index - 1) }
                Spacer(Modifier.weight(1f))
                IkButton("Next ayah", enabled = ui.index < ui.ayahs.size - 1, tag = "hifz_next") { a.onAyah(ui.index + 1) }
            }
            // Optional Noor AI tips.
            IkCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("hifz_ai"), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkIconTile(Icons.Default.AutoAwesome, size = 34.dp, tint = IkGoldDeep, bg = IkGold.copy(alpha = 0.16f))
                    Spacer(Modifier.width(10.dp))
                    Text("Memorisation tips from Noor AI", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (ui.aiTips == null) IkPill(if (ui.aiLoading) "Asking…" else "Ask", false, tag = "hifz_ai_ask") { if (!ui.aiLoading) a.onTips() }
                }
                ui.aiTips?.let { Text(it, color = stitchText(), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 10.dp)) }
            }
        }
        Text(HIFZ_HELPER_NOTE, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun HiddenWord(len: Int, modifier: Modifier) {
    val w = (14 + len * 7).coerceAtMost(110)
    Box(modifier.padding(vertical = 10.dp).width(w.dp).height(26.dp).clip(RoundedCornerShape(8.dp))
        .background(if (isStitchLight()) IkMint else Color(0xFF1B3A31)).border(1.dp, IkGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { Box(Modifier.size(4.dp).clip(CircleShape).background(ikAccentText().copy(alpha = 0.5f))) } }
    }
}

@Composable
private fun ResultCard(r: RecitationResult, graded: Boolean?, a: HifzActions, modifier: Modifier) {
    IkCard(modifier.testTag("hifz_result"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkRing(r.score, size = 64.dp, stroke = 7.dp, track = stitchLine(), colors = if (r.passed) listOf(IkGreen, Color(0xFF3FA27A)) else listOf(IkAmber, IkGold)) {
                Text("${(r.score * 100).toInt()}%", color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (r.passed) "MashaAllah, well recalled" else "Nearly there — try again", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${r.correct} of ${r.words.size} words matched", color = stitchMutedText(), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Key(ikAccentText(), "Matched ${r.correct}"); Key(IkAmber, "Missed ${r.missed}"); Key(IkRed, "Different ${r.wrong}")
        }
        if (graded != null) Text(if (graded) "Saved · next review scheduled" else "Saved · we'll bring it back tomorrow", color = IkGoldDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(10.dp))
        IkButton("Recite again", Modifier.fillMaxWidth(), icon = Icons.Default.Replay, primary = false, tag = "hifz_retry", onClick = a.onRetry)
    }
}

@Composable private fun Key(c: Color, t: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(5.dp)); Text(t, color = stitchMutedText(), fontSize = 11.sp)
    }
}
