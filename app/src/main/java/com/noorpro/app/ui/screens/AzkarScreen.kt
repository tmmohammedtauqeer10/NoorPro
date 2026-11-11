package com.noorpro.app.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.R
import com.noorpro.app.data.spiritual.SpiritualContentRepository
import com.noorpro.app.data.spiritual.SpiritualItem
import com.noorpro.app.data.spiritual.SpiritualSection
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.islamic.*
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

/** 1.2.0 (116): which Adhkar tab to open (set by the Dua hub's "Daily masnoon duas" entry). */
object AzkarNav { var initialTab: String = "" }

private val AdhkarArabic = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))

private data class AdhkarTab(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val ADHKAR_TABS = listOf(
    AdhkarTab("morning", "Morning", Icons.Outlined.WbSunny),
    AdhkarTab("evening", "Evening", Icons.Outlined.NightsStay),
    AdhkarTab("after_salah", "After Salah", Icons.Outlined.Mosque),
    AdhkarTab("daily", "Daily Duas", Icons.Outlined.AutoStories),
)

/**
 * 1.2.0 (116) Adhkar: morning / evening / after-salah adhkar and daily masnoon duas from assets/content/adhkar.jgz
 * (Hisn al-Muslim and al-Adhkar, every text referenced). Counters are per day and shared with the Tasbih store.
 */
@Composable
fun AzkarScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val section = remember { runCatching { SpiritualContentRepository.load(context.applicationContext, "adhkar") }.getOrNull() }
    val start = remember {
        AzkarNav.initialTab.ifBlank { if (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) in 4..14) "morning" else "evening" }
            .also { AzkarNav.initialTab = "" }
    }
    var tab by rememberSaveable { mutableStateOf(start) }
    var lang by rememberSaveable { mutableStateOf("en") }
    val back = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DUA_HUB) }
    androidx.activity.compose.BackHandler { back() }
    Column(Modifier.ikScreen()) {
        IkHeader("Adhkar", "Hisn al-Muslim · al-Adhkar", onBack = back)
        if (section == null) {
            IkEmptyState(Icons.Outlined.ErrorOutline, "Adhkar unavailable", "The bundled adhkar could not be loaded.", "adhkar_error")
            return@Column
        }
        AdhkarContent(viewModel, section, tab, lang, onTab = { tab = it }, onLang = { lang = it })
    }
}

@Composable
private fun AdhkarContent(viewModel: DeenViewModel, section: SpiritualSection, tab: String, lang: String, onTab: (String) -> Unit, onLang: (String) -> Unit) {
    val cat = section.categories.firstOrNull { it.id == tab } ?: section.categories.first()
    val date = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val counts = remember(cat.id) { mutableStateMapOf<String, Int>() }
    val counted = cat.items.filter { it.count > 0 }
    val done = counted.count { (counts[it.id] ?: 0) >= it.count }
    LazyColumn(Modifier.fillMaxSize().testTag("adhkar_list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ADHKAR_TABS.forEach { t -> IkPill(t.label, t.id == cat.id, icon = t.icon, tag = "adhkar_tab_${t.id}") { onTab(t.id) } }
            }
        }
        item {
            IkHero {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cat.title.uppercase(), color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (cat.id) { "morning" -> "From Fajr until the morning"; "evening" -> "From 'Asr until night"; "after_salah" -> "After every fard prayer"; else -> "Masnoon duas for the day" },
                            color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp
                        )
                        Text("${cat.items.size} adhkar · every text referenced", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    if (counted.isNotEmpty()) {
                        IkRing(if (counted.isEmpty()) 0f else done / counted.size.toFloat(), size = 74.dp, stroke = 7.dp) {
                            Text("$done/${counted.size}", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.testTag("adhkar_progress"))
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Meaning", color = stitchMutedText(), fontSize = 12.sp)
                listOf("en" to "English", "ur" to "اردو", "hi" to "हिन्दी").forEach { (k, l) -> IkPill(l, lang == k, tag = "adhkar_lang_$k") { onLang(k) } }
            }
        }
        items(cat.items, key = { it.id }) { item ->
            AdhkarCard(viewModel, item, date, lang) { counts[item.id] = it }
        }
        item {
            Text(section.disclaimer, color = stitchMutedText(), fontSize = 12.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(6.dp))
            Text(section.intro, color = stitchMutedText(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun AdhkarCard(viewModel: DeenViewModel, item: SpiritualItem, date: String, lang: String, onCount: (Int) -> Unit) {
    val key = "Adhkar: " + item.id.substringAfter('/')
    val persisted by viewModel.userPreferencesRepo.getDhikrCountFlow(date, key).collectAsState(initial = 0)
    var count by remember(key, persisted) { mutableIntStateOf(persisted) }
    LaunchedEffect(count) { onCount(count) }
    val scope = rememberCoroutineScope()
    val target = item.count
    val complete = target > 0 && count >= target
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    fun save(n: Int) { count = n.coerceAtLeast(0); scope.launch { viewModel.userPreferencesRepo.updateDhikrCount(date, key, count) } }
    IkCard(Modifier.animateContentSize().let { if (complete) it.border(1.5.dp, IkGold, RoundedCornerShape(20.dp)) else it }.testTag("adhkar_item_${item.id.substringAfter('/')}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (target > 1) Text("×$target", color = ikAccentText(), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(ikSoft()).padding(horizontal = 9.dp, vertical = 3.dp))
        }
        Spacer(Modifier.height(10.dp))
        val long = item.arabic.length > 420
        Text(
            if (long && !expanded) item.arabic.take(400) + " …" else item.arabic,
            fontFamily = AdhkarArabic, fontSize = 23.sp, lineHeight = 44.sp, color = stitchText(), textAlign = TextAlign.Right,
            style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl), modifier = Modifier.fillMaxWidth()
        )
        if (long) Text(if (expanded) "Show less" else "Show full text", color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp))
        if (item.translit.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(item.translit, color = stitchMutedText(), fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 19.sp)
        }
        val meaning = when (lang) { "ur" -> item.urdu; "hi" -> item.hindi.ifBlank { item.english }; else -> item.english }
        if (meaning.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                meaning, color = stitchText(), fontSize = if (lang == "ur") 17.sp else 14.sp, lineHeight = if (lang == "ur") 30.sp else 21.sp,
                fontFamily = if (lang == "ur") AdhkarArabic else null, textAlign = if (lang == "ur") TextAlign.Right else TextAlign.Start,
                style = if (lang == "ur") LocalTextStyle.current.copy(textDirection = TextDirection.Rtl) else LocalTextStyle.current,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (item.virtue.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ikSoft()).padding(10.dp)) {
                Icon(Icons.Outlined.Star, null, tint = IkGoldDeep, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(item.virtue, color = stitchText(), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.MenuBook, null, tint = stitchMutedText(), modifier = Modifier.size(14.dp).padding(top = 2.dp)); Spacer(Modifier.width(6.dp))
            Text(item.source, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.testTag("adhkar_ref"))
        }
        if (target > 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(stitchLine())) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((count.toFloat() / target).coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(if (complete) IkGold else IkGreen))
                }
                Spacer(Modifier.width(10.dp))
                Text("$count/$target", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.testTag("adhkar_count_${item.id.substringAfter('/')}"))
                if (count > 0) Icon(Icons.Outlined.Refresh, "Reset", tint = stitchMutedText(), modifier = Modifier.padding(start = 6.dp).size(34.dp).clip(CircleShape).clickable { save(0) }.padding(7.dp))
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(if (complete) IkGold else IkGreen)
                        .clickable { if (count < target) save(count + 1) }.testTag("adhkar_tap_${item.id.substringAfter('/')}"),
                    contentAlignment = Alignment.Center
                ) { Icon(if (complete) Icons.Outlined.Check else Icons.Outlined.TouchApp, if (complete) "Completed" else "Count", tint = Color.White, modifier = Modifier.size(24.dp)) }
            }
        }
    }
}
