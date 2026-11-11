package com.noorpro.app.ui.islamic

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.spiritual.PathLesson
import com.noorpro.app.data.spiritual.TasawwufPath
import com.noorpro.app.data.spiritual.TasawwufPathStore
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon

/** 1.2.0 (116) Tasawwuf learning path: which page of the path is showing. */
enum class PathPage { HOME, LESSON, GLOSSARY, FAQ, BOOKS }

/** Entry card shown at the top of the Tasawwuf section. */
@Composable
fun TasawwufPathEntryCard(path: TasawwufPath?, done: Set<String>, onOpen: () -> Unit) {
    val total = path?.lessons?.size ?: 24
    val n = path?.lessons?.count { it.id in done } ?: 0
    IkHero(Modifier.clickable(onClick = onOpen).testTag("tas_path_entry")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LEARNING PATH", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(4.dp))
                Text("12 weeks of Ihsan", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("24 short lessons · knowledge checks · glossary · books", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.clip(RoundedCornerShape(50)).background(IkGold).padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (n == 0) "Begin the path" else if (n >= total) "Review the path" else "Continue · lesson ${n + 1}", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            IkRing(if (total == 0) 0f else n / total.toFloat(), size = 78.dp, stroke = 7.dp) {
                Text("$n/$total", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

data class TasawwufPathActions(
    val onBack: () -> Unit = {},
    val onLesson: (String) -> Unit = {},
    val onPage: (PathPage) -> Unit = {},
    val onCategory: (String) -> Unit = {},
    val onAnswer: (Int) -> Unit = {},
    val onDone: (Boolean) -> Unit = {},
    val onNext: (String) -> Unit = {},
    val onBook: (String) -> Unit = {},
    val onLink: (String) -> Unit = {},
)

/** Stateless path UI (used by the screen and the preview renders). */
@Composable
fun TasawwufPathContent(path: TasawwufPath, page: PathPage, lessonId: String?, done: Set<String>, answer: Int?, actions: TasawwufPathActions) {
    val lesson = lessonId?.let(path::lesson)
    Column(Modifier.ikScreen()) {
        IkHeader(
            when (page) { PathPage.HOME -> "Tasawwuf Path"; PathPage.LESSON -> path.moduleOf(lessonId.orEmpty())?.let { "Week ${it.week} · ${it.title}" } ?: "Lesson"
                PathPage.GLOSSARY -> "Glossary"; PathPage.FAQ -> "Questions"; PathPage.BOOKS -> "Books" },
            subtitle = if (page == PathPage.HOME) "${path.weeks} weeks · ${path.lessons.size} lessons · offline" else null,
            onBack = actions.onBack,
        )
        when {
            page == PathPage.LESSON && lesson != null -> PathLessonView(path, lesson, lesson.id in done, answer, actions)
            page == PathPage.GLOSSARY -> PathListView(path.glossary, "tas_glossary")
            page == PathPage.FAQ -> PathListView(path.faqs, "tas_faq", footer = path.teacherGuidance)
            page == PathPage.BOOKS -> PathBooksView(path, actions)
            else -> PathHomeView(path, done, actions)
        }
    }
}

@Composable
private fun PathHomeView(path: TasawwufPath, done: Set<String>, a: TasawwufPathActions) {
    val resume = path.resume(done)
    LazyColumn(Modifier.fillMaxSize().testTag("tas_path_home"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            IkHero {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("سلوک · احسان", color = IkGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(path.title.ifBlank { "Tasawwuf learning path" }, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(path.description, color = IkCream.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    val n = path.lessons.count { it.id in done }
                    IkRing(path.progress(done), size = 84.dp, stroke = 8.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(path.progress(done) * 100).toInt()}%", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("$n of ${path.lessons.size}", color = IkCream.copy(alpha = 0.75f), fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val label = when { done.isEmpty() -> "Begin lesson 1"; resume == null -> "Path complete · review"; else -> "Continue · ${resume.title}" }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IkGold).clickable { a.onLesson((resume ?: path.ordered.first()).id) }
                    .padding(horizontal = 14.dp, vertical = 11.dp).testTag("tas_continue"), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PlayArrow, null, tint = IkGreenDeep, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(6.dp))
                    Text(label, color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            IkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkIconTile(Icons.Outlined.AccountTree, size = 36.dp); Spacer(Modifier.width(10.dp))
                    Text(path.orientationTitle, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(path.orientationBody, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(6.dp))
                Text(path.orientationSources.joinToString(" · "), color = ikAccentText(), fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IkPill("Silsila charts", false, icon = Icons.Outlined.AccountTree, tag = "tas_silsila") { a.onCategory("tariqas") }
                    IkPill("Awliya Allah", false, icon = Icons.Outlined.AutoAwesome, tag = "tas_awliya") { a.onCategory("awliya") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PathTile("Glossary", "${path.glossary.size} terms", Icons.Outlined.Translate, Modifier.weight(1f), "tas_tile_glossary") { a.onPage(PathPage.GLOSSARY) }
                PathTile("Questions", "${path.faqs.size} FAQs", Icons.Outlined.HelpOutline, Modifier.weight(1f), "tas_tile_faq") { a.onPage(PathPage.FAQ) }
                PathTile("Books", "${path.books.count { it.reader.isNotBlank() }} offline", Icons.AutoMirrored.Outlined.MenuBook, Modifier.weight(1f), "tas_tile_books") { a.onPage(PathPage.BOOKS) }
            }
        }
        path.stages.forEachIndexed { si, stage ->
            item { IkSection("Stage ${si + 1} · ${stage.title}") }
            item { Text(stage.description, color = stitchMutedText(), fontSize = 12.sp) }
            stage.moduleIds.mapNotNull { id -> path.modules.firstOrNull { it.id == id } }.forEach { m ->
                item(key = m.id) {
                    IkCard(padding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(ikSoft()), contentAlignment = Alignment.Center) {
                                Text("${m.week}", color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("WEEK ${m.week}", color = IkGoldDeep, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                                Text(m.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            val c = m.lessonIds.count { it in done }
                            Text("$c/${m.lessonIds.size}", color = if (c == m.lessonIds.size) ikAccentText() else stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        m.lessonIds.mapNotNull(path::lesson).forEach { l ->
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { a.onLesson(l.id) }.padding(horizontal = 4.dp, vertical = 6.dp).testTag("tas_lesson_${l.id}"),
                                verticalAlignment = Alignment.CenterVertically) {
                                val isDone = l.id in done
                                Icon(if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                                    tint = if (isDone) ikAccentText() else stitchMutedText(), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(l.title, color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("${l.minutes} min", color = stitchMutedText(), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        item { Text(path.credit + " " + path.reviewNote, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 6.dp)) }
    }
}

@Composable
private fun PathTile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, tag: String, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).clickable(onClick = onClick)
        .padding(12.dp).testTag(tag)) {
        IkIconTile(icon, size = 34.dp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(sub, color = stitchMutedText(), fontSize = 11.sp)
    }
}

@Composable
private fun PathLessonView(path: TasawwufPath, l: PathLesson, isDone: Boolean, answer: Int?, a: TasawwufPathActions) {
    val idx = path.ordered.indexOfFirst { it.id == l.id }
    val next = path.nextAfter(l.id)
    val m = path.moduleOf(l.id)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 28.dp).testTag("tas_lesson_view"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("LESSON ${idx + 1} OF ${path.lessons.size} · ${l.minutes} MIN", color = IkGoldDeep, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
        Text(l.title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
        SelectionContainer { Text(l.summary, color = stitchText(), fontSize = 15.sp, lineHeight = 23.sp) }
        if (l.practice.isNotBlank()) PathBlock(Icons.Outlined.DirectionsWalk, "Practice", l.practice)
        if (l.reflection.isNotBlank()) PathBlock(Icons.Outlined.SelfImprovement, "Reflect privately", l.reflection, italic = true)
        val srcs = l.sourceIds.mapNotNull(path::source)
        if (srcs.isNotEmpty()) {
            Text("SOURCES", color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                srcs.forEach { s -> IkPill(s.label, false, icon = Icons.Outlined.Link) { if (s.url.isNotBlank()) a.onLink(s.url) } }
            }
        }
        l.check?.let { c ->
            IkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkIconTile(Icons.Outlined.Quiz, size = 32.dp); Spacer(Modifier.width(8.dp))
                    Text("Knowledge check", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(c.question, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp)
                c.options.forEachIndexed { i, opt ->
                    Spacer(Modifier.height(8.dp))
                    val picked = answer == i
                    val right = answer != null && i == c.answer
                    val bg = when { right -> ikSoft(); picked -> IkRed.copy(alpha = 0.10f); else -> Color.Transparent }
                    val line = when { right -> ikAccentText(); picked -> IkRed; else -> stitchLine() }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, line, RoundedCornerShape(12.dp))
                        .clickable(enabled = answer == null) { a.onAnswer(i) }.padding(12.dp).testTag("tas_opt_$i"), verticalAlignment = Alignment.CenterVertically) {
                        Text(opt, color = stitchText(), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (right) Icon(Icons.Outlined.CheckCircle, null, tint = ikAccentText(), modifier = Modifier.size(18.dp))
                        else if (picked) Icon(Icons.Outlined.Cancel, null, tint = IkRed, modifier = Modifier.size(18.dp))
                    }
                }
                if (answer != null) {
                    Spacer(Modifier.height(10.dp))
                    Text((if (answer == c.answer) "Correct. " else "Not quite. ") + c.explanation, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
        val books = m?.bookIds.orEmpty().mapNotNull { id -> path.books.firstOrNull { it.id == id } }
        if (books.isNotEmpty()) {
            Text("READ WITH THIS WEEK", color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            books.forEach { b -> PathBookRow(b, a) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IkButton(if (isDone) "Completed" else "Mark complete", Modifier.weight(1f), icon = if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.Done,
                primary = !isDone, tag = "tas_done") { a.onDone(!isDone) }
            if (next != null) IkButton("Next lesson", Modifier.weight(1f), icon = Icons.Outlined.ArrowForward, primary = false, tag = "tas_next") { a.onNext(next.id) }
        }
        Text("Completion marks study activity, not spiritual rank. Learn specialised practices from a qualified shaykh.", color = stitchMutedText(), fontSize = 11.sp)
    }
}

@Composable
private fun PathBlock(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, italic: Boolean = false) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikSoft()).padding(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = ikAccentText(), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(body, color = stitchText(), fontSize = 14.sp, lineHeight = 20.sp, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal)
        }
    }
}

@Composable
private fun PathBookRow(b: com.noorpro.app.data.spiritual.PathBook, a: TasawwufPathActions) {
    IkCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(Icons.AutoMirrored.Outlined.MenuBook, size = 36.dp); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(b.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(listOf(b.author, b.level).filter { it.isNotBlank() }.joinToString(" · "), color = stitchMutedText(), fontSize = 11.sp)
            }
        }
        if (b.description.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(b.description, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp) }
        if (b.guidance.isNotBlank()) Text("Read: " + b.guidance, color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp, fontStyle = FontStyle.Italic)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (b.reader.isNotBlank()) IkPill("Read offline", true, icon = Icons.AutoMirrored.Outlined.MenuBook, tag = "tas_read_${b.reader}") { a.onBook(b.reader) }
            if (b.url.isNotBlank()) IkPill("Publisher / reference", false, icon = Icons.Outlined.OpenInNew) { a.onLink(b.url) }
        }
    }
}

@Composable
private fun PathBooksView(path: TasawwufPath, a: TasawwufPathActions) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Books marked “Read offline” are bundled in the app. Other titles link to their publisher or reference page; modern translations keep their own rights.", color = stitchMutedText(), fontSize = 12.sp, lineHeight = 17.sp) }
        items(path.books.sortedBy { if (it.reader.isNotBlank()) 0 else 1 }, key = { it.id }) { PathBookRow(it, a) }
    }
}

@Composable
private fun PathListView(rows: List<Pair<String, String>>, tag: String, footer: List<String> = emptyList()) {
    var open by remember { mutableStateOf(setOf<Int>()) }
    LazyColumn(Modifier.fillMaxSize().testTag(tag), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows.size) { i ->
            val (t, d) = rows[i]
            val expanded = tag == "tas_glossary" || i in open
            IkCard(Modifier.clickable { open = if (i in open) open - i else open + i }, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (tag != "tas_glossary") Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = stitchMutedText())
                }
                if (expanded) { Spacer(Modifier.height(4.dp)); Text(d, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp) }
            }
        }
        if (footer.isNotEmpty()) {
            item { IkSection("Choosing a teacher") }
            items(footer) { f -> Row { Text("•  ", color = IkGoldDeep); Text(f, color = stitchMutedText(), fontSize = 13.sp, lineHeight = 19.sp) } }
        }
    }
}


/** Stateful path screen; [onOpenBook] opens the bundled reader, [onOpenCategory] jumps to a Tasawwuf section category. */
@Composable
fun TasawwufPathScreen(onClose: () -> Unit, onOpenBook: (String) -> Unit, onOpenCategory: (String) -> Unit) {
    val ctx = LocalContext.current
    val path = remember { runCatching { TasawwufPath.load(ctx.applicationContext) }.getOrNull() }
    val store = remember { TasawwufPathStore(ctx) }
    var done by remember { mutableStateOf(store.done()) }
    var page by rememberSaveable { mutableStateOf(PathPage.HOME) }
    var lessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var answer by remember(lessonId) { mutableStateOf(lessonId?.let(store::answer)) }
    val back = { if (page == PathPage.HOME) onClose() else { page = PathPage.HOME; lessonId = null } }
    BackHandler(onBack = back)
    if (path == null) { Column(Modifier.ikScreen()) { IkHeader("Tasawwuf Path", onBack = onClose); IkEmptyState(Icons.Outlined.ErrorOutline, "Path unavailable", "The bundled learning path could not be opened.", "tas_err") }; return }
    TasawwufPathContent(path, page, lessonId, done, answer, TasawwufPathActions(
        onBack = back,
        onLesson = { lessonId = it; page = PathPage.LESSON },
        onPage = { page = it },
        onCategory = onOpenCategory,
        onAnswer = { i -> lessonId?.let { store.setAnswer(it, i) }; answer = i },
        onDone = { d -> lessonId?.let { done = store.setDone(it, d) } },
        onNext = { id -> lessonId?.let { done = store.setDone(it, true) }; lessonId = id },
        onBook = onOpenBook,
        onLink = { url -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
    ))
}
