package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.CourseExample
import com.noorpro.app.data.CourseLesson
import com.noorpro.app.data.CourseQuiz
import com.noorpro.app.data.QuranCourseRepository
import com.noorpro.app.ui.components.QuranArabicFont

@Composable
internal fun ArabicText(text: String, size: Int, color: Color, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(text, fontFamily = QuranArabicFont, fontSize = size.sp, lineHeight = (size * 1.75).sp, color = color, textAlign = TextAlign.Start, modifier = modifier)
    }
}

@Composable
fun CourseLessonView(
    lesson: CourseLesson, levelTitle: String, position: Int, total: Int, isDone: Boolean, bestScore: Int,
    onBack: () -> Unit, onPrev: (() -> Unit)?, onNext: (() -> Unit)?, onToggleDone: () -> Unit,
    onQuizScore: (Int) -> Unit, onOpenWordByWord: (Int) -> Unit,
) {
    val p = coursePalette()
    val scroll = rememberScrollState()
    LaunchedEffect(lesson.id) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        CourseTopBar(lesson.title, "$levelTitle · ${position + 1}/$total", onBack)
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Section(p, "Goal") { Text(lesson.goal, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
            Text(lesson.en, color = p.text, fontSize = 15.sp, lineHeight = 23.sp)
            if (lesson.ur.isNotBlank()) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(lesson.ur, color = p.muted, fontSize = 16.sp, lineHeight = 28.sp, fontFamily = QuranArabicFont, modifier = Modifier.fillMaxWidth())
            }
            if (lesson.points.isNotEmpty()) Section(p, "Key points") {
                lesson.points.forEach { Row(Modifier.padding(vertical = 2.dp)) { Text("•", color = CourseGold, modifier = Modifier.width(14.dp)); Text(it, color = p.text, fontSize = 14.sp, lineHeight = 21.sp) } }
            }
            if (lesson.letters.isNotEmpty()) LettersGrid(p, lesson)
            if (lesson.examples.isNotEmpty()) {
                Text("Examples", color = p.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val rules = lesson.examples.mapNotNull { it.rule }.distinct()
                if (rules.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(rules) { r -> Row(Modifier.clip(RoundedCornerShape(50)).background(p.chip).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ruleColor(r))); Spacer(Modifier.width(5.dp)); Text(ruleName(r), color = p.text, fontSize = 11.sp)
                    } }
                }
                lesson.examples.forEach { ExampleCard(p, it) }
            }
            when (lesson.kind) {
                "read", "hifz", "selftest" -> CoursePractice(lesson, onOpenWordByWord)
            }
            if (lesson.quiz.isNotEmpty()) QuizBlock(p, lesson.id, lesson.quiz, bestScore, onQuizScore)
            Button(
                onClick = onToggleDone, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDone) p.chip else CourseGreen, contentColor = if (isDone) CourseGreen else Color.White),
            ) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(6.dp)); Text(if (isDone) "Completed (tap to undo)" else "Mark lesson complete") }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            OutlinedButton(onClick = { onPrev?.invoke() }, enabled = onPrev != null) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(4.dp)); Text("Previous") }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onNext?.invoke() }, enabled = onNext != null, colors = ButtonDefaults.buttonColors(containerColor = CourseGreen)) { Text("Next"); Spacer(Modifier.width(4.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
        }
    }
}

@Composable
private fun Section(p: CoursePalette, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(title.uppercase(), color = CourseGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun LettersGrid(p: CoursePalette, lesson: CourseLesson) {
    var sel by remember(lesson.id) { mutableStateOf(lesson.letters.first()) }
    Section(p, "Tap a letter") {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.chip).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArabicText(sel.ar, 64, CourseGreen)
            Text(sel.name, color = p.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Sound: ${sel.sound}", color = p.muted, fontSize = 13.sp)
            Text("Makhraj: ${sel.group}", color = p.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text("No bundled audio for single letters; learn the sound from a teacher.", color = p.muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(10.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lesson.letters.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { l ->
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (l == sel) CourseGreen else p.card).border(1.dp, p.line, RoundedCornerShape(10.dp))
                                    .clickable { sel = l },
                                contentAlignment = Alignment.Center,
                            ) { Text(l.ar, fontFamily = QuranArabicFont, fontSize = 24.sp, color = if (l == sel) Color.White else p.text) }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExampleCard(p: CoursePalette, e: CourseExample) {
    val key = if (e.surah != null) "${e.surah}:${e.ayah}" else null
    val playing = key != null && CourseAudio.playingKey == key
    val c = ruleColor(e.rule)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card).border(1.dp, if (e.rule != null) c.copy(alpha = 0.5f) else p.line, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            ArabicText(e.ar, if (e.ar.length > 40) 24 else 32, if (e.rule != null) c else p.text, Modifier.fillMaxWidth())
            if (e.label.isNotBlank()) Text(e.label, color = p.text, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (e.rule != null) Text(ruleName(e.rule) + "  ", color = c, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(e.ref ?: "Practice text · no audio", color = p.muted, fontSize = 11.sp)
            }
        }
        if (e.surah != null && e.ayah != null) {
            IconButton(
                onClick = { if (playing) CourseAudio.stop() else CourseAudio.play(e.surah, e.ayah) },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(CourseGreen.copy(alpha = 0.12f)),
            ) { Icon(if (playing) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp, if (playing) "Stop" else "Play ayah (Husary)", tint = CourseGreen) }
        }
    }
}

@Composable
private fun QuizBlock(p: CoursePalette, lessonId: String, quiz: List<CourseQuiz>, best: Int, onScore: (Int) -> Unit) {
    val answers = remember(lessonId) { mutableStateMapOf<Int, Int>() }
    val score = quiz.indices.count { answers[it] == quiz[it].answer }
    val finished = answers.size == quiz.size
    LaunchedEffect(finished) { if (finished) onScore(score) }
    Section(p, "Quick quiz") {
        quiz.forEachIndexed { qi, q ->
            Text("${qi + 1}. ${q.question}", color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            if (q.ar != null) ArabicText(q.ar, 34, CourseGreen, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            q.options.forEachIndexed { oi, o ->
                val chosen = answers[qi]
                val bg = when {
                    chosen == null -> p.chip
                    oi == q.answer -> CourseGreen.copy(alpha = 0.2f)
                    oi == chosen -> Color(0xFFC0563F).copy(alpha = 0.2f)
                    else -> p.chip
                }
                val isAr = o.any { it in '\u0600'..'\u06FF' }
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp)).background(bg)
                        .clickable(enabled = chosen == null) { answers[qi] = oi }.padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (isAr) Text(o, fontFamily = QuranArabicFont, fontSize = 22.sp, color = p.text) else Text(o, color = p.text, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (finished) "Score: $score / ${quiz.size}" else "Answered ${answers.size} / ${quiz.size}", color = CourseGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (best >= 0) Text("Best: $best / ${quiz.size}", color = p.muted, fontSize = 12.sp)
            if (finished) TextButton(onClick = { answers.clear() }) { Text("Retry") }
        }
    }
}

@Composable
fun CoursePractice(lesson: CourseLesson, onOpenWordByWord: (Int) -> Unit) {
    val p = coursePalette()
    val ctx = LocalContext.current
    var surah by remember(lesson.id) { mutableIntStateOf(lesson.surahs.firstOrNull() ?: 1) }
    val ayahs = remember(surah) { runCatching { QuranCourseRepository.surahAyahs(ctx, surah) }.getOrDefault(emptyList()) }
    var repeats by remember { mutableIntStateOf(3) }
    var hifzIndex by remember(surah) { mutableIntStateOf(-1) }
    val revealed = remember(surah, lesson.id) { mutableStateMapOf<Int, Boolean>() }
    val marks = remember(surah, lesson.id) { mutableStateMapOf<Int, Boolean>() }

    Section(p, "Choose a surah") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(lesson.surahs) { s ->
                Text(
                    "$s · ${QuranCourseRepository.surahName(ctx, s)}", fontSize = 12.sp,
                    color = if (s == surah) Color.White else p.text,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (s == surah) CourseGreen else p.chip).clickable { CourseAudio.stop(); surah = s }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        if (lesson.kind == "read") TextButton(onClick = { onOpenWordByWord(surah) }) { Text("Open word-by-word for this surah") }
        if (lesson.kind == "hifz") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Repeat each ayah", color = p.text, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                listOf(3, 5, 7, 10).forEach { n ->
                    Text("$n×", fontSize = 12.sp, color = if (n == repeats) Color.White else p.text,
                        modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(50)).background(if (n == repeats) CourseGold else p.chip).clickable { repeats = n }.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            fun playFrom(i: Int) {
                if (i >= ayahs.size) { hifzIndex = -1; return }
                hifzIndex = i
                CourseAudio.play(surah, ayahs[i].ayah, repeats) { if (hifzIndex == i) playFrom(i + 1) }
            }
            Button(onClick = { if (hifzIndex >= 0) { CourseAudio.stop(); hifzIndex = -1 } else playFrom(0) }, colors = ButtonDefaults.buttonColors(containerColor = CourseGreen)) {
                Text(if (hifzIndex >= 0) "Stop (ayah ${hifzIndex + 1})" else "Start hifz playback")
            }
        }
        if (lesson.kind == "selftest" && marks.isNotEmpty()) Text("Self-test score: ${marks.values.count { it }} / ${ayahs.size}", color = CourseGreen, fontWeight = FontWeight.Bold)
    }
    ayahs.forEachIndexed { i, a ->
        val key = "${a.surah}:${a.ayah}"
        val highlight = lesson.kind == "hifz" && i == hifzIndex
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (highlight) CourseGreen.copy(alpha = 0.12f) else p.card).border(1.dp, p.line, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ayah ${a.ayah}", color = p.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                val playing = CourseAudio.playingKey == key
                IconButton(onClick = { if (playing) CourseAudio.stop() else CourseAudio.play(a.surah, a.ayah) }, modifier = Modifier.size(34.dp)) {
                    Icon(if (playing) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp, "Play", tint = CourseGreen)
                }
            }
            if (lesson.kind == "selftest" && revealed[i] != true) {
                Text("Recite from memory, then reveal.", color = p.muted, fontSize = 13.sp)
                TextButton(onClick = { revealed[i] = true }) { Text("Reveal") }
            } else {
                ArabicText(a.arabic, 26, p.text, Modifier.fillMaxWidth())
                if (lesson.kind == "read") Text(a.english, color = p.muted, fontSize = 13.sp, lineHeight = 19.sp)
                if (lesson.kind == "selftest") Row {
                    TextButton(onClick = { marks[i] = true }) { Text(if (marks[i] == true) "✓ Correct" else "I was right") }
                    TextButton(onClick = { marks[i] = false }) { Text(if (marks[i] == false) "✗ Needs work" else "I made a mistake") }
                }
            }
        }
    }
}
