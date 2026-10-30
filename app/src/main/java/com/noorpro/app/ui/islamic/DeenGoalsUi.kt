package com.noorpro.app.ui.islamic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.data.PrayerDatabase
import com.noorpro.app.islamic.deen.*
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DeenTodayUi(val today: DeenDay, val cfg: DeenGoalsConfig, val streak: Int, val week: DeenWeekSummary)

sealed interface DeenAiState { data object Idle : DeenAiState; data object Loading : DeenAiState; data class Text(val text: String) : DeenAiState; data object Unavailable : DeenAiState }

internal fun goalIcon(g: DeenGoal): ImageVector = when (g) {
    DeenGoal.SALAH -> Icons.Outlined.Favorite
    DeenGoal.QURAN -> Icons.AutoMirrored.Outlined.MenuBook
    DeenGoal.MORNING -> Icons.Outlined.WbSunny
    DeenGoal.EVENING -> Icons.Outlined.WbTwilight
    DeenGoal.SADAQAH -> Icons.Outlined.VolunteerActivism
    DeenGoal.DUA -> Icons.Outlined.NightsStay
}

@Composable
fun DeenGoalsScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val store = remember { DeenGoalsStore(ctx) }
    val today = remember { LocalDate.now().toEpochDay() }
    val logsFlow = remember {
        PrayerDatabase.getDatabase(ctx).prayerTrackerDao()
            .getLogsBetweenDates(LocalDate.ofEpochDay(today - DeenGoalsStore.KEEP_DAYS).toString(), LocalDate.ofEpochDay(today).toString())
    }
    val logs by logsFlow.collectAsState(initial = emptyList())
    var tick by remember { mutableIntStateOf(0) }
    var cfg by remember { mutableStateOf(store.config()) }
    val days = remember(logs, tick) { store.merged(logs) }
    val ui = remember(days, cfg) {
        val streak = DeenGoalsLogic.streak(days, cfg, today)
        DeenTodayUi(days[today] ?: DeenDay(today), cfg, streak, DeenGoalsLogic.week(days, cfg, today))
    }
    LaunchedEffect(ui) { store.saveWidget(DeenGoalsLogic.snapshot(ui.today, ui.cfg, ui.streak)) }
    var page by rememberSaveable { mutableStateOf("today") }
    var ai by remember { mutableStateOf<DeenAiState>(DeenAiState.Idle) }
    val scope = rememberCoroutineScope()
    BackHandler(page != "today") { page = "today" }
    when (page) {
        "week" -> DeenWeekContent(ui.week, ai, onBack = { page = "today" }) {
            ai = DeenAiState.Loading
            scope.launch {
                val raw = runCatching { AiFeatureService.generateResponse(DeenGoalsLogic.aiPrompt(ui.week), NoorAiMode.AskNoor) }.getOrNull()
                ai = if (!raw.isNullOrBlank() && raw != AiFeatureService.UNAVAILABLE_MESSAGE) DeenAiState.Text(raw.trim()) else DeenAiState.Unavailable
            }
        }
        "settings" -> DeenGoalsSettingsContent(cfg, onBack = { page = "today" }) { c -> store.setConfig(c); cfg = store.config() }
        else -> DeenTodayContent(ui, onBack = { viewModel.goBack() }, onWeek = { page = "week" }, onSettings = { page = "settings" },
            onSalah = { i -> viewModel.toggleDailyPrayer(SALAH_NAMES[i]) },
            onPages = { d -> store.addPages(today, d); tick++ },
            onToggle = { g -> store.toggle(today, g); tick++ })
    }
}

@Composable
private fun CheckDot(done: Boolean, size: Int = 30, partial: Float = 0f) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(if (done) IkGreen else Color.Transparent)
        .border(2.dp, if (done) IkGreen else if (partial > 0f) IkGold else stitchLine(), CircleShape), contentAlignment = Alignment.Center) {
        if (done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size((size * 0.6f).dp))
    }
}

@Composable
fun DeenTodayContent(ui: DeenTodayUi, onBack: () -> Unit, onWeek: () -> Unit, onSettings: () -> Unit,
                     onSalah: (Int) -> Unit, onPages: (Int) -> Unit, onToggle: (DeenGoal) -> Unit) {
    val cfg = ui.cfg; val d = ui.today
    val done = DeenGoalsLogic.completed(d, cfg)
    Column(Modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("deen_today")) {
        IkHeader("Daily Deen", LocalDate.ofEpochDay(d.day).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)), onBack) {
            Box(Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onSettings).testTag("deen_settings"), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, "Goals", tint = stitchText())
            }
        }
        IkHero(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IkRing(DeenGoalsLogic.score(d, cfg), size = 104.dp, stroke = 9.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$done/${cfg.goals.size}", color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("goals", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("TODAY", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text(if (done == cfg.goals.size && done > 0) "All done — Alhamdulillah" else "Small deeds, done with love",
                        color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 23.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(DeenGoalsLogic.streakLine(ui.streak, done), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 16.sp,
                        modifier = Modifier.testTag("deen_streak"))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cfg.goals.forEach { g ->
                when (g) {
                    DeenGoal.SALAH -> IkCard(Modifier.testTag("deen_goal_salah")) {
                        GoalTitle(g, "${DeenGoalsLogic.salahCount(d.salah)} of 5", DeenGoalsLogic.complete(g, d, cfg))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SALAH_NAMES.forEachIndexed { i, n ->
                                val on = d.salah and (1 shl i) != 0
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) IkGreen else ikSoft())
                                    .clickable { onSalah(i) }.padding(vertical = 8.dp).testTag("deen_salah_$i"), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(if (on) Icons.Default.Check else Icons.Outlined.Favorite, null, tint = if (on) Color.White else ikAccentText().copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    Text(n, color = if (on) Color.White else ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text("Synced with your prayer tracker", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    DeenGoal.QURAN -> IkCard(Modifier.testTag("deen_goal_quran")) {
                        GoalTitle(g, "${d.pages} of ${cfg.quranPages} pages", DeenGoalsLogic.complete(g, d, cfg))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(ikSoft())) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(DeenGoalsLogic.progress(g, d, cfg)).clip(RoundedCornerShape(4.dp)).background(IkGold))
                            }
                            Spacer(Modifier.width(12.dp))
                            StepButton(Icons.Default.Remove, "deen_pages_minus", d.pages > 0) { onPages(-1) }
                            Text("${d.pages}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp).testTag("deen_pages"))
                            StepButton(Icons.Default.Add, "deen_pages_plus", true) { onPages(1) }
                        }
                    }
                    else -> {
                        val ok = g in d.done
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, if (ok) IkGreen.copy(alpha = 0.5f) else stitchLine(), RoundedCornerShape(20.dp))
                            .clickable { onToggle(g) }.padding(14.dp).testTag("deen_goal_${g.id}"), verticalAlignment = Alignment.CenterVertically) {
                            IkIconTile(goalIcon(g))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(g.sub, color = stitchMutedText(), fontSize = 12.sp)
                            }
                            CheckDot(ok)
                        }
                    }
                }
            }
            if (cfg.goals.isEmpty()) IkNotice("No goals switched on", "Pick the goals that suit you in settings — even one is enough.", tag = "deen_none")
            IkSection("This week")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp))
                .clickable(onClick = onWeek).padding(14.dp).testTag("deen_week_open"), verticalAlignment = Alignment.Bottom) {
                ui.week.days.forEachIndexed { i, day ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        WeekBar(ui.week.dayScores[i], 46)
                        Text(LocalDate.ofEpochDay(day).dayOfWeek.name.take(1), color = if (day == d.day) ikAccentText() else stitchMutedText(), fontSize = 11.sp,
                            fontWeight = if (day == d.day) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Icon(Icons.Outlined.BarChart, null, tint = ikAccentText(), modifier = Modifier.padding(start = 6.dp).size(20.dp))
            }
            Text(ui.week.headline, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GoalTitle(g: DeenGoal, status: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IkIconTile(goalIcon(g))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(status, color = if (done) ikAccentText() else stitchMutedText(), fontSize = 12.sp, fontWeight = if (done) FontWeight.Bold else FontWeight.Normal)
        }
        CheckDot(done)
    }
}

@Composable
private fun StepButton(icon: ImageVector, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(CircleShape).background(if (enabled) ikSoft() else Color.Transparent).border(1.dp, stitchLine(), CircleShape)
        .clickable(enabled = enabled, onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (enabled) ikAccentText() else stitchMutedText(), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun WeekBar(score: Float, maxH: Int) {
    Box(Modifier.width(14.dp).height(maxH.dp).clip(RoundedCornerShape(7.dp)).background(ikSoft()), contentAlignment = Alignment.BottomCenter) {
        if (score > 0f) Box(Modifier.fillMaxWidth().fillMaxHeight(score.coerceIn(0.12f, 1f)).clip(RoundedCornerShape(7.dp)).background(if (score >= 1f) IkGold else IkGreen))
    }
}

@Composable
fun DeenWeekContent(s: DeenWeekSummary, ai: DeenAiState, onBack: () -> Unit, modifier: Modifier = Modifier, onAskAi: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("deen_week")) {
        IkHeader("Your week", "${LocalDate.ofEpochDay(s.days.first()).format(fmt)} – ${LocalDate.ofEpochDay(s.days.last()).format(fmt)}", onBack)
        IkHero(Modifier.padding(horizontal = 16.dp)) {
            Text("WEEKLY REFLECTION", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(4.dp))
            Text(s.headline, color = Color.White, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 23.sp, modifier = Modifier.testTag("deen_headline"))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("${s.activeDays}/7" to "days", "${s.salahPrayed}" to "salah", "${s.pagesRead}" to "pages").forEach { (v, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.10f)).padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(l, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IkSection("Goal by goal")
            IkCard {
                Row(Modifier.padding(start = 132.dp)) {
                    s.days.forEach { Text(LocalDate.ofEpochDay(it).dayOfWeek.name.take(1), color = stitchMutedText(), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                }
                s.perGoal.forEach { gw ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("deen_week_${gw.goal.id}"), verticalAlignment = Alignment.CenterVertically) {
                        Text(gw.goal.title, color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.width(132.dp).padding(end = 6.dp))
                        gw.days.forEach { ok ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(if (ok) IkGreen else ikSoft()))
                            }
                        }
                    }
                }
                Text("Consistency matters more than quantity — every square is a deed.", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
            IkSection("Encouragement")
            when (ai) {
                is DeenAiState.Text -> IkCard(Modifier.testTag("deen_ai_text")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = IkGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text("From Noor AI", color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(ai.text, color = stitchText(), fontSize = 14.sp, lineHeight = 21.sp)
                    Text("AI can make mistakes — verify any hadith with a scholar or trusted source.", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
                DeenAiState.Unavailable -> IkNotice("Noor AI isn't reachable right now", "Your summary above is all saved on this phone. Try again later if you'd like a few kind words.", offline = true, tag = "deen_ai_off")
                else -> Column {
                    Text("Optional: ask Noor AI for a few gentle words about your week. Only the counts above are shared.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    IkButton(if (ai == DeenAiState.Loading) "Asking Noor AI…" else "Encourage me", Modifier.fillMaxWidth(), Icons.Outlined.AutoAwesome,
                        enabled = ai != DeenAiState.Loading, tag = "deen_ai_ask", onClick = onAskAi)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DeenGoalsSettingsContent(cfg: DeenGoalsConfig, onBack: () -> Unit, modifier: Modifier = Modifier, onChange: (DeenGoalsConfig) -> Unit) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("deen_settings_page")) {
        IkHeader("Your goals", "Choose what fits your life", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DeenGoal.entries.forEach { g ->
                val on = g in cfg.enabled
                IkCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IkIconTile(goalIcon(g)); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(g.sub, color = stitchMutedText(), fontSize = 12.sp)
                        }
                        Switch(on, { onChange(cfg.copy(enabled = if (on) cfg.enabled - g else cfg.enabled + g)) }, Modifier.testTag("deen_switch_${g.id}"),
                            colors = SwitchDefaults.colors(checkedTrackColor = IkGreen, checkedThumbColor = Color.White,
                                uncheckedTrackColor = if (isStitchLight()) Color(0xFFE5E7E4) else Color(0xFF2A3430), uncheckedBorderColor = stitchLine()))
                    }
                    if (g == DeenGoal.QURAN && on) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pages a day", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.weight(1f))
                            StepButton(Icons.Default.Remove, "deen_target_minus", cfg.quranPages > DeenGoalsLogic.MIN_PAGES_TARGET) { onChange(cfg.copy(quranPages = cfg.quranPages - 1)) }
                            Text("${cfg.quranPages}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp).testTag("deen_target"))
                            StepButton(Icons.Default.Add, "deen_target_plus", cfg.quranPages < DeenGoalsLogic.MAX_PAGES_TARGET) { onChange(cfg.copy(quranPages = cfg.quranPages + 1)) }
                        }
                    }
                }
            }
            IkNotice("Gentle by design", "A day counts when you do anything at all, and one quiet day never breaks your streak. Everything stays on this phone.", tag = "deen_gentle")
            Spacer(Modifier.height(24.dp))
        }
    }
}
