package com.noorpro.app.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.R
import com.noorpro.app.data.spiritual.SpiritualContentRepository
import com.noorpro.app.data.spiritual.SpiritualItem
import com.noorpro.app.data.spiritual.SpiritualPrefs
import com.noorpro.app.data.spiritual.SpiritualSection
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SpiritualArabicFont = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))
private const val FAV_CATEGORY = "__fav__"

/** Shared list/detail screen for the bundled Darood Sharif, Ruhani and Tasawwuf sections. */
@Composable
fun SpiritualContentScreen(viewModel: DeenViewModel, sectionId: String) {
    val context = LocalContext.current
    var section by remember(sectionId) { mutableStateOf<SpiritualSection?>(null) }
    var loadError by remember(sectionId) { mutableStateOf(false) }
    LaunchedEffect(sectionId) {
        try {
            section = withContext(Dispatchers.IO) { SpiritualContentRepository.load(context.applicationContext, sectionId) }
        } catch (_: Exception) {
            loadError = true
        }
    }
    var favourites by remember { mutableStateOf(SpiritualPrefs.favourites(context)) }
    var fontSize by remember { mutableFloatStateOf(SpiritualPrefs.arabicFontSize(context)) }
    var categoryId by rememberSaveable(sectionId) { mutableStateOf<String?>(null) }
    var itemId by rememberSaveable(sectionId) { mutableStateOf<String?>(null) }

    BackHandler(enabled = itemId != null || categoryId != null) {
        if (itemId != null) itemId = null else categoryId = null
    }

    val s = section
    val allItems = remember(s) { s?.categories?.flatMap { it.items }.orEmpty() }
    val item = allItems.firstOrNull { it.id == itemId }
    val category = s?.categories?.firstOrNull { it.id == categoryId }
    val title = when {
        item != null -> item.title
        categoryId == FAV_CATEGORY -> "Favourites"
        category != null -> category.title
        else -> s?.title ?: ""
    }

    StitchScreen {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = {
                    when {
                        itemId != null -> itemId = null
                        categoryId != null -> categoryId = null
                        else -> viewModel.goBack()
                    }
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchPrimary()) }
                Text(
                    title, color = stitchText(), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    fontFamily = com.noorpro.app.ui.theme.LibreCaslon, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item != null) {
                    val fav = item.id in favourites
                    IconButton(onClick = { favourites = SpiritualPrefs.toggleFavourite(context, item.id) }) {
                        Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favourite", tint = if (fav) StitchGold else stitchMutedText())
                    }
                }
            }
            when {
                loadError -> Text("Content could not be loaded.", color = stitchMutedText(), modifier = Modifier.padding(24.dp))
                s == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = stitchPrimary()) }
                item != null -> SpiritualItemDetail(viewModel, s, item, fontSize) { fontSize = it; SpiritualPrefs.setArabicFontSize(context, it) }
                categoryId == FAV_CATEGORY -> SpiritualItemList(allItems.filter { it.id in favourites }, favourites, "No favourites yet. Tap the heart on any item.") { itemId = it.id }
                category != null -> SpiritualItemList(category.items, favourites, null) { itemId = it.id }
                else -> SpiritualCategoryList(s, favourites.count { f -> allItems.any { it.id == f } }) { categoryId = it }
            }
        }
    }
}

@Composable
private fun SpiritualDisclaimer(text: String) {
    if (text.isBlank()) return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x33D4AF37)).padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = StitchGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = stitchText(), fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SpiritualCategoryList(section: SpiritualSection, favCount: Int, onOpen: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (section.titleUr.isNotBlank()) Text(section.titleUr, fontFamily = SpiritualArabicFont, fontSize = 24.sp, color = stitchPrimary())
                if (section.intro.isNotBlank()) Text(section.intro, color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp)
                SpiritualDisclaimer(section.disclaimer)
            }
        }
        item {
            StitchCard(Modifier.fillMaxWidth()) {
                Column {
                    StitchListRow("Favourites", "$favCount saved", Icons.Default.Favorite) { onOpen(FAV_CATEGORY) }
                    section.categories.forEach { c ->
                        HorizontalDivider(color = stitchMutedText().copy(alpha = 0.15f))
                        StitchListRow(c.title, "${c.items.size} items", Icons.Default.AutoStories) { onOpen(c.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpiritualItemList(items: List<SpiritualItem>, favourites: Set<String>, emptyText: String?, onOpen: (SpiritualItem) -> Unit) {
    if (items.isEmpty() && emptyText != null) {
        Text(emptyText, color = stitchMutedText(), modifier = Modifier.padding(24.dp))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { it ->
            StitchCard(Modifier.fillMaxWidth().clickable { onOpen(it) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(it.title, color = stitchText(), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (it.id in favourites) Icon(Icons.Default.Favorite, contentDescription = null, tint = StitchGold, modifier = Modifier.size(16.dp))
                    }
                    if (it.arabic.isNotBlank()) Text(
                        it.arabic, fontFamily = SpiritualArabicFont, fontSize = 20.sp, color = stitchPrimary(), maxLines = 2,
                        overflow = TextOverflow.Ellipsis, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                        modifier = Modifier.fillMaxWidth()
                    ) else if (it.english.isNotBlank()) Text(it.english, color = stitchMutedText(), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SpiritualItemDetail(viewModel: DeenViewModel, section: SpiritualSection, item: SpiritualItem, fontSize: Float, onFontSize: (Float) -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val shareText = remember(item) {
        buildString {
            appendLine(item.title)
            if (item.arabic.isNotBlank()) { appendLine(); appendLine(item.arabic) }
            if (item.translit.isNotBlank()) { appendLine(); appendLine(item.translit) }
            if (item.urdu.isNotBlank()) { appendLine(); appendLine(item.urdu) }
            if (item.english.isNotBlank()) { appendLine(); appendLine(item.english) }
            appendLine(); append("Source: ").appendLine(item.source)
            append("— NoorPro")
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (item.subtitle.isNotBlank()) Text(item.subtitle, color = stitchMutedText(), fontSize = 14.sp, fontStyle = FontStyle.Italic)
        SpiritualDisclaimer(section.disclaimer)
        if (item.arabic.isNotBlank()) {
            StitchCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        item.arabic, fontFamily = SpiritualArabicFont, fontSize = fontSize.sp, lineHeight = (fontSize * 1.9f).sp,
                        color = stitchText(), textAlign = TextAlign.Right,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = "Arabic font size", tint = stitchMutedText(), modifier = Modifier.size(18.dp))
                        Slider(value = fontSize, onValueChange = onFontSize, valueRange = 18f..44f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                        Text("${fontSize.toInt()}", color = stitchMutedText(), fontSize = 12.sp)
                    }
                }
            }
        }
        SpiritualTextBlock("Transliteration", item.translit, italic = true)
        SpiritualTextBlock("اردو", item.urdu, rtl = true)
        SpiritualTextBlock("English", item.english)
        SpiritualTextBlock("When / How many", item.whenText)
        SpiritualTextBlock("Virtue (as reported)", item.virtue)
        SpiritualTextBlock("Source", item.source)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(shareText)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copy")
            }
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                context.startActivity(Intent.createChooser(send, "Share"))
            }) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share")
            }
        }
        if (item.arabic.isNotBlank() && section.id != "tasawwuf") SpiritualCounter(viewModel, item)
    }
}

@Composable
private fun SpiritualTextBlock(label: String, text: String, italic: Boolean = false, rtl: Boolean = false) {
    if (text.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text(
            text, color = stitchText(), fontSize = if (rtl) 18.sp else 15.sp, lineHeight = if (rtl) 30.sp else 22.sp,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = if (rtl) SpiritualArabicFont else null,
            textAlign = if (rtl) TextAlign.Right else TextAlign.Start,
            style = if (rtl) LocalTextStyle.current.copy(textDirection = TextDirection.Rtl) else LocalTextStyle.current,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Counter that shares the Tasbih's per-day dhikr persistence (UserPreferencesRepository.updateDhikrCount). */
@Composable
private fun SpiritualCounter(viewModel: DeenViewModel, item: SpiritualItem) {
    val date = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val key = "Wird: " + item.title
    val persisted by viewModel.userPreferencesRepo.getDhikrCountFlow(date, key).collectAsState(initial = 0)
    var count by remember(key, persisted) { mutableIntStateOf(persisted) }
    val scope = rememberCoroutineScope()
    val target = if (item.count > 0) item.count else 0
    fun save(n: Int) { count = n.coerceAtLeast(0); scope.launch { viewModel.userPreferencesRepo.updateDhikrCount(date, key, count) } }
    StitchCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Counter (today)", color = stitchMutedText(), fontSize = 12.sp)
                Text(if (target > 0) "$count / $target" else "$count", color = stitchText(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { save(0) }) { Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = stitchMutedText()) }
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(stitchPrimary()).clickable { save(count + 1) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Fingerprint, contentDescription = "Count", tint = Color.White, modifier = Modifier.size(36.dp)) }
        }
    }
}
