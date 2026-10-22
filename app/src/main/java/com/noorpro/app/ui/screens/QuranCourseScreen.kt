package com.noorpro.app.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.CourseLesson
import com.noorpro.app.data.QuranCourse
import com.noorpro.app.data.QuranCourseProgress
import com.noorpro.app.data.QuranCourseRepository
import com.noorpro.app.data.QuranLearningSurah
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

internal val CourseGreen = Color(0xFF1F7A5A)
internal val CourseGold = Color(0xFFC9A227)

internal class CoursePalette(val dark: Boolean) {
    val bg = if (dark) Color(0xFF0B1512) else Color(0xFFFBF8F1)
    val card = if (dark) Color(0xFF13211C) else Color.White
    val text = if (dark) Color(0xFFE9EFEA) else Color(0xFF1C2622)
    val muted = if (dark) Color(0xFF93A39B) else Color(0xFF6B7770)
    val line = if (dark) Color(0xFF223630) else Color(0xFFE7E1D3)
    val chip = if (dark) Color(0xFF1A2A24) else Color(0xFFF1EEE4)
}

@Composable
internal fun coursePalette(): CoursePalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(dark) { CoursePalette(dark) }
}

/** Tajweed colour per rule (word-level highlight). */
internal fun ruleColor(rule: String?): Color = when (rule) {
    "ghunna" -> Color(0xFFE07B00)
    "ikhfa" -> Color(0xFF8E44AD)
    "idgham" -> Color(0xFF2E8B57)
    "iqlab" -> Color(0xFF1E6FD9)
    "izhar" -> Color(0xFF00897B)
    "qalqala" -> Color(0xFFD32F2F)
    "madd" -> Color(0xFFC2185B)
    "tafkheem" -> Color(0xFF283593)
    "tarqeeq" -> Color(0xFF4FA3E0)
    "waqf" -> Color(0xFF6D6D6D)
    "hamzawasl" -> Color(0xFF8D8D8D)
    "special" -> Color(0xFFB8860B)
    else -> Color.Unspecified
}

internal fun ruleName(rule: String?): String = when (rule) {
    "ghunna" -> "Ghunna"; "ikhfa" -> "Ikhfa"; "idgham" -> "Idgham"; "iqlab" -> "Iqlab"; "izhar" -> "Izhar"
    "qalqala" -> "Qalqala"; "madd" -> "Madd"; "tafkheem" -> "Heavy (tafkheem)"; "tarqeeq" -> "Light (tarqeeq)"
    "waqf" -> "Stopping"; "hamzawasl" -> "Hamzatul wasl"; "special" -> "Special rule"; else -> ""
}

/** One shared player for per-ayah Husary audio. Repeats [times] then calls [onDone]. */
internal object CourseAudio {
    private var player: MediaPlayer? = null
    var playingKey by mutableStateOf<String?>(null)
        private set
    fun stop() { runCatching { player?.release() }; player = null; playingKey = null }
    fun play(surah: Int, ayah: Int, times: Int = 1, onDone: () -> Unit = {}) {
        stop()
        val key = "$surah:$ayah"
        playingKey = key
        var left = times
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            mp.setDataSource(QuranCourseRepository.husaryUrl(surah, ayah))
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener {
                left--
                if (left > 0 && player === mp) { it.seekTo(0); it.start() } else { if (player === mp) stop(); onDone() }
            }
            mp.setOnErrorListener { _, _, _ -> if (player === mp) stop(); true }
            mp.prepareAsync()
        }.onFailure { stop() }
    }
}

private sealed class CourseNav {
    object Home : CourseNav()
    data class Level(val index: Int) : CourseNav()
    data class Lesson(val id: String) : CourseNav()
}

@Composable
fun QuranCourseScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val course = remember { runCatching { QuranCourseRepository.load(ctx) }.getOrNull() }
    val progress = remember { QuranCourseProgress(ctx) }
    var done by remember { mutableStateOf(progress.completed()) }
    var nav by remember { mutableStateOf<CourseNav>(CourseNav.Home) }
    DisposableEffect(Unit) { onDispose { CourseAudio.stop() } }

    fun back() {
        CourseAudio.stop()
        nav = when (val n = nav) {
            CourseNav.Home -> { viewModel.navigateTo(DeenScreen.EDUCATION); CourseNav.Home }
            is CourseNav.Level -> CourseNav.Home
            is CourseNav.Lesson -> course?.levels?.indexOfFirst { lv -> lv.lessons.any { it.id == n.id } }?.let { CourseNav.Level(it) } ?: CourseNav.Home
        }
    }
    BackHandler(enabled = nav != CourseNav.Home) { back() }

    if (course == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Course could not be loaded.") }
        return
    }
    when (val n = nav) {
        CourseNav.Home -> CourseHome(
            course, done, progress.lastLesson, onBack = { back() },
            onLevel = { nav = CourseNav.Level(it) },
            onResume = { id -> nav = CourseNav.Lesson(id) },
            onSurahLearning = { viewModel.navigateTo(DeenScreen.QURAN_LEARNING_DASHBOARD) },
        )
        is CourseNav.Level -> CourseLevelList(course, n.index, done, onBack = { back() }, onLesson = { nav = CourseNav.Lesson(it) })
        is CourseNav.Lesson -> {
            val all = course.allLessons
            val idx = all.indexOfFirst { it.id == n.id }.coerceAtLeast(0)
            LaunchedEffect(n.id) { progress.lastLesson = n.id }
            CourseLessonView(
                lesson = all[idx], levelTitle = course.levels.first { lv -> lv.lessons.any { it.id == n.id } }.title,
                position = idx, total = all.size, isDone = n.id in done, bestScore = progress.bestScore(n.id),
                onBack = { back() },
                onPrev = if (idx > 0) ({ CourseAudio.stop(); nav = CourseNav.Lesson(all[idx - 1].id) }) else null,
                onNext = if (idx < all.size - 1) ({ CourseAudio.stop(); nav = CourseNav.Lesson(all[idx + 1].id) }) else null,
                onToggleDone = { progress.setCompleted(n.id, n.id !in done); done = progress.completed() },
                onQuizScore = { progress.saveScore(n.id, it) },
                onOpenWordByWord = { s ->
                    viewModel.openLearningSurah(QuranLearningSurah(s, "", QuranCourseRepository.surahName(ctx, s), QuranCourseRepository.surahAyahs(ctx, s).size, null))
                },
            )
        }
    }
}

@Composable
internal fun CourseTopBar(title: String, subtitle: String?, onBack: () -> Unit) {
    val p = coursePalette()
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = p.text) }
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (subtitle != null) Text(subtitle, color = p.muted, fontSize = 12.sp, maxLines = 1)
        }
    }
    HorizontalDivider(color = p.line, thickness = 0.5.dp)
}

@Composable
fun CourseHome(
    course: QuranCourse, done: Set<String>, lastLesson: String?,
    onBack: () -> Unit, onLevel: (Int) -> Unit, onResume: (String) -> Unit, onSurahLearning: () -> Unit,
) {
    val p = coursePalette()
    val total = course.allLessons.size
    val resume = lastLesson?.let { id -> course.allLessons.firstOrNull { it.id == id } }
        ?: course.allLessons.firstOrNull { it.id !in done }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        CourseTopBar("Quran Reading Course", "Qaida to advanced tajweed · Hafs 'an 'Asim", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.navigationBarsPadding()) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CourseGreen).padding(18.dp)) {
                    Text("Your progress", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text("${done.size} of $total lessons", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { if (total == 0) 0f else done.size / total.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = CourseGold, trackColor = Color.White.copy(alpha = 0.25f))
                    if (resume != null) {
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { onResume(resume.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CourseGreen)) {
                            Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp))
                            Text(if (lastLesson != null) "Continue: ${resume.title}" else "Start: ${resume.title}", maxLines = 1)
                        }
                    }
                }
            }
            itemsIndexed(course.levels) { i, lv ->
                val n = lv.lessons.count { it.id in done }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(16.dp))
                        .clickable { onLevel(i) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(if (n == lv.lessons.size) CourseGreen else CourseGold.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = if (n == lv.lessons.size) Color.White else CourseGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lv.title, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(lv.subtitle, color = p.muted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { n / lv.lessons.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = CourseGreen, trackColor = p.chip)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("$n/${lv.lessons.size}", color = p.muted, fontSize = 12.sp)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.chip).clickable(onClick = onSurahLearning).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = CourseGreen)
                    Spacer(Modifier.width(12.dp))
                    Column { Text("Surah Learning (word by word)", color = p.text, fontWeight = FontWeight.SemiBold); Text("All 114 surahs with Husary audio", color = p.muted, fontSize = 12.sp) }
                }
            }
            item { Text("Rules follow Hafs 'an 'Asim as taught in: " + course.sources.joinToString("; ") + ".", color = p.muted, fontSize = 11.sp) }
        }
    }
}

@Composable
fun CourseLevelList(course: QuranCourse, index: Int, done: Set<String>, onBack: () -> Unit, onLesson: (String) -> Unit) {
    val p = coursePalette()
    val lv = course.levels[index]
    Column(Modifier.fillMaxSize().background(p.bg)) {
        CourseTopBar(lv.title, lv.subtitle, onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.navigationBarsPadding()) {
            itemsIndexed(lv.lessons) { i, l ->
                val d = l.id in done
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(14.dp))
                        .clickable { onLesson(l.id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (d) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (d) CourseGreen else p.muted)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Lesson ${i + 1}", color = p.muted, fontSize = 11.sp)
                        Text(l.title, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(l.goal, color = p.muted, fontSize = 12.sp, maxLines = 2)
                    }
                }
            }
        }
    }
}
