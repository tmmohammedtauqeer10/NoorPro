package com.noorpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.R
import com.noorpro.app.data.spiritual.BookLibrary
import com.noorpro.app.data.spiritual.BookPrefs
import com.noorpro.app.data.spiritual.BookText
import com.noorpro.app.data.spiritual.LibraryBook
import com.noorpro.app.data.spiritual.SpiritualPrefs
import com.noorpro.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext

private val ReaderArabicFont = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))

private enum class ReaderMode { TOC, CHAPTER, SEARCH, BOOKMARKS }
private data class SearchHit(val section: Int, val para: Int, val snippet: String)

/** Offline full-text reader: TOC, chapter pages, search, bookmarks, last-read position, RTL and the shared Arabic font size. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun BookReaderScreen(book: LibraryBook, onClose: () -> Unit) {
    val context = LocalContext.current
    var lang by rememberSaveable(book.id) { mutableStateOf(book.editions.first().lang) }
    val edition = book.editions.firstOrNull { it.lang == lang } ?: book.editions.first()
    val key = "${book.id}.${edition.lang}"
    var text by remember(key) { mutableStateOf<BookText?>(null) }
    var error by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        try { text = withContext(Dispatchers.IO) { BookLibrary.load(context.applicationContext, book.id, edition.lang) } } catch (_: Exception) { error = true }
    }
    var mode by rememberSaveable(key) { mutableStateOf(ReaderMode.TOC) }
    var section by rememberSaveable(key) { mutableIntStateOf(0) }
    var startPara by rememberSaveable(key) { mutableIntStateOf(0) }
    var fontSize by remember { mutableFloatStateOf(SpiritualPrefs.arabicFontSize(context)) }
    var bookmarks by remember(key) { mutableStateOf(BookPrefs.bookmarks(context, key)) }
    var query by rememberSaveable(key) { mutableStateOf("") }
    var hits by remember(key) { mutableStateOf<List<SearchHit>?>(null) }
    var searching by remember { mutableStateOf(false) }

    BackHandler { if (mode != ReaderMode.TOC) mode = ReaderMode.TOC else onClose() }
    fun open(s: Int, p: Int) { section = s; startPara = p; mode = ReaderMode.CHAPTER }

    val t = text
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            IconButton(onClick = { if (mode != ReaderMode.TOC) mode = ReaderMode.TOC else onClose() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchPrimary())
            }
            Text(
                if (mode == ReaderMode.CHAPTER && t != null) t.sections.getOrNull(section)?.title.orEmpty() else book.title,
                color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { mode = ReaderMode.SEARCH }) { Icon(Icons.Default.Search, contentDescription = "Search in book", tint = stitchPrimary()) }
            IconButton(onClick = { bookmarks = BookPrefs.bookmarks(context, key); mode = ReaderMode.BOOKMARKS }) {
                Icon(Icons.Default.Bookmarks, contentDescription = "Bookmarks", tint = stitchPrimary())
            }
        }
        if (book.editions.size > 1) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                book.editions.forEach { e ->
                    FilterChip(selected = e.lang == edition.lang, onClick = { lang = e.lang }, label = { Text(langName(e.lang)) })
                }
            }
        }
        when {
            error -> Text("This book could not be loaded.", color = stitchMutedText(), modifier = Modifier.padding(24.dp))
            t == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = stitchPrimary()) }
            mode == ReaderMode.TOC -> ReaderToc(t, edition.completeness, BookPrefs.lastRead(context, key)) { s, p -> open(s, p) }
            mode == ReaderMode.CHAPTER -> ReaderChapter(
                t, section, startPara, fontSize,
                onFont = { fontSize = it; SpiritualPrefs.setArabicFontSize(context, it) },
                isMarked = { p -> bookmarks.any { it.section == section && it.para == p } },
                onMark = { p, label -> BookPrefs.toggleBookmark(context, key, section, p, label); bookmarks = BookPrefs.bookmarks(context, key) },
                onPosition = { p -> BookPrefs.setLastRead(context, key, section, p) },
                onSection = { s -> open(s, 0) }
            )
            mode == ReaderMode.SEARCH -> {
                LaunchedEffect(query, key) {
                    snapshotFlow { query }.debounce(350).collect { q ->
                        if (q.trim().length < 2) { hits = null; return@collect }
                        searching = true
                        hits = withContext(Dispatchers.Default) { searchBook(t, q.trim()) }
                        searching = false
                    }
                }
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
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
                                    ReaderText(hit.snippet, t.rtl, fontSize * 0.7f)
                                }
                            }
                        }
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                if (bookmarks.isEmpty()) item { Text("No bookmarks yet. Tap the bookmark icon beside a paragraph.", color = stitchMutedText(), modifier = Modifier.padding(vertical = 16.dp)) }
                itemsIndexed(bookmarks) { _, b ->
                    StitchCard(Modifier.fillMaxWidth().clickable { open(b.section, b.para) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(t.sections.getOrNull(b.section)?.title.orEmpty(), color = StitchGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            ReaderText(b.label, t.rtl, fontSize * 0.7f)
                        }
                    }
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

/** Strips Arabic diacritics/tatweel so a search matches vocalised and unvocalised text alike. */
private fun normalizeForSearch(s: String): String =
    s.replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "").replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')

private fun langName(l: String) = when (l) { "ar" -> "العربية"; "fa" -> "فارسی"; "en" -> "English"; else -> l }

@Composable
private fun ReaderText(s: String, rtl: Boolean, size: Float) {
    Text(
        s, color = stitchText(), fontSize = size.sp, lineHeight = (size * if (rtl) 1.8f else 1.5f).sp,
        fontFamily = if (rtl) ReaderArabicFont else null, textAlign = if (rtl) TextAlign.Right else TextAlign.Start,
        style = LocalTextStyle.current.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ReaderToc(t: BookText, completeness: String, last: Pair<Int, Int>?, onOpen: (Int, Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Text(completeness, color = stitchMutedText(), fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            if (last != null && last.first in t.sections.indices) {
                Button(onClick = { onOpen(last.first, last.second) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.MenuBook, contentDescription = null); Spacer(Modifier.width(8.dp))
                    Text("Continue: " + t.sections[last.first].title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
            }
            Text("CONTENTS", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 6.dp))
        }
        itemsIndexed(t.sections) { i, s ->
            CompositionLocalProvider(LocalLayoutDirection provides if (t.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(i, 0) }.padding(start = ((s.level - 1).coerceIn(0, 3) * 14).dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        s.title, color = if (s.level <= 1) stitchText() else stitchMutedText(),
                        fontWeight = if (s.level <= 2) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = if (t.rtl) ReaderArabicFont else null, fontSize = if (s.level <= 1) 16.sp else 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${s.paras.size}¶", color = stitchMutedText(), fontSize = 11.sp)
                }
            }
            HorizontalDivider(color = stitchMutedText().copy(alpha = 0.15f))
        }
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
private fun ReaderChapter(
    t: BookText, section: Int, startPara: Int, fontSize: Float, onFont: (Float) -> Unit,
    isMarked: (Int) -> Boolean, onMark: (Int, String) -> Unit, onPosition: (Int) -> Unit, onSection: (Int) -> Unit
) {
    val s = t.sections.getOrNull(section) ?: return
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (startPara + 1).coerceAtMost(s.paras.size))
    LaunchedEffect(section) {
        snapshotFlow { state.firstVisibleItemIndex }.debounce(400).collect { onPosition((it - 1).coerceAtLeast(0)) }
    }
    val size = if (t.rtl) fontSize * 0.8f else (fontSize * 0.62f).coerceAtLeast(13f)
    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TextFields, contentDescription = "Font size", tint = stitchMutedText(), modifier = Modifier.size(18.dp))
                Slider(value = fontSize, onValueChange = onFont, valueRange = 18f..44f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Text("${section + 1}/${t.sections.size}", color = stitchMutedText(), fontSize = 12.sp)
            }
            Text(s.title, color = stitchPrimary(), fontWeight = FontWeight.Bold, fontSize = (size + 3).sp, fontFamily = if (t.rtl) ReaderArabicFont else null,
                textAlign = if (t.rtl) TextAlign.Right else TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
        itemsIndexed(s.paras) { i, p ->
            val marked = isMarked(i)
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { ReaderText(p, t.rtl, size) }
                IconButton(onClick = { onMark(i, p.take(80)) }, modifier = Modifier.size(32.dp)) {
                    Icon(if (marked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = if (marked) StitchGold else stitchMutedText().copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { onSection(section - 1) }, enabled = section > 0) { Text("Previous") }
                OutlinedButton(onClick = { onSection(section + 1) }, enabled = section < t.sections.lastIndex) { Text("Next") }
            }
        }
    }
}
