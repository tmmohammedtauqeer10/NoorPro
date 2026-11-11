package com.noorpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.R
import com.noorpro.app.data.spiritual.BookLibrary
import com.noorpro.app.data.spiritual.BookMark
import com.noorpro.app.data.spiritual.BookPrefs
import com.noorpro.app.data.spiritual.BookText
import com.noorpro.app.data.spiritual.LibraryBook
import com.noorpro.app.data.spiritual.SpiritualPrefs
import com.noorpro.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ReaderArabicFont = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))
private val ReaderSerif = FontFamily.Serif

private enum class ReaderMode { READ, SEARCH, BOOKMARKS }
private data class SearchHit(val section: Int, val para: Int, val snippet: String)

/** Readable chapters (sections that have text); part headers without text are only shown in Contents. */
private fun BookText.chapters(): List<Int> = sections.indices.filter { sections[it].paras.isNotEmpty() }
/** The part (level-1 heading) a section belongs to, or null if it is itself a part. */
private fun BookText.partOf(i: Int): Int? {
    if (sections[i].level <= 1) return null
    for (k in i - 1 downTo 0) if (sections[k].level <= 1) return k
    return null
}

/**
 * Offline book reader: title/author header, Contents drawer (parts > chapters, current chapter highlighted),
 * prev/next chapter and progress at the bottom, last-read position, in-book search, a single bookmark action
 * for the current position plus a bookmarks list, and adjustable text size.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(book: LibraryBook, onClose: () -> Unit) {
    val context = LocalContext.current
    var lang by rememberSaveable(book.id) { mutableStateOf(book.editions.first().lang) }
    val edition = book.editions.firstOrNull { it.lang == lang } ?: book.editions.first()
    val prefKey = "${book.id}.${edition.lang}"
    var text by remember(prefKey) { mutableStateOf<BookText?>(null) }
    var error by remember(prefKey) { mutableStateOf(false) }
    LaunchedEffect(prefKey) {
        try { text = withContext(Dispatchers.IO) { BookLibrary.load(context.applicationContext, book.id, edition.lang) } } catch (_: Exception) { error = true }
    }
    var mode by rememberSaveable(prefKey) { mutableStateOf(ReaderMode.READ) }
    var section by rememberSaveable(prefKey) { mutableIntStateOf(-1) }
    var startPara by rememberSaveable(prefKey) { mutableIntStateOf(0) }
    var openNonce by remember { mutableIntStateOf(0) }
    var currentPara by remember(prefKey) { mutableIntStateOf(0) }
    var fontSize by remember { mutableFloatStateOf(BookPrefs.fontSize(context)) }
    var arabicSize by remember { mutableFloatStateOf(SpiritualPrefs.arabicFontSize(context)) }
    var bookmarks by remember(prefKey) { mutableStateOf(BookPrefs.bookmarks(context, prefKey)) }
    var query by rememberSaveable(prefKey) { mutableStateOf("") }
    var hits by remember(prefKey) { mutableStateOf<List<SearchHit>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showSize by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    val t = text
    // First open: resume the last-read position, else the first chapter.
    LaunchedEffect(t) {
        if (t != null && section !in t.sections.indices) {
            val last = BookPrefs.lastRead(context, prefKey)
            val ch = t.chapters()
            if (last != null && last.first in t.sections.indices && t.sections[last.first].paras.isNotEmpty()) { section = last.first; startPara = last.second }
            else { section = ch.firstOrNull() ?: 0; startPara = 0 }
            currentPara = startPara
        }
    }
    fun open(s: Int, p: Int) {
        val tt = text ?: return
        val target = if (tt.sections.getOrNull(s)?.paras?.isEmpty() == true) tt.chapters().firstOrNull { it > s } ?: s else s
        section = target; startPara = p; currentPara = p; openNonce++; mode = ReaderMode.READ
        BookPrefs.setLastRead(context, prefKey, target, p)
        scope.launch { drawer.close() }
    }
    BackHandler {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            mode != ReaderMode.READ -> mode = ReaderMode.READ
            else -> onClose()
        }
    }
    val marked = bookmarks.any { it.section == section && it.para == currentPara }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = t != null,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = stitchSurface(), modifier = Modifier.fillMaxWidth(0.86f)) {
                if (t != null) ContentsList(t, book.title, t.author.ifBlank { book.author }, section) { open(it, 0) }
            }
        }
    ) {
        Scaffold(
            containerColor = stitchBackground(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                Column(Modifier.fillMaxWidth().background(stitchSurface())) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                        IconButton(onClick = { if (mode != ReaderMode.READ) mode = ReaderMode.READ else onClose() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchPrimary())
                        }
                        Column(Modifier.weight(1f)) {
                            Text(t?.title?.ifBlank { null } ?: book.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val author = t?.author?.ifBlank { null } ?: book.author
                            if (author.isNotBlank()) Text(author, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { scope.launch { drawer.open() } }, enabled = t != null) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Contents", tint = stitchPrimary())
                        }
                        IconButton(onClick = { mode = ReaderMode.SEARCH }, enabled = t != null) { Icon(Icons.Default.Search, contentDescription = "Search in book", tint = stitchPrimary()) }
                        IconButton(enabled = t != null && mode == ReaderMode.READ, onClick = {
                            val tt = t ?: return@IconButton
                            val s = tt.sections.getOrNull(section) ?: return@IconButton
                            val label = s.title + " — " + s.paras.getOrNull(currentPara).orEmpty().take(70)
                            val added = BookPrefs.toggleBookmark(context, prefKey, section, currentPara, label)
                            bookmarks = BookPrefs.bookmarks(context, prefKey)
                            scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(if (added) "Bookmark added" else "Bookmark removed") }
                        }) {
                            Icon(if (marked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Bookmark this position", tint = if (marked) StitchGold else stitchPrimary())
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More", tint = stitchPrimary()) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Contents") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) }, onClick = { menu = false; scope.launch { drawer.open() } })
                                DropdownMenuItem(text = { Text("Bookmarks (${bookmarks.size})") }, leadingIcon = { Icon(Icons.Default.Bookmarks, null) }, onClick = { menu = false; bookmarks = BookPrefs.bookmarks(context, prefKey); mode = ReaderMode.BOOKMARKS })
                                DropdownMenuItem(text = { Text("Text size") }, leadingIcon = { Icon(Icons.Default.TextFields, null) }, onClick = { menu = false; showSize = true })
                                book.editions.takeIf { it.size > 1 }?.forEach { e ->
                                    DropdownMenuItem(text = { Text(langName(e.lang) + if (e.lang == edition.lang) "  ✓" else "") }, onClick = { menu = false; lang = e.lang })
                                }
                            }
                        }
                    }
                    if (showSize) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text("A", color = stitchMutedText(), fontSize = 13.sp)
                            if (t?.rtl == true) Slider(value = arabicSize, onValueChange = { arabicSize = it; SpiritualPrefs.setArabicFontSize(context, it) }, valueRange = 18f..44f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                            else Slider(value = fontSize, onValueChange = { fontSize = it; BookPrefs.setFontSize(context, it) }, valueRange = 14f..28f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                            Text("A", color = stitchMutedText(), fontSize = 20.sp)
                            IconButton(onClick = { showSize = false }) { Icon(Icons.Default.Check, contentDescription = "Done", tint = stitchPrimary()) }
                        }
                    }
                    HorizontalDivider(color = stitchMutedText().copy(alpha = 0.15f))
                }
            },
            bottomBar = {
                if (t != null && mode == ReaderMode.READ && section in t.sections.indices) {
                    val ch = t.chapters(); val pos = ch.indexOf(section)
                    ChapterNavBar(
                        pos = pos, total = ch.size,
                        paraProgress = if (t.sections[section].paras.size <= 1) 1f else currentPara.toFloat() / (t.sections[section].paras.size - 1),
                        onPrev = { if (pos > 0) open(ch[pos - 1], 0) },
                        onNext = { if (pos in 0 until ch.lastIndex) open(ch[pos + 1], 0) }
                    )
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when {
                    error -> Text("This book could not be loaded.", color = stitchMutedText(), modifier = Modifier.padding(24.dp))
                    t == null || section !in t.sections.indices -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = stitchPrimary()) }
                    mode == ReaderMode.READ -> key(section, openNonce) {
                        ChapterView(t, section, startPara, if (t.rtl) arabicSize * 0.8f else fontSize, bookmarks) { p ->
                            currentPara = p; BookPrefs.setLastRead(context, prefKey, section, p)
                        }
                    }
                    mode == ReaderMode.SEARCH -> {
                        LaunchedEffect(prefKey) {
                            snapshotFlow { query }.debounce(350).collect { q ->
                                if (q.trim().length < 2) { hits = null; return@collect }
                                searching = true
                                hits = withContext(Dispatchers.Default) { searchBook(t, q.trim()) }
                                searching = false
                            }
                        }
                        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                placeholder = { Text(if (t.rtl) "ابحث في الكتاب" else "Search in this book") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) })
                            if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
                            val h = hits
                            if (h != null) Text(if (h.size >= MAX_HITS) "First $MAX_HITS results" else "${h.size} results", color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                                itemsIndexed(h.orEmpty()) { _, hit ->
                                    StitchCard(Modifier.fillMaxWidth().clickable { open(hit.section, hit.para) }) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(t.sections[hit.section].title, color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(hit.snippet, color = stitchText(), fontSize = 14.sp, lineHeight = 21.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> BookmarkList(t, bookmarks, onOpen = { open(it.section, it.para) }, onDelete = { b ->
                        BookPrefs.toggleBookmark(context, prefKey, b.section, b.para, b.label); bookmarks = BookPrefs.bookmarks(context, prefKey)
                    })
                }
            }
        }
    }
}

private const val MAX_HITS = 300

private fun searchBook(t: BookText, q: String): List<SearchHit> {
    val out = ArrayList<SearchHit>()
    val norm = normalizeForSearch(q)
    t.sections.forEachIndexed { si, s ->
        s.paras.forEachIndexed { pi, p ->
            val n = normalizeForSearch(p)
            val i = n.indexOf(norm, ignoreCase = true)
            if (i >= 0) {
                val start = (i - 60).coerceAtLeast(0)
                val end = (i + norm.length + 90).coerceAtMost(n.length)
                out.add(SearchHit(si, pi, (if (start > 0) "…" else "") + n.substring(start, end) + (if (end < n.length) "…" else "")))
                if (out.size >= MAX_HITS) return out
            }
        }
    }
    return out
}

/** Strips Arabic diacritics/tatweel and transliteration marks so searches match loosely. */
private fun normalizeForSearch(s: String): String =
    s.replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "").replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')

private fun langName(l: String) = when (l) { "ar" -> "العربية"; "fa" -> "فارسی"; "en" -> "English"; else -> l }

@Composable
private fun ContentsList(t: BookText, title: String, author: String, current: Int, onOpen: (Int) -> Unit) {
    val currentPart = t.partOf(current) ?: current
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (current - 2).coerceAtLeast(0))
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(stitchSoftSurface()).padding(20.dp)) {
            Text("CONTENTS", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Text(title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = ReaderSerif)
            if (author.isNotBlank()) Text(author, color = stitchMutedText(), fontSize = 12.sp)
        }
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
            itemsIndexed(t.sections) { i, s ->
                val isCurrent = i == current
                val isPart = s.level <= 1
                val bg = if (isCurrent) stitchPrimary().copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable { onOpen(i) }
                        .padding(start = if (isPart) 12.dp else 28.dp, end = 12.dp, top = if (isPart) 12.dp else 8.dp, bottom = if (isPart) 8.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCurrent) { Box(Modifier.size(width = 3.dp, height = 18.dp).background(StitchGold)); Spacer(Modifier.width(8.dp)) }
                    Text(
                        s.title,
                        color = when { isCurrent -> stitchPrimary(); isPart && i == currentPart -> stitchPrimary(); isPart -> stitchText(); else -> stitchMutedText() },
                        fontWeight = if (isPart || isCurrent) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isPart) 15.sp else 14.sp, lineHeight = 19.sp,
                        fontFamily = if (t.rtl) ReaderArabicFont else null,
                        textAlign = if (t.rtl) TextAlign.Right else TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterNavBar(pos: Int, total: Int, paraProgress: Float, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(stitchSurface()).navigationBarsPadding()) {
        LinearProgressIndicator(
            progress = { ((pos.coerceAtLeast(0) + paraProgress.coerceIn(0f, 1f)) / total.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(3.dp), color = StitchGold, trackColor = stitchMutedText().copy(alpha = 0.15f)
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPrev, enabled = pos > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null); Text("Previous")
            }
            Text("Chapter ${pos + 1} of $total", color = stitchMutedText(), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            TextButton(onClick = onNext, enabled = pos in 0 until total - 1) {
                Text("Next"); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
private fun ChapterView(t: BookText, section: Int, startPara: Int, size: Float, bookmarks: List<BookMark>, onPosition: (Int) -> Unit) {
    val s = t.sections[section]
    val part = t.partOf(section)?.let { t.sections[it].title }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = if (startPara <= 0) 0 else (startPara + 1).coerceAtMost(s.paras.size))
    LaunchedEffect(section) {
        snapshotFlow { state.firstVisibleItemIndex }.debounce(300).collect { onPosition((it - 1).coerceIn(0, (s.paras.size - 1).coerceAtLeast(0))) }
    }
    val rtl = t.rtl
    LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 40.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (part != null) Text(part.uppercase(), color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    s.title, color = stitchPrimary(), fontWeight = FontWeight.Bold, fontSize = (size + 5).sp, lineHeight = ((size + 5) * 1.3f).sp,
                    fontFamily = if (rtl) ReaderArabicFont else ReaderSerif, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(width = 48.dp, height = 2.dp).background(StitchGold))
            }
        }
        itemsIndexed(s.paras) { i, p ->
            val markedHere = bookmarks.any { it.section == section && it.para == i }
            if (p == "⁂") {
                Text("⁂", color = StitchGold, fontSize = (size + 2).sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            } else Row(Modifier.fillMaxWidth().padding(bottom = (size * 0.9f).dp)) {
                if (markedHere) Box(Modifier.padding(end = 8.dp).width(3.dp).height((size * 1.6f).dp).background(StitchGold))
                Text(
                    p, color = stitchText(), fontSize = size.sp, lineHeight = (size * if (rtl) 1.85f else 1.6f).sp,
                    fontFamily = if (rtl) ReaderArabicFont else ReaderSerif,
                    textAlign = if (rtl) TextAlign.Right else TextAlign.Justify,
                    style = LocalTextStyle.current.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BookmarkList(t: BookText, bookmarks: List<BookMark>, onOpen: (BookMark) -> Unit, onDelete: (BookMark) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        item { Text("BOOKMARKS", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp) }
        if (bookmarks.isEmpty()) item { Text("No bookmarks yet. While reading, tap the bookmark icon in the top bar to save your place.", color = stitchMutedText(), modifier = Modifier.padding(vertical = 16.dp)) }
        itemsIndexed(bookmarks) { _, b ->
            StitchCard(Modifier.fillMaxWidth().clickable { onOpen(b) }) {
                Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.sections.getOrNull(b.section)?.title.orEmpty(), color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(t.sections.getOrNull(b.section)?.paras?.getOrNull(b.para)?.take(110)?.plus("…") ?: b.label, color = stitchMutedText(), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onDelete(b) }) { Icon(Icons.Default.Delete, contentDescription = "Remove bookmark", tint = stitchMutedText()) }
                }
            }
        }
    }
}
