package com.noorpro.app.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.noorpro.app.data.QuranMetaData
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LinesPerPage = 16
private val QuranFont = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))

private data class ReaderColors(
    val outside: Color, val paper: Color, val edge: Color, val ink: Color,
    val muted: Color, val primary: Color, val gold: Color, val rule: Color
)
private val DayColors = ReaderColors(
    Color(0xFFF4EFE3), Color(0xFFFFFBEF), Color(0xFFE5D8B8), Color(0xFF17140F),
    Color(0xFF6B6255), Color(0xFF07583F), Color(0xFFB98A28), Color(0xFFCCB878)
)
private val NightColors = ReaderColors(
    Color(0xFF111A18), Color(0xFF1B2622), Color(0xFF3B5148), Color(0xFFF4EBD5),
    Color(0xFFB8B1A2), Color(0xFF0A4938), Color(0xFFD6B755), Color(0xFF6C6347)
)

internal data class IndoPakAyah(val surah: Int, val number: Int, val text: String)
internal enum class MushafLineKind { SURAH_TITLE, BISMILLAH, QURAN, BLANK }
internal data class MushafLine(
    val text: String,
    val kind: MushafLineKind,
    val firstAyah: Int? = null,
    val lastAyah: Int? = null
)
internal data class MushafPage(val lines: List<MushafLine>, val firstAyah: Int, val lastAyah: Int)
private data class WordToken(val text: String, val ayah: Int)

/** Complete offline Indo-Pak source. Numeric word records are metadata, not Quran text. */
private object IndoPakSource {
    @Volatile private var cache: Map<Int, List<IndoPakAyah>>? = null

    suspend fun load(context: Context): Map<Int, List<IndoPakAyah>> = withContext(Dispatchers.IO) {
        cache ?: synchronized(this@IndoPakSource) {
            cache ?: read(context).also {
                check(it.size == 114 && it.values.sumOf(List<IndoPakAyah>::size) == 6236) {
                    "Bundled Indo-Pak Quran is incomplete"
                }
                cache = it
            }
        }
    }

    private fun read(context: Context): Map<Int, List<IndoPakAyah>> {
        val words = HashMap<Int, HashMap<Int, ArrayList<Pair<Int, String>>>>()
        context.assets.open("quran/indopak.json").use { input ->
            android.util.JsonReader(input.reader(Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    reader.nextName()
                    reader.beginObject()
                    var surah = 0
                    var ayah = 0
                    var word = 0
                    var text = ""
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "surah" -> surah = reader.nextString().toIntOrNull() ?: 0
                            "ayah" -> ayah = reader.nextString().toIntOrNull() ?: 0
                            "word" -> word = reader.nextString().toIntOrNull() ?: 0
                            "text" -> text = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    val clean = normalizeWord(text)
                    if (surah in 1..114 && ayah > 0 && clean.isNotBlank() && !clean.isMarker()) {
                        words.getOrPut(surah) { HashMap() }
                            .getOrPut(ayah) { ArrayList() }
                            .add(word to clean)
                    }
                }
                reader.endObject()
            }
        }
        return words.mapValues { (surah, ayahs) ->
            ayahs.entries.sortedBy { it.key }.map { (number, entries) ->
                IndoPakAyah(surah, number, entries.sortedBy { it.first }.joinToString(" ") { it.second })
            }
        }
    }
}

@Composable
fun IndoPakQuranScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("indopak_reader", Context.MODE_PRIVATE) }
    var data by remember { mutableStateOf<Map<Int, List<IndoPakAyah>>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var surah by remember { mutableStateOf(prefs.getInt("last_surah", 1).coerceIn(1, 114)) }
    var fontSize by remember { mutableStateOf(prefs.getInt("font_size", 25).coerceIn(21, 31)) }
    var night by remember { mutableStateOf(prefs.getBoolean("night", false)) }
    var picker by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var bookmarks by remember {
        mutableStateOf(prefs.getStringSet("page_bookmarks", emptySet()).orEmpty().toSet())
    }
    val colors = if (night) NightColors else DayColors

    LaunchedEffect(Unit) {
        runCatching { IndoPakSource.load(context) }
            .onSuccess { data = it }
            .onFailure { loadFailed = true }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.outside)) {
        val textWidth = with(density) {
            (maxWidth.coerceAtMost(620.dp) - 62.dp).coerceAtLeast(210.dp).toPx()
        }
        val typeface = remember {
            ResourcesCompat.getFont(context, R.font.noto_naskh_arabic) ?: Typeface.SERIF
        }
        val textPx = with(density) { fontSize.sp.toPx() }
        val ayahs = data?.get(surah).orEmpty()
        val arabicName = QuranMetaData.surahNamesAr.getOrElse(surah - 1) { "" }
        val pages = remember(ayahs, textWidth, textPx, arabicName) {
            if (ayahs.isEmpty()) emptyList() else {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    textSize = textPx
                    isSubpixelText = true
                }
                buildMushafPages(
                    ayahs = ayahs,
                    surahNameArabic = arabicName,
                    maxWidthPx = textWidth,
                    measureText = { paint.measureText(it) }
                )
            }
        }
        val initial = prefs.getInt("last_page_$surah", 0)
            .coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val pager = rememberPagerState(initialPage = initial) { pages.size.coerceAtLeast(1) }

        LaunchedEffect(surah, pages.size) {
            if (pages.isNotEmpty()) {
                pager.scrollToPage(prefs.getInt("last_page_$surah", 0).coerceIn(0, pages.lastIndex))
            }
        }
        LaunchedEffect(surah, pager.currentPage, pages.size) {
            if (pages.isNotEmpty()) {
                prefs.edit().putInt("last_surah", surah)
                    .putInt("last_page_$surah", pager.currentPage).apply()
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (controls) ReaderTopBar(
                    number = surah,
                    name = QuranMetaData.surahNamesEn.getOrElse(surah - 1) { "" },
                    bookmarked = "$surah:${pager.currentPage}" in bookmarks,
                    colors = colors,
                    onBack = { viewModel.navigateTo(DeenScreen.LIBRARY_DASHBOARD) },
                    onPick = { picker = true },
                    onSettings = { settings = true },
                    onBookmark = {
                        val key = "$surah:${pager.currentPage}"
                        bookmarks = bookmarks.toMutableSet().apply {
                            if (!add(key)) remove(key)
                        }.toSet()
                        prefs.edit().putStringSet("page_bookmarks", bookmarks).apply()
                    }
                )
            },
            bottomBar = {
                if (controls && pages.isNotEmpty()) PageBar(
                    page = pager.currentPage,
                    count = pages.size,
                    juz = juzFor(surah, pages[pager.currentPage].firstAyah),
                    colors = colors,
                    previous = {
                        when {
                            pager.currentPage > 0 -> scope.launch {
                                pager.animateScrollToPage(pager.currentPage - 1)
                            }
                            surah > 1 -> surah--
                        }
                    },
                    next = {
                        when {
                            pager.currentPage < pages.lastIndex -> scope.launch {
                                pager.animateScrollToPage(pager.currentPage + 1)
                            }
                            surah < 114 -> surah++
                        }
                    }
                )
            }
        ) { padding ->
            when {
                loadFailed -> ReaderMessage("Unable to open the bundled Quran.", colors, Modifier.padding(padding))
                data == null -> LoadingReader(colors, Modifier.padding(padding))
                pages.isEmpty() -> ReaderMessage("This surah is unavailable offline.", colors, Modifier.padding(padding))
                else -> HorizontalPager(
                    state = pager,
                    key = { "$surah-$fontSize-$it" },
                    modifier = Modifier.fillMaxSize().padding(padding).pointerInput(Unit) {
                        detectTapGestures { controls = !controls }
                    }
                ) { index ->
                    PageSheet(
                        page = pages[index],
                        pageNumber = index + 1,
                        pageCount = pages.size,
                        surahName = QuranMetaData.surahNamesEn.getOrElse(surah - 1) { "" },
                        juz = juzFor(surah, pages[index].firstAyah),
                        fontSize = fontSize,
                        colors = colors
                    )
                }
            }
        }

        if (picker) SurahPicker(surah, colors, { picker = false }) {
            surah = it
            picker = false
        }
        if (settings) ReaderSettings(fontSize, night, colors, { settings = false }, {
            fontSize = it
            prefs.edit().putInt("font_size", it).apply()
        }, {
            night = it
            prefs.edit().putBoolean("night", it).apply()
        })
    }
}

@Composable
private fun ReaderTopBar(
    number: Int,
    name: String,
    bookmarked: Boolean,
    colors: ReaderColors,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onSettings: () -> Unit,
    onBookmark: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(colors.primary).statusBarsPadding()
            .height(64.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onPick)
                .padding(horizontal = 5.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$number. $name", color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text("Indo-Pak • 16 line", color = Color.White.copy(alpha = .72f), fontSize = 10.sp)
            }
            Icon(Icons.Default.ExpandMore, null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        TextButton(onClick = onSettings, modifier = Modifier.width(44.dp)) {
            Text("Aa", color = Color.White, fontWeight = FontWeight.Bold)
        }
        IconButton(onBookmark) {
            Icon(
                if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                if (bookmarked) "Remove bookmark" else "Bookmark page",
                tint = if (bookmarked) colors.gold else Color.White
            )
        }
    }
}

@Composable
private fun PageSheet(
    page: MushafPage,
    pageNumber: Int,
    pageCount: Int,
    surahName: String,
    juz: Int,
    fontSize: Int,
    colors: ReaderColors
) {
    val density = LocalDensity.current
    val rowHeight = with(density) { (fontSize.sp.toPx() * 1.48f).toDp() }.coerceAtLeast(35.dp)
    Box(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 620.dp).padding(12.dp)
                .shadow(8.dp, RoundedCornerShape(7.dp), ambientColor = Color.Black.copy(alpha = .14f))
                .clip(RoundedCornerShape(7.dp)).background(colors.paper).mushafFrame(colors)
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeaderText("Juz $juz", colors)
                HeaderText(surahName, colors)
                HeaderText("16 lines", colors)
            }
            Spacer(Modifier.height(5.dp))
            page.lines.forEach { MushafRow(it, fontSize, rowHeight, colors) }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ayah ${page.firstAyah}–${page.lastAyah}", color = colors.muted, fontSize = 10.sp)
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(toArabicIndic(pageNumber), color = Color.White, fontFamily = QuranFont)
                }
                Text("$pageNumber / $pageCount", color = colors.muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun HeaderText(text: String, colors: ReaderColors) {
    Text(text, color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun MushafRow(line: MushafLine, fontSize: Int, height: Dp, colors: ReaderColors) {
    val title = line.kind == MushafLineKind.SURAH_TITLE
    Box(
        Modifier.fillMaxWidth().height(height).drawBehind {
            drawLine(
                colors.rule.copy(alpha = if (line.kind == MushafLineKind.BLANK) .2f else .52f),
                Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f
            )
            if (title) {
                drawRoundRect(
                    colors.primary.copy(alpha = .1f), Offset(2f, 3f),
                    Size(size.width - 4f, size.height - 7f), CornerRadius(12f)
                )
                drawRoundRect(
                    colors.gold, Offset(2f, 3f), Size(size.width - 4f, size.height - 7f),
                    CornerRadius(12f), style = Stroke(1.5f)
                )
            }
        },
        contentAlignment = Alignment.Center
    ) {
        if (line.kind != MushafLineKind.BLANK) Text(
            text = line.text,
            color = if (line.kind == MushafLineKind.QURAN) colors.ink else colors.primary,
            fontSize = (if (title) fontSize - 3 else if (line.kind == MushafLineKind.BISMILLAH) fontSize - 2 else fontSize)
                .coerceAtLeast(19).sp,
            fontFamily = QuranFont,
            fontWeight = if (title) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            style = TextStyle(textDirection = TextDirection.Rtl),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun Modifier.mushafFrame(colors: ReaderColors) = drawBehind {
    val a = 5.dp.toPx()
    val b = 10.dp.toPx()
    drawRoundRect(colors.gold, style = Stroke(2.dp.toPx()), cornerRadius = CornerRadius(10.dp.toPx()))
    drawRoundRect(
        colors.primary, Offset(a, a), Size(size.width - a * 2, size.height - a * 2),
        CornerRadius(8.dp.toPx()), style = Stroke(1.dp.toPx())
    )
    drawRoundRect(
        colors.gold.copy(alpha = .68f), Offset(b, b), Size(size.width - b * 2, size.height - b * 2),
        CornerRadius(6.dp.toPx()), style = Stroke(.7.dp.toPx())
    )
    val radius = 5.dp.toPx()
    val step = (size.height - b * 4) / 8f
    for (index in 1..7) {
        val y = b * 2 + step * index
        diamond(Offset(a, y), radius, colors.gold)
        diamond(Offset(size.width - a, y), radius, colors.gold)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.diamond(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(path, color)
}

@Composable
private fun PageBar(
    page: Int,
    count: Int,
    juz: Int,
    colors: ReaderColors,
    previous: () -> Unit,
    next: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(colors.primary).navigationBarsPadding()
            .height(62.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(previous) {
            Icon(Icons.AutoMirrored.Filled.NavigateBefore, "Previous page", tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Page ${page + 1} of $count", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Juz $juz • Swipe to turn page", color = Color.White.copy(alpha = .72f), fontSize = 10.sp)
        }
        IconButton(next) {
            Icon(Icons.AutoMirrored.Filled.NavigateNext, "Next page", tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun ReaderSettings(
    fontSize: Int,
    night: Boolean,
    colors: ReaderColors,
    close: () -> Unit,
    setFont: (Int) -> Unit,
    setNight: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = close) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.paper).padding(22.dp)
        ) {
            Text("Reading settings", color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text("Arabic text size", color = colors.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(21, 23, 25, 27, 29, 31).forEach { size ->
                    Box(
                        Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(11.dp))
                            .background(if (size == fontSize) colors.primary else colors.edge)
                            .clickable { setFont(size) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$size", color = if (size == fontSize) Color.White else colors.ink,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(colors.edge.copy(alpha = .65f)).clickable { setNight(!night) }
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (night) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = colors.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (night) "Use light paper" else "Use night reading", color = colors.ink, fontWeight = FontWeight.SemiBold)
                    Text("Comfortable reading in different light", color = colors.muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Text is measured again after a size change so every page keeps 16 clean lines.",
                color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(close) { Text("Done", color = colors.primary, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SurahPicker(
    selected: Int,
    colors: ReaderColors,
    close: () -> Unit,
    select: (Int) -> Unit
) {
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f))
                .padding(horizontal = 22.dp, vertical = 42.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = 420.dp).heightIn(max = 720.dp)
                    .clip(RoundedCornerShape(26.dp)).background(colors.paper)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 15.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Choose Surah", color = colors.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("All 114 surahs available offline", color = colors.muted, fontSize = 12.sp)
                    }
                    TextButton(close) { Text("Close", color = colors.primary) }
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(114) { index ->
                        val number = index + 1
                        val active = number == selected
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(if (active) colors.primary.copy(alpha = .12f) else Color.Transparent)
                                .clickable { select(number) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (active) colors.primary else colors.edge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$number", color = if (active) Color.White else colors.ink,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                QuranMetaData.surahNamesEn.getOrElse(index) { "" },
                                color = colors.ink, fontSize = 16.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                QuranMetaData.surahNamesAr.getOrElse(index) { "" },
                                color = colors.primary, fontSize = 21.sp, fontFamily = QuranFont,
                                style = TextStyle(textDirection = TextDirection.Rtl)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingReader(colors: ReaderColors, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = colors.primary)
            Spacer(Modifier.height(14.dp))
            Text("Preparing offline Quran pages…", color = colors.muted)
        }
    }
}

@Composable
private fun ReaderMessage(message: String, colors: ReaderColors, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = colors.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(30.dp))
    }
}

internal fun buildMushafPages(
    ayahs: List<IndoPakAyah>,
    surahNameArabic: String,
    maxWidthPx: Float,
    measureText: (String) -> Float,
    linesPerPage: Int = LinesPerPage
): List<MushafPage> {
    if (ayahs.isEmpty() || maxWidthPx <= 0f || linesPerPage < 1) return emptyList()
    val intro = mutableListOf(MushafLine("سُورَةُ $surahNameArabic", MushafLineKind.SURAH_TITLE))
    val body: List<IndoPakAyah>
    if (ayahs.first().surah == 1) {
        val first = ayahs.first()
        intro += MushafLine(
            "${first.text}\u00A0﴿${toArabicIndic(first.number)}﴾",
            MushafLineKind.BISMILLAH, first.number, first.number
        )
        body = ayahs.drop(1)
    } else {
        if (ayahs.first().surah != 9) {
            intro += MushafLine("بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ", MushafLineKind.BISMILLAH)
        }
        body = ayahs
    }

    val tokens = buildList {
        body.forEach { ayah ->
            val words = ayah.text.split(Regex("\\s+")).filter(String::isNotBlank)
            words.forEachIndexed { index, word ->
                add(
                    WordToken(
                        if (index == words.lastIndex) "$word\u00A0﴿${toArabicIndic(ayah.number)}﴾" else word,
                        ayah.number
                    )
                )
            }
        }
    }
    val lines = mutableListOf<MushafLine>()
    var current = mutableListOf<WordToken>()
    fun flush() {
        if (current.isEmpty()) return
        lines += MushafLine(
            current.joinToString(" ") { it.text }, MushafLineKind.QURAN,
            current.first().ayah, current.last().ayah
        )
        current = mutableListOf()
    }
    tokens.forEach { token ->
        val candidate = (current + token).joinToString(" ") { it.text }
        if (current.isNotEmpty() && measureText(candidate) > maxWidthPx) flush()
        current += token
    }
    flush()

    return (intro + lines).chunked(linesPerPage).map { content ->
        val first = content.firstNotNullOfOrNull { it.firstAyah }
            ?: body.firstOrNull()?.number ?: ayahs.first().number
        val last = content.asReversed().firstNotNullOfOrNull { it.lastAyah } ?: first
        MushafPage(
            content + List(linesPerPage - content.size) { MushafLine("", MushafLineKind.BLANK) },
            first, last
        )
    }
}

internal fun normalizeWord(value: String) =
    value.replace("\u200E", "")
        .replace("\u200F", "")
        .replace("\u2002", " ")
        .filterNot { it == '\u200B' || it == '\uFEFF' || it.code in 0xE000..0xF8FF }
        .trim()

private fun String.isMarker() =
    isNotBlank() && all { it in '\u0660'..'\u0669' || it in '\u06F0'..'\u06F9' }

private fun juzFor(surah: Int, ayah: Int): Int {
    val starts = listOf(
        1 to 1, 2 to 142, 2 to 253, 3 to 93, 4 to 24, 4 to 148,
        5 to 82, 6 to 111, 7 to 88, 8 to 41, 9 to 93, 11 to 6,
        12 to 53, 15 to 1, 17 to 1, 18 to 75, 21 to 1, 23 to 1,
        25 to 21, 27 to 56, 29 to 46, 33 to 31, 36 to 28, 39 to 32,
        41 to 47, 46 to 1, 51 to 31, 58 to 1, 67 to 1, 78 to 1
    )
    return starts.indexOfLast {
        it.first < surah || (it.first == surah && it.second <= ayah)
    }.coerceAtLeast(0) + 1
}

internal fun toArabicIndic(number: Int): String {
    val digits = "٠١٢٣٤٥٦٧٨٩"
    return number.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}
