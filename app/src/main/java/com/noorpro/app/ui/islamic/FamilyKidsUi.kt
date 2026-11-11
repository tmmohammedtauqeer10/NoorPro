package com.noorpro.app.ui.islamic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EmojiPeople
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.islamic.hifz.HifzAyah
import com.noorpro.app.islamic.hifz.HifzQuran
import com.noorpro.app.islamic.hifz.HifzRange
import com.noorpro.app.islamic.kids.*
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/* 1.1.6 Family & Kids UI (family_kids_enabled). Stateless contents + one wired screen. */

/** Deep, calm card tones (emerald, teal, amber, plum, navy) — pattern art only, never figures. */
internal val KidsHues = listOf(Color(0xFF0B6243), Color(0xFF1F5E73), Color(0xFF8A5A12), Color(0xFF5B3A62), Color(0xFF223A5E), Color(0xFF6B4A2B))
internal fun kidsHue(i: Int) = KidsHues[((i % KidsHues.size) + KidsHues.size) % KidsHues.size]

data class KidsHomeUi(val name: String = "", val stars: Int = 0, val today: Int = 0, val active: Boolean = false)

enum class PinMode { SET, CONFIRM, ENTER, GATE }

@Composable
fun FamilyKidsScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val kids = remember { KidsStore(ctx) }
    val family = remember { FamilyStore(ctx) }
    LaunchedEffect(Unit) { KidsMode.init(ctx) }
    val active = KidsMode.active
    val day = remember { LocalDate.now().toEpochDay() }
    var page by remember { mutableStateOf(if (KidsMode.active) "kids" else "hub") }
    var stars by remember { mutableIntStateOf(kids.stars()) }
    var today by remember { mutableIntStateOf(kids.todayCount(day)) }
    var surah by remember { mutableStateOf<KidsSurah?>(null) }
    var story by remember { mutableStateOf<ProphetStory?>(null) }
    var pinMode by remember { mutableStateOf(PinMode.SET) }
    var firstPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var gate by remember { mutableStateOf(KidsPin.gate()) }
    var members by remember { mutableStateOf(family.members()) }
    var log by remember { mutableStateOf(family.log()) }
    var famDay by remember { mutableLongStateOf(day) }
    var tick by remember { mutableIntStateOf(0) } // re-read "earned today" after a star

    fun earn(key: String) { if (kids.earn(key, day)) { stars = kids.stars(); today = kids.todayCount(day); tick++ } }
    fun earned(key: String) = tick >= 0 && kids.earnedToday(key, day)
    val home = if (active) "kids" else "hub"
    fun up() { page = when (page) { "surah" -> "surahs"; "story" -> "stories"; "surahs", "duas", "stories", "manners" -> "kids"; "pin" -> home; "kids" -> if (active) "kids" else "hub"; else -> home } }

    BackHandler(enabled = page != "hub" && !(page == "kids" && active)) { up() }
    // In kids mode, system back on the kids home asks for the parent PIN instead of leaving.
    BackHandler(enabled = page == "kids" && active) { pinMode = PinMode.ENTER; pinError = null; page = "pin" }

    val ui = KidsHomeUi(kids.childName(), stars, today, active)
    when (page) {
        "kids" -> KidsHomeContent(ui, onBack = if (active) null else ({ page = "hub" }), onOpen = { page = it },
            onParent = { pinMode = PinMode.ENTER; pinError = null; page = "pin" })
        "surahs" -> KidsSurahListContent(KIDS_SURAHS.map { it to earned("s${it.id}") }, onBack = ::up) { surah = it; page = "surah" }
        "surah" -> surah?.let { s ->
            val ayahs = remember(s.id) { runCatching { HifzQuran.ayahs(ctx, HifzRange(s.id, 1, 300)) }.getOrDefault(emptyList()) }
            KidsSurahContent(s, ayahs, earned("s${s.id}"), onBack = ::up,
                onListen = { viewModel.playQueue(listOf(com.noorpro.app.ui.screens.q(s.id)), 0) }, onStar = { earn("s${s.id}") })
        }
        "duas" -> KidsDuasContent(KIDS_DUAS.map { it to earned("d${it.id}") }, onBack = ::up) { earn("d${it.id}") }
        "stories" -> KidsStoriesContent(PROPHET_STORIES.map { it to earned("p${it.id}") }, onBack = ::up) { story = it; page = "story" }
        "story" -> story?.let { p -> KidsStoryContent(p, earned("p${p.id}"), onBack = ::up) { earn("p${p.id}") } }
        "manners" -> KidsMannersContent(MANNERS_CARDS.map { it to earned("m${it.id}") }, onBack = ::up) { earn("m${it.id}") }
        "family" -> FamilyPrayerContent(members, log, famDay, day, onBack = ::up,
            onDay = { famDay = (famDay + it).coerceAtMost(day) },
            onToggle = { id, p -> log = family.toggle(famDay, id, p) },
            onAdd = { n -> family.addMember(n); members = family.members() },
            onRemove = { id -> family.removeMember(id); members = family.members() })
        "pin" -> KidsPinContent(pinMode, pinError, gate.question, kids.lockedForMs(), onBack = ::up,
            onForgot = { gate = KidsPin.gate(); pinMode = PinMode.GATE; pinError = null }) { entered ->
            when (pinMode) {
                PinMode.SET -> if (KidsPin.isValidNew(entered)) { firstPin = entered; pinMode = PinMode.CONFIRM; pinError = null } else pinError = "Choose a PIN that's harder to guess."
                PinMode.CONFIRM -> if (entered == firstPin) { kids.setPin(entered); kids.setActive(true); page = "kids" } else { pinMode = PinMode.SET; pinError = "PINs didn't match. Try again." }
                PinMode.ENTER -> if (kids.checkPin(entered)) { kids.setActive(false); page = "hub" }
                    else pinError = if (kids.lockedForMs() > 0) "Too many tries. Wait 30 seconds." else "That PIN isn't right."
                PinMode.GATE -> if (entered.toIntOrNull() == gate.answer) { kids.clearPin(); kids.setActive(false); pinMode = PinMode.SET; pinError = null; page = "hub" }
                    else { gate = KidsPin.gate(); pinError = "Not quite — here's a new one." }
            }
        }
        else -> FamilyHubContent(ui, members.size, log[day].orEmpty().values.sumOf { com.noorpro.app.islamic.kids.FamilyLogic.count(it) },
            onBack = { viewModel.goBack() },
            onStartKids = { if (kids.hasPin()) { kids.setActive(true); page = "kids" } else { pinMode = PinMode.SET; pinError = null; page = "pin" } },
            onPreview = { page = "kids" }, onFamily = { page = "family" }, onName = { kids.setChildName(it); tick++ })
    }
}

/** Filled 8-point khatam star. */
@Composable
internal fun KhatamStar(modifier: Modifier = Modifier, color: Color = IkGold, outline: Boolean = false) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2; val ri = r * 0.72f
        val p = Path()
        for (k in 0 until 16) {
            val a = Math.toRadians(-90.0 + 22.5 * k).toFloat(); val rr = if (k % 2 == 0) r else ri
            val x = c.x + rr * cos(a); val y = c.y + rr * sin(a)
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        if (outline) drawPath(p, color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())) else drawPath(p, color)
    }
}

@Composable
private fun StarButton(done: Boolean, label: String, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(if (done) ikSoft() else IkGreen)
        .clickable(enabled = !done, onClick = onClick).padding(horizontal = 14.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        KhatamStar(Modifier.size(18.dp), if (done) IkGoldDeep else IkGold)
        Spacer(Modifier.width(8.dp))
        Text(if (done) "Star earned today" else label, color = if (done) ikAccentText() else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun StarCount(n: Int, modifier: Modifier = Modifier, onDark: Boolean = true) {
    Row(modifier.clip(RoundedCornerShape(50)).background(if (onDark) Color.White.copy(alpha = 0.12f) else ikSoft()).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically) {
        KhatamStar(Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
        Text("$n", color = if (onDark) IkCream else stitchText(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun FamilyHubContent(ui: KidsHomeUi, memberCount: Int, todayLogged: Int, onBack: () -> Unit, onStartKids: () -> Unit, onPreview: () -> Unit, onFamily: () -> Unit,
                     onName: (String) -> Unit, modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf(ui.name) }
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("family_hub")) {
        IkHeader("Family & Kids", "Learning and prayer, together", onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NOOR KIDS", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("A calm space for little Muslims", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Short surahs, daily du'as, stories of the Prophets and good manners. Reels, Ummah and messages stay hidden until you enter your PIN.",
                        color = IkCream.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.width(8.dp))
                Box(contentAlignment = Alignment.Center) {
                    KhatamStar(Modifier.size(78.dp), IkGold.copy(alpha = 0.18f))
                    KhatamStar(Modifier.size(52.dp), IkGold, outline = true)
                    Text("${ui.stars}", color = IkGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            IkField(name, { name = it; onName(it) }, "Child's name (optional)", tag = "kids_name", maxLen = 24)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = onStartKids).testTag("kids_start"),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = IkGreenDeep, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("Start Kids Mode", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(Modifier.height(46.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, IkCream.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).clickable(onClick = onPreview)
                    .padding(horizontal = 16.dp).testTag("kids_preview"), verticalAlignment = Alignment.CenterVertically) {
                    Text("Preview", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        IkSection("Family prayer tracker", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp))
            .clickable(onClick = onFamily).padding(14.dp).testTag("family_open"), verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(Icons.Outlined.FamilyRestroom, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (memberCount == 0) "Add your family" else "$memberCount in your family", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(if (memberCount == 0) "Tick off the five prayers together, day by day." else "$todayLogged of ${memberCount * 5} prayers ticked today",
                    color = stitchMutedText(), fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = stitchMutedText())
        }
        IkNotice("Private to this phone", "Kids progress and the family tracker are saved only on this device — no account, nothing uploaded.",
            Modifier.padding(16.dp), tag = "family_privacy")
    }
}

@Composable
private fun KidsTile(title: String, sub: String, icon: ImageVector, hue: Color, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(132.dp).clip(RoundedCornerShape(22.dp)).background(hue).clickable(onClick = onClick).testTag(tag)) {
        IkLattice(Modifier.matchParentSize(), IkGold.copy(alpha = 0.16f), 26.dp)
        Column(Modifier.padding(14.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = IkGold, modifier = Modifier.size(21.dp))
            }
            Column {
                Text(title, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(sub, color = IkCream.copy(alpha = 0.78f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun KidsHomeContent(ui: KidsHomeUi, onBack: (() -> Unit)?, onOpen: (String) -> Unit, onParent: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("kids_home")) {
        IkHeader("Noor Kids", if (ui.active) "Kids Mode is on" else "Preview", onBack) {
            if (ui.active) Row(Modifier.clip(RoundedCornerShape(50)).background(ikSoft()).clickable(onClick = onParent).padding(horizontal = 12.dp, vertical = 7.dp).testTag("kids_parent"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = ikAccentText(), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
                Text("Parent", color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("السَّلَامُ عَلَيْكُمْ", color = IkGold, fontFamily = QuranArabicFont, fontSize = 20.sp)
                    Text(if (ui.name.isBlank()) "Assalamu 'alaykum!" else "Assalamu 'alaykum, ${ui.name}!", color = IkCream, fontFamily = LibreCaslon,
                        fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Learn something new and collect a star for each one today.", color = IkCream.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp)
                }
                StarCount(ui.stars, Modifier.testTag("kids_stars"))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i -> KhatamStar(Modifier.padding(end = 6.dp).size(22.dp), if (i < ui.today) IkGold else IkGold.copy(alpha = 0.45f), outline = i >= ui.today) }
                Spacer(Modifier.width(6.dp))
                Text("${ui.today} today", color = IkCream.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KidsTile("Short Surahs", "${KIDS_SURAHS.size} to listen & learn", Icons.Outlined.MenuBook, kidsHue(0), "kids_tile_surahs", Modifier.weight(1f)) { onOpen("surahs") }
                KidsTile("Daily Du'as", "${KIDS_DUAS.size} everyday du'as", Icons.Outlined.VolunteerActivism, kidsHue(1), "kids_tile_duas", Modifier.weight(1f)) { onOpen("duas") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KidsTile("Prophet Stories", "${PROPHET_STORIES.size} stories from the Qur'an", Icons.Outlined.AutoStories, kidsHue(4), "kids_tile_stories", Modifier.weight(1f)) { onOpen("stories") }
                KidsTile("Good Manners", "${MANNERS_CARDS.size} adab cards", Icons.Outlined.EmojiPeople, kidsHue(2), "kids_tile_manners", Modifier.weight(1f)) { onOpen("manners") }
            }
        }
        if (ui.active) Text("Reels, Ummah and messages are hidden in Kids Mode.", color = stitchMutedText(), fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DoneDot(done: Boolean) {
    if (done) KhatamStar(Modifier.size(22.dp)) else KhatamStar(Modifier.size(22.dp), stitchLine(), outline = true)
}

@Composable
fun KidsSurahListContent(items: List<Pair<KidsSurah, Boolean>>, onBack: () -> Unit, modifier: Modifier = Modifier, onOpen: (KidsSurah) -> Unit) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("kids_surahs")) {
        IkHeader("Short Surahs", "Listen, repeat, learn", onBack)
        items.forEachIndexed { i, (s, done) ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikCard())
                .border(1.dp, stitchLine(), RoundedCornerShape(18.dp)).clickable { onOpen(s) }.padding(12.dp).testTag("kids_surah_${s.id}"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(kidsHue(i)), contentAlignment = Alignment.Center) {
                    IkLattice(Modifier.matchParentSize(), IkGold.copy(alpha = 0.2f), 16.dp)
                    Text("${s.id}", color = IkCream, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${s.name} · ${s.meaning}", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.note, color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(s.arabic, color = ikAccentText(), fontFamily = QuranArabicFont, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                DoneDot(done)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun KidsSurahContent(s: KidsSurah, ayahs: List<HifzAyah>, done: Boolean, onBack: () -> Unit, onListen: () -> Unit, onStar: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("kids_surah")) {
        IkHeader(s.name, s.meaning, onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("سُورَةُ ${s.arabic}", color = IkGold, fontFamily = QuranArabicFont, fontSize = 24.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(s.note, color = IkCream.copy(alpha = 0.85f), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(IkGold).clickable(onClick = onListen).testTag("kids_listen"),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, tint = IkGreenDeep); Spacer(Modifier.width(6.dp))
                Text("Listen", color = IkGreenDeep, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        if (s.id != 1 && s.id != 9) Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", color = ikAccentText(), fontFamily = QuranArabicFont, fontSize = 22.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (ayahs.isEmpty()) Text("The Arabic text is still loading on this device.", color = stitchMutedText(), fontSize = 13.sp)
            ayahs.forEachIndexed { i, a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(ikSoft()), contentAlignment = Alignment.Center) {
                        Text("${a.ayah}", color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(a.text, color = stitchText(), fontFamily = QuranArabicFont, fontSize = 26.sp, lineHeight = 46.sp,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl), textAlign = TextAlign.Right, modifier = Modifier.weight(1f))
                }
                if (i < ayahs.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(stitchLine().copy(alpha = 0.5f)))
            }
        }
        StarButton(done, "I practised it", "kids_star", Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onStar)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun KidsDuasContent(items: List<Pair<KidsDua, Boolean>>, onBack: () -> Unit, modifier: Modifier = Modifier, onStar: (KidsDua) -> Unit) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("kids_duas")) {
        IkHeader("Daily Du'as", "Little words, big rewards", onBack)
        items.forEachIndexed { i, (d, done) ->
            IkCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag("kids_dua_${d.id}"), padding = 0.dp) {
                Row(Modifier.fillMaxWidth().background(kidsHue(i)).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(d.title, color = IkCream, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Box(Modifier.clickable(enabled = !done) { onStar(d) }.testTag("kids_dua_star_${d.id}")) { KhatamStar(Modifier.size(22.dp), IkGold, outline = !done) }
                }
                Column(Modifier.padding(14.dp)) {
                    Text(d.arabic, color = stitchText(), fontFamily = QuranArabicFont, fontSize = 24.sp, lineHeight = 42.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(d.transliteration, color = ikAccentText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    Text(d.meaning, color = stitchText(), fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    Text(d.source, color = stitchMutedText(), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun KidsStoriesContent(items: List<Pair<ProphetStory, Boolean>>, onBack: () -> Unit, modifier: Modifier = Modifier, onOpen: (ProphetStory) -> Unit) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("kids_stories")) {
        IkHeader("Prophet Stories", "Stories from the Qur'an", onBack)
        items.forEach { (p, done) ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(104.dp).clip(RoundedCornerShape(22.dp)).background(kidsHue(p.hue))
                .clickable { onOpen(p) }.testTag("kids_story_${p.id}")) {
                IkLattice(Modifier.matchParentSize(), IkGold.copy(alpha = 0.15f), 28.dp)
                Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("peace be upon him", color = IkCream.copy(alpha = 0.7f), fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(p.lesson, color = IkCream.copy(alpha = 0.88f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(p.arabic.substringBefore(' '), color = IkGold, fontFamily = QuranArabicFont, fontSize = 26.sp)
                        if (done) KhatamStar(Modifier.size(16.dp))
                    }
                }
            }
        }
        Text("NoorPro never shows pictures of Prophets — only patterns and words.", color = stitchMutedText(), fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
fun KidsStoryContent(p: ProphetStory, done: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier, onStar: () -> Unit) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("kids_story")) {
        IkHeader(p.name, "Peace be upon him", onBack)
        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)).background(kidsHue(p.hue)), contentAlignment = Alignment.Center) {
            IkLattice(Modifier.matchParentSize(), IkGold.copy(alpha = 0.18f), 30.dp)
            Text(p.arabic, color = IkGold, fontFamily = QuranArabicFont, fontSize = 30.sp)
        }
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            p.paragraphs.forEachIndexed { i, para ->
                Row(Modifier.padding(vertical = 6.dp)) {
                    Text("${i + 1}", color = IkGoldDeep, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(18.dp))
                    Text(para, color = stitchText(), fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikSoft()).padding(14.dp).testTag("kids_lesson")) {
            Text("LESSON", color = ikAccentText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(p.lesson, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp)
            Text(p.refs, color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        StarButton(done, "I finished the story", "kids_star", Modifier.padding(16.dp).fillMaxWidth(), onStar)
    }
}

@Composable
fun KidsMannersContent(items: List<Pair<MannersCard, Boolean>>, onBack: () -> Unit, modifier: Modifier = Modifier, onStar: (MannersCard) -> Unit) {
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("kids_manners")) {
        IkHeader("Good Manners", "Adab of a Muslim", onBack)
        items.chunked(2).forEachIndexed { r, row ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { c, (m, done) ->
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, if (done) IkGold else stitchLine(), RoundedCornerShape(20.dp))
                        .testTag("kids_manner_${m.id}")) {
                        Box(Modifier.fillMaxWidth().height(10.dp).background(kidsHue(r * 2 + c)))
                        Column(Modifier.padding(12.dp).weight(1f)) {
                            Text(m.title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(m.body, color = stitchText(), fontSize = 12.sp, lineHeight = 17.sp)
                            Spacer(Modifier.weight(1f).heightIn(min = 6.dp))
                            Text(m.ref, color = stitchMutedText(), fontSize = 10.sp)
                        }
                        Row(Modifier.fillMaxWidth().clickable(enabled = !done) { onStar(m) }.background(if (done) ikSoft() else Color.Transparent).padding(10.dp).testTag("kids_manner_star_${m.id}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            KhatamStar(Modifier.size(16.dp), IkGold, outline = !done); Spacer(Modifier.width(6.dp))
                            Text(if (done) "Done today" else "I did this today", color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun KidsPinContent(mode: PinMode, error: String?, gateQuestion: String, lockedMs: Long, onBack: () -> Unit, onForgot: () -> Unit, modifier: Modifier = Modifier, onDone: (String) -> Unit) {
    var digits by remember(mode, error) { mutableStateOf("") }
    val len = if (mode == PinMode.GATE) 3 else KidsPin.LENGTH
    val title = when (mode) { PinMode.SET -> "Set a parent PIN"; PinMode.CONFIRM -> "Confirm your PIN"; PinMode.ENTER -> "Parent PIN"; PinMode.GATE -> "Grown-ups only" }
    val sub = when (mode) {
        PinMode.SET -> "You'll need it to leave Kids Mode."; PinMode.CONFIRM -> "Enter the same 4 digits again."
        PinMode.ENTER -> "Enter your PIN to leave Kids Mode."; PinMode.GATE -> "Answer to reset the PIN: $gateQuestion = ?"
    }
    fun press(d: Char) { if (digits.length < len) { digits += d; if (mode != PinMode.GATE && digits.length == len) { val v = digits; digits = ""; onDone(v) } } }
    Column(modifier.ikScreen().testTag("kids_pin"), horizontalAlignment = Alignment.CenterHorizontally) {
        IkHeader("", null, onBack)
        Box(Modifier.size(64.dp).clip(CircleShape).background(ikSoft()), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Lock, null, tint = ikAccentText(), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(sub, color = stitchMutedText(), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp).testTag("kids_pin_sub"))
        Spacer(Modifier.height(14.dp))
        if (mode == PinMode.GATE) Text(digits.ifEmpty { "—" }, color = stitchText(), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(len) { i -> Box(Modifier.size(16.dp).clip(CircleShape).background(if (i < digits.length) IkGreen else Color.Transparent).border(2.dp, if (i < digits.length) IkGreen else stitchLine(), CircleShape)) }
        }
        Text(error ?: if (lockedMs > 0) "Too many tries. Wait a moment." else " ", color = IkRed, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp).testTag("kids_pin_error"))
        Spacer(Modifier.weight(1f))
        val keys = listOf("123", "456", "789")
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(vertical = 7.dp)) { row.forEach { PinKey("$it", "pin_$it") { press(it) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(vertical = 7.dp)) {
            if (mode == PinMode.GATE) PinKey(null, "pin_ok", icon = Icons.Default.Check) { val v = digits; digits = ""; onDone(v) } else Spacer(Modifier.size(74.dp))
            PinKey("0", "pin_0") { press('0') }
            PinKey(null, "pin_del", icon = Icons.AutoMirrored.Filled.Backspace) { digits = digits.dropLast(1) }
        }
        Text(if (mode == PinMode.ENTER) "Forgot PIN?" else " ", color = ikAccentText(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp).clickable(enabled = mode == PinMode.ENTER, onClick = onForgot).testTag("kids_pin_forgot"))
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PinKey(label: String?, tag: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Box(Modifier.size(74.dp).clip(CircleShape).background(ikCard()).border(1.dp, stitchLine(), CircleShape).clickable(onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, null, tint = stitchText()) else Text(label.orEmpty(), color = stitchText(), fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FamilyPrayerContent(members: List<FamilyMember>, log: Map<Long, Map<String, Int>>, day: Long, today: Long, onBack: () -> Unit, onDay: (Int) -> Unit,
                        onToggle: (String, Int) -> Unit, onAdd: (String) -> Unit, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val dayMap = log[day].orEmpty()
    val label = when (day) { today -> "Today"; today - 1 -> "Yesterday"
        else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())) }
    val week = (today - 6..today).toList()
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("family_tracker")) {
        IkHeader("Family Prayers", "Saved only on this phone", onBack) {
            if (members.isNotEmpty()) Text(if (editing) "Done" else "Edit", color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { editing = !editing }.padding(10.dp).testTag("family_edit"))
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ikSoft()).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).clickable { onDay(-1) }.testTag("family_prev"), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day", tint = ikAccentText()) }
            Text(label, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).testTag("family_day"))
            Box(Modifier.size(38.dp).clip(CircleShape).clickable(enabled = day < today) { onDay(1) }.testTag("family_next"), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day", tint = if (day < today) ikAccentText() else stitchLine())
            }
        }
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), padding = 10.dp) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(96.dp))
                FAMILY_PRAYERS.forEach { Text(it.take(3), color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
            }
            if (members.isEmpty()) Text("Add family members to start ticking prayers together.", color = stitchMutedText(), fontSize = 13.sp, modifier = Modifier.padding(8.dp).testTag("family_empty"))
            members.forEach { m ->
                val mask = dayMap[m.id] ?: 0
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).testTag("family_row_${m.id}"), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.width(96.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (editing) Box(Modifier.size(24.dp).clip(CircleShape).clickable { onRemove(m.id) }.testTag("family_remove_${m.id}"), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, "Remove", tint = IkRed, modifier = Modifier.size(16.dp)) }
                        else Box(Modifier.size(24.dp).clip(CircleShape).background(kidsHue(m.hue)), contentAlignment = Alignment.Center) {
                            Text(m.name.take(1).uppercase(), color = IkCream, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(6.dp))
                        Text(m.name, color = stitchText(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FAMILY_PRAYERS.indices.forEach { p ->
                        val on = FamilyLogic.has(mask, p)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (on) IkGreen else Color.Transparent)
                                .border(1.5.dp, if (on) IkGreen else stitchLine(), RoundedCornerShape(10.dp)).clickable { onToggle(m.id, p) }.testTag("family_${m.id}_$p"),
                                contentAlignment = Alignment.Center) {
                                if (on) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            if (adding) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { IkField(name, { name = it }, "Name", tag = "family_name", maxLen = 24) }
                    Spacer(Modifier.width(8.dp))
                    IkButton("Add", enabled = name.isNotBlank(), tag = "family_add_ok") { onAdd(name); name = ""; adding = false }
                }
            } else if (members.size < FamilyStore.MAX_MEMBERS) Row(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).clickable { adding = true }.padding(8.dp).testTag("family_add"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PersonAdd, null, tint = ikAccentText(), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("Add family member", color = ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (members.isNotEmpty()) {
            IkSection("This week", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            IkCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("family_week"), padding = 12.dp) {
                members.forEach { m ->
                    val counts = FamilyLogic.week(log, m.id, week)
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, color = stitchText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(80.dp))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                            counts.forEach { c ->
                                Box(Modifier.weight(1f).height(26.dp), contentAlignment = Alignment.BottomCenter) {
                                    Box(Modifier.fillMaxWidth().fillMaxHeight(maxOf(c, 0) / 5f + 0.08f).clip(RoundedCornerShape(4.dp))
                                        .background(if (c == 5) IkGold else if (c > 0) IkGreen.copy(alpha = 0.35f + c * 0.12f) else stitchLine()))
                                }
                            }
                        }
                        Text("${counts.sum()}/35", color = stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
                    }
                }
                Text("Every prayer counts — gold means all five that day.", color = stitchMutedText(), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

