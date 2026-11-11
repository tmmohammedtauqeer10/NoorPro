package com.noorpro.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.quiz.QuizCat
import com.noorpro.app.data.quiz.QuizMadhhab
import com.noorpro.app.data.quiz.QuizQuestion2
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.islamic.*
import com.noorpro.app.ui.theme.LibreCaslon

// ---------------------------------------------------------------- state

data class QuizHomeUi(
    val lang: String = "en",
    val madhhab: String = QuizMadhhab.HANAFI,
    val streak: Int = 0,
    val xp: Int = 0,
    val accuracy: Int? = null,
    val dailyScore: Int? = null,
    val mistakes: Int = 0,
    val counts: Map<QuizCat, Int> = emptyMap(),
    val best: Map<QuizCat, Int> = emptyMap(),
    val level: Int? = null,
    val timer: Boolean = false,
    val kids: Boolean = false,
    val aiCat: QuizCat = QuizCat.SEERAH,
    val aiBusy: Boolean = false,
    val aiNote: String? = null,
    val aiCount: Int = 0,
)

class QuizHomeActions(
    val onBack: () -> Unit = {},
    val onDaily: () -> Unit = {},
    val onCategory: (QuizCat) -> Unit = {},
    val onReview: () -> Unit = {},
    val onLevel: (Int?) -> Unit = {},
    val onTimer: (Boolean) -> Unit = {},
    val onMadhhab: (String) -> Unit = {},
    val onLang: (String) -> Unit = {},
    val onAiCat: (QuizCat) -> Unit = {},
    val onGenerate: () -> Unit = {},
)

data class QuizPlayUi(
    val title: String,
    val question: QuizQuestion2,
    val index: Int,
    val total: Int,
    val lang: String = "en",
    val madhhab: String = QuizMadhhab.HANAFI,
    /** null = not answered yet, -1 = time ran out. */
    val picked: Int? = null,
    val timeLeft: Int? = null,
    val score: Int = 0,
    val translating: Boolean = false,
    val untranslated: Boolean = false,
)

class QuizPlayActions(val onPick: (Int) -> Unit = {}, val onNext: () -> Unit = {}, val onQuit: () -> Unit = {})

data class QuizResultUi(
    val title: String,
    val correct: Int,
    val total: Int,
    val xp: Int,
    val lang: String = "en",
    val madhhab: String = QuizMadhhab.HANAFI,
    val streak: Int? = null,
    val wrong: List<QuizQuestion2> = emptyList(),
)

class QuizResultActions(val onReview: () -> Unit = {}, val onAgain: () -> Unit = {}, val onDone: () -> Unit = {})

// ---------------------------------------------------------------- helpers

internal fun quizIcon(c: QuizCat): ImageVector = when (c) {
    QuizCat.QURAN -> Icons.AutoMirrored.Outlined.MenuBook
    QuizCat.SEERAH -> Icons.Outlined.Mosque
    QuizCat.HADITH -> Icons.AutoMirrored.Outlined.LibraryBooks
    QuizCat.FIQH -> Icons.Outlined.Balance
    QuizCat.AQEEDAH -> Icons.Outlined.WbSunny
    QuizCat.HISTORY -> Icons.Outlined.HistoryEdu
    QuizCat.AWLIYA -> Icons.Outlined.Spa
    QuizCat.DUAS -> Icons.Outlined.VolunteerActivism
    QuizCat.KIDS -> Icons.Outlined.ChildCare
}

@Composable
private fun QuizDirection(lang: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides if (QuizStrings.rtl(lang)) LayoutDirection.Rtl else LayoutDirection.Ltr, content = content)
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier, tag: String) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
        .padding(vertical = 12.dp, horizontal = 10.dp).testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = IkGoldDeep, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
        Text(label, color = stitchMutedText(), fontSize = 11.sp, maxLines = 1)
    }
}

// ---------------------------------------------------------------- home

@Composable
fun QuizHomeContent(ui: QuizHomeUi, actions: QuizHomeActions) {
    val l = ui.lang
    fun s(k: String) = QuizStrings.s(k, l)
    QuizDirection(l) {
        Column(Modifier.ikScreen().testTag("quiz_home")) {
            IkHeader(s("title"), s("subtitle"), onBack = actions.onBack)
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(Icons.Outlined.LocalFireDepartment, "${ui.streak}", s("streak"), Modifier.weight(1f), "quiz_stat_streak")
                        StatTile(Icons.Outlined.Star, "${ui.xp}", s("xp"), Modifier.weight(1f), "quiz_stat_xp")
                        StatTile(Icons.Outlined.TaskAlt, ui.accuracy?.let { "$it%" } ?: "—", s("accuracy"), Modifier.weight(1f), "quiz_stat_accuracy")
                    }
                }
                item {
                    IkHero(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WbTwilight, null, tint = IkGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s("daily").uppercase(), color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(if (ui.kids) s("kids_note") else s("daily_body"), color = Color.White, fontSize = 16.sp, lineHeight = 22.sp, fontFamily = LibreCaslon)
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).clickable(onClick = actions.onDaily)
                                .padding(horizontal = 18.dp, vertical = 11.dp).testTag("quiz_daily_start"), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (ui.dailyScore != null) Icons.Outlined.Replay else Icons.Outlined.PlayArrow, null, tint = IkGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (ui.dailyScore != null) s("play_again") else s("start"), color = IkGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            if (ui.dailyScore != null) {
                                Spacer(Modifier.width(12.dp))
                                Text("${s("done_today")} · ${ui.dailyScore}/10", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.testTag("quiz_daily_done"))
                            }
                        }
                    }
                }
                item {
                    IkCard(Modifier.padding(horizontal = 16.dp), padding = 14.dp) {
                        Text(s("level"), color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf<Int?>(null, 1, 2, 3).forEach { lv -> IkPill(QuizStrings.level(lv, l), ui.level == lv, tag = "quiz_level_${lv ?: 0}") { actions.onLevel(lv) } }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, tint = ikAccentText(), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(s("timer"), color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(ui.timer, actions.onTimer, Modifier.testTag("quiz_timer"),
                                colors = SwitchDefaults.colors(checkedTrackColor = IkGreen, checkedThumbColor = Color.White))
                        }
                        if (!ui.kids) {
                            Spacer(Modifier.height(10.dp))
                            Text(s("madhhab"), color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                QuizMadhhab.all.forEach { m -> IkPill(QuizMadhhab.label(m), ui.madhhab == m, tag = "quiz_madhhab_$m") { actions.onMadhhab(m) } }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(s("language"), color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuizStrings.languages.forEach { (code, name) -> IkPill(name, ui.lang == code, tag = "quiz_lang_$code") { actions.onLang(code) } }
                        }
                    }
                }
                item { IkSection(s("categories"), Modifier.padding(horizontal = 16.dp)) }
                val cats = if (ui.kids) listOf(QuizCat.KIDS) else QuizCat.entries.toList()
                items(cats.chunked(2)) { pair ->
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { c -> CategoryCard(c, ui, Modifier.weight(1f)) { actions.onCategory(c) } }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item {
                    IkCard(Modifier.padding(horizontal = 16.dp).clickable(onClick = actions.onReview).testTag("quiz_review"), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IkIconTile(Icons.Outlined.RestartAlt, size = 42.dp, tint = IkAmber, bg = IkAmber.copy(alpha = 0.12f))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s("review"), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(if (ui.mistakes == 0) s("no_mistakes") else s("review_body"), color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
                            }
                            if (ui.mistakes > 0) IkTag("${ui.mistakes}", bg = IkAmber.copy(alpha = 0.14f), fg = IkAmber)
                        }
                    }
                }
                if (!ui.kids) item {
                    IkCard(Modifier.padding(horizontal = 16.dp).testTag("quiz_ai_card"), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IkIconTile(Icons.Outlined.AutoAwesome, size = 42.dp, tint = IkGoldDeep, bg = IkGold.copy(alpha = 0.14f))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s("ai_title"), color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(s("ai_body"), color = stitchMutedText(), fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuizCat.entries.filter { it != QuizCat.KIDS }.forEach { c -> IkPill(QuizStrings.cat(c, l), ui.aiCat == c, tag = "quiz_ai_cat_${c.key}") { actions.onAiCat(c) } }
                        }
                        Spacer(Modifier.height(10.dp))
                        IkButton(if (ui.aiBusy) s("generating") else s("generate"), Modifier.fillMaxWidth(), icon = Icons.Outlined.AutoAwesome,
                            enabled = !ui.aiBusy, tag = "quiz_generate", onClick = actions.onGenerate)
                        if (ui.aiNote != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(ui.aiNote, color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.testTag("quiz_ai_note"))
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.OfflinePin, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${s("offline_ok")} · ${ui.counts.values.sum()} ${s("questions")}", color = stitchMutedText(), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(c: QuizCat, ui: QuizHomeUi, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp))
        .clickable(onClick = onClick).padding(14.dp).testTag("quiz_cat_${c.key}")) {
        IkIconTile(quizIcon(c), size = 40.dp)
        Spacer(Modifier.height(10.dp))
        Text(QuizStrings.cat(c, ui.lang), color = stitchText(), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (c == QuizCat.FIQH) "${QuizStrings.catBlurb(c)} · ${QuizMadhhab.label(ui.madhhab)}" else QuizStrings.catBlurb(c),
            color = stitchMutedText(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${ui.counts[c] ?: 0} ${QuizStrings.s("questions", ui.lang)}", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.weight(1f))
            ui.best[c]?.let { Text("${QuizStrings.s("best", ui.lang)} $it/10", color = IkGoldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ---------------------------------------------------------------- play

@Composable
fun QuizPlayContent(ui: QuizPlayUi, actions: QuizPlayActions) {
    val l = ui.lang
    fun s(k: String) = QuizStrings.s(k, l)
    val q = ui.question
    val right = q.answerFor(ui.madhhab)
    val answered = ui.picked != null
    QuizDirection(l) {
        Column(Modifier.ikScreen().testTag("quiz_play")) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = actions.onQuit).testTag("quiz_quit"), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, s("quit"), tint = stitchText())
                }
                Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    Text(ui.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${s("question")} ${ui.index + 1} ${s("of")} ${ui.total}", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.testTag("quiz_progress_text"))
                }
                if (ui.timeLeft != null) {
                    val low = ui.timeLeft <= 5 && !answered
                    Row(Modifier.clip(RoundedCornerShape(50)).background(if (low) IkRed.copy(alpha = 0.12f) else ikSoft()).padding(horizontal = 10.dp, vertical = 6.dp).testTag("quiz_timer_left"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Timer, null, tint = if (low) IkRed else ikAccentText(), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${ui.timeLeft}s", color = if (low) IkRed else ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(ui.total) { i ->
                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50))
                        .background(when { i < ui.index || (i == ui.index && answered) -> IkGreen; i == ui.index -> IkGold; else -> stitchLine() }))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IkTag(QuizStrings.cat(q.cat, l))
                    IkTag(QuizStrings.level(q.level, l), bg = IkGold.copy(alpha = 0.14f), fg = IkGoldDeep)
                    if (q.hasMadhhabVariants) IkTag("${QuizMadhhab.label(ui.madhhab)} ${s("view_of")}", bg = IkGreen.copy(alpha = 0.10f), fg = ikAccentText())
                    if (q.generated) IkTag(s("ai_badge"), bg = IkGold.copy(alpha = 0.14f), fg = IkGoldDeep)
                }
                Spacer(Modifier.height(14.dp))
                Text(q.textFor(l), color = stitchText(), fontSize = 21.sp, lineHeight = 29.sp, fontFamily = LibreCaslon, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("quiz_question"))
                if (ui.translating) {
                    Spacer(Modifier.height(6.dp)); Text(s("translating"), color = stitchMutedText(), fontSize = 12.sp)
                } else if (ui.untranslated && l != "en") {
                    Spacer(Modifier.height(6.dp)); Text(s("english_only"), color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.testTag("quiz_untranslated"))
                }
                Spacer(Modifier.height(18.dp))
                q.optionsFor(l).forEachIndexed { i, opt ->
                    val state = when {
                        !answered -> 0
                        i == right -> 1
                        i == ui.picked -> 2
                        else -> 3
                    }
                    val border = when (state) { 1 -> IkGreen; 2 -> IkRed; else -> stitchLine() }
                    val bg = when (state) { 1 -> IkGreen.copy(alpha = 0.10f); 2 -> IkRed.copy(alpha = 0.08f); else -> ikCard() }
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                        .border(if (state == 1 || state == 2) 2.dp else 1.dp, border, RoundedCornerShape(16.dp))
                        .clickable(enabled = !answered) { actions.onPick(i) }.padding(horizontal = 14.dp, vertical = 13.dp).testTag("quiz_option_$i"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(when (state) { 1 -> IkGreen; 2 -> IkRed; else -> ikSoft() }), contentAlignment = Alignment.Center) {
                            when (state) {
                                1 -> Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                2 -> Icon(Icons.Outlined.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                else -> Text(('A' + i).toString(), color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(opt, color = if (state == 3) stitchMutedText() else stitchText(), fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                    }
                }
                if (answered) {
                    val ok = ui.picked == right
                    Spacer(Modifier.height(4.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikSoft()).padding(14.dp).testTag("quiz_explanation")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Info, null, tint = if (ok) IkGreen else IkAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(when { ok -> s("correct"); ui.picked == -1 -> s("time_up"); else -> s("wrong") }, color = if (ok) ikAccentText() else IkAmber,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.testTag("quiz_feedback"))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(q.explanationFor(ui.madhhab, l), color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = IkGoldDeep, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${s("source")}: ${q.source}", color = IkGoldDeep, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("quiz_source"))
                        }
                        if (l != "en" && q.isTranslated(l)) {
                            Spacer(Modifier.height(6.dp)); Text(s("machine_tr"), color = stitchMutedText(), fontSize = 11.sp)
                        }
                    }
                }
            }
            if (answered) {
                IkButton(if (ui.index + 1 >= ui.total) s("finish") else s("next"), Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    tag = "quiz_next", onClick = actions.onNext)
            }
        }
    }
}

// ---------------------------------------------------------------- result

@Composable
fun QuizResultContent(ui: QuizResultUi, actions: QuizResultActions) {
    val l = ui.lang
    fun s(k: String) = QuizStrings.s(k, l)
    QuizDirection(l) {
        LazyColumn(Modifier.ikScreen().testTag("quiz_result"), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { IkHeader(s("result"), ui.title, onBack = actions.onDone) }
            item {
                IkHero(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IkRing(if (ui.total == 0) 0f else ui.correct / ui.total.toFloat(), size = 104.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${ui.correct}/${ui.total}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon,
                                    modifier = Modifier.testTag("quiz_score"))
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column {
                            Text("+${ui.xp} ${s("xp")}", color = IkGold, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("quiz_xp_earned"))
                            Text(s("earned"), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            if (ui.streak != null) {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = IkGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${ui.streak} · ${s("streak")}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("quiz_result_streak"))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IkButton(s("play_again"), Modifier.weight(1f), icon = Icons.Outlined.Replay, primary = false, tag = "quiz_again", onClick = actions.onAgain)
                    IkButton(s("done"), Modifier.weight(1f), tag = "quiz_done", onClick = actions.onDone)
                }
            }
            if (ui.wrong.isNotEmpty()) {
                item {
                    IkSection(s("to_review"), Modifier.padding(horizontal = 16.dp)) {
                        Text(s("review"), color = ikAccentText(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = actions.onReview).padding(horizontal = 8.dp, vertical = 4.dp).testTag("quiz_result_review"))
                    }
                }
                items(ui.wrong, key = { it.id }) { q ->
                    IkCard(Modifier.padding(horizontal = 16.dp), padding = 14.dp) {
                        Text(q.textFor(l), color = stitchText(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Check, null, tint = IkGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(q.optionsFor(l)[q.answerFor(ui.madhhab)], color = ikAccentText(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(q.source, color = stitchMutedText(), fontSize = 11.sp)
                    }
                }
            } else {
                item {
                    Text(s("all_correct"), color = ikAccentText(), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
