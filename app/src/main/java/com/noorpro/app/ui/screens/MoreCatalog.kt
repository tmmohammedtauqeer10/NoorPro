package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.pressScale
import com.noorpro.app.ui.components.staggeredEntrance
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen

/** One destination in the "More" / "See All" catalog. Each screen appears exactly once. */
data class MoreItem(val title: String, val keywords: String, val icon: ImageVector, val screen: DeenScreen, val flag: String? = null) {
    /** 1.1.6: items with a Remote Config [flag] only appear when it is on. */
    val visible: Boolean get() = (flag == null || com.noorpro.app.islamic.IslamicFlags.isOn(flag)) &&
        !(com.noorpro.app.islamic.kids.KidsMode.enforced && com.noorpro.app.islamic.kids.KidsMode.blocks(screen))
}
data class MoreCategory(val title: String, val icon: ImageVector, val items: List<MoreItem>)

/** Single source of truth for More tab + Home "See All" (1.0.73). Umrah Essentials Kit stays hidden. */
val moreCatalog: List<MoreCategory> = listOf(
    MoreCategory("Worship", Icons.Default.Mosque, listOf(
        MoreItem("Prayer Times", "salah namaz adhan", Icons.Default.AccessTime, DeenScreen.PRAYER_TIMES),
        MoreItem("Qibla", "direction kaaba compass", Icons.Default.Explore, DeenScreen.QIBLA_MORE),
        MoreItem("Adhan Sound", "adhan sound voice tone alarm notification vibrate preview prayer time", Icons.Default.NotificationsActive, DeenScreen.ADHAN_VOICES, com.noorpro.app.islamic.IslamicFlags.ADHAN_VOICES),
        MoreItem("Masjid Pages", "masjid mosque jamaat jummah iqamah bayan announcements", Icons.Default.Mosque, DeenScreen.MASJID_PAGES, com.noorpro.app.islamic.IslamicFlags.MASJID_PAGES),
        MoreItem("Prayer Tracker", "qaza missed", Icons.Default.CheckCircle, DeenScreen.QAZA_TRACKER),
        MoreItem("Tasbih", "dhikr counter", Icons.Default.Fingerprint, DeenScreen.TASBIH),
        MoreItem("Azkar", "morning evening adhkar", Icons.Default.WbTwilight, DeenScreen.AZKAR),
        MoreItem("Duas", "supplication", Icons.Default.VolunteerActivism, DeenScreen.DUA_HUB),
        MoreItem("Hajj & Umrah", "pilgrimage", Icons.Default.TravelExplore, DeenScreen.HAJJ_UMRAH),
    )),
    MoreCategory("Quran & Learning", Icons.AutoMirrored.Filled.MenuBook, listOf(
        MoreItem("Quran", "read surah translation", Icons.AutoMirrored.Filled.MenuBook, DeenScreen.QURAN),
        MoreItem("Indo-Pak Quran", "mushaf 15 line", Icons.Default.ImportContacts, DeenScreen.INDOPAK_QURAN),
        MoreItem("Bookmarks", "saved verses", Icons.Default.Bookmark, DeenScreen.BOOKMARKS),
        MoreItem("Daily Deen Goals", "goals checklist habits streak salah adhkar sadaqah dua daily routine", Icons.Default.TaskAlt, DeenScreen.DEEN_GOALS, com.noorpro.app.islamic.IslamicFlags.DEEN_GOALS),
        MoreItem("Dua Requests", "dua request ameen pray for me prayed ummah community anonymous", Icons.Default.VolunteerActivism, DeenScreen.UMMAH_DUA_REQUESTS, com.noorpro.app.islamic.IslamicFlags.DUA_REQUESTS),
        MoreItem("Halaqa Rooms", "halaqa live audio room voice quran circle dars naat dhikr talk listen raise hand", Icons.Default.RecordVoiceOver, DeenScreen.UMMAH_HALAQA, com.noorpro.app.islamic.IslamicFlags.HALAQA_ROOMS),
        MoreItem("Ask a Scholar", "ask scholar question fatwa mufti imam answer fiqh ruling verified library", Icons.Default.ContactSupport, DeenScreen.UMMAH_ASK_SCHOLAR, com.noorpro.app.islamic.IslamicFlags.ASK_SCHOLAR),
        MoreItem("Good-deed Challenges", "challenge good deeds friends streak together quran page adhkar salawat sadaqah kindness habit", Icons.Default.EmojiEvents, DeenScreen.UMMAH_CHALLENGES, com.noorpro.app.islamic.IslamicFlags.CHALLENGES),
        MoreItem("Ayah & Hadith Cards", "share card image ayah verse hadith quote story post design wallpaper", Icons.Default.Style, DeenScreen.UMMAH_SHARE_CARDS, com.noorpro.app.islamic.IslamicFlags.SHARE_CARDS),
        MoreItem("Offline Quran", "offline download save quran audio recitation reciter storage sleep timer repeat ayah memorise no internet", Icons.Default.DownloadForOffline, DeenScreen.QURAN_OFFLINE, com.noorpro.app.islamic.IslamicFlags.QURAN_OFFLINE),
        MoreItem("Audio Routines", "routine schedule kahf friday mulk night listen habit reminder quran audio daily", Icons.Default.EventRepeat, DeenScreen.AUDIO_ROUTINES, com.noorpro.app.islamic.IslamicFlags.AUDIO_ROUTINES),
        MoreItem("Family & Kids", "kids children family prayer tracker stories prophets duas manners", Icons.Default.FamilyRestroom, DeenScreen.FAMILY_KIDS, com.noorpro.app.islamic.IslamicFlags.FAMILY_KIDS),
        MoreItem("Group Khatm", "khatm khatam para juz together family invite", Icons.Default.Groups, DeenScreen.GROUP_KHATM, com.noorpro.app.islamic.IslamicFlags.GROUP_KHATM),
        MoreItem("Quran Learning", "qaida tajweed course", Icons.Default.School, DeenScreen.QAIDA_TUTOR),
        MoreItem("Hifz Coach", "hifz memorise memorize revision recite", Icons.Default.Psychology, DeenScreen.HIFZ_COACH, com.noorpro.app.islamic.IslamicFlags.HIFZ_COACH),
        MoreItem("Hadith", "bukhari muslim", Icons.Default.LibraryBooks, DeenScreen.HADITH_LIBRARY),
        MoreItem("Islamic Library", "books", Icons.Default.LocalLibrary, DeenScreen.LIBRARY_DASHBOARD),
        MoreItem("PDF Library", "pdf books", Icons.Default.PictureAsPdf, DeenScreen.PDF_LIBRARY),
        MoreItem("Quiz", "knowledge test", Icons.Default.Quiz, DeenScreen.QUIZ_DASHBOARD),
        MoreItem("Noor AI", "ask ai question", Icons.Default.AutoAwesome, DeenScreen.AI_HUB),
    )),
    MoreCategory("Community", Icons.Default.Groups, listOf(
        MoreItem("Ummah", "community posts", Icons.Default.Public, DeenScreen.UMMAH),
        MoreItem("Reels", "short videos", Icons.Default.SmartDisplay, DeenScreen.REELS),
        MoreItem("Groups", "discover groups", Icons.Default.Diversity3, DeenScreen.DISCOVER_GROUPS),
        MoreItem("Messages", "chat", Icons.Default.Forum, DeenScreen.UMMAH_MESSAGES),
    )),
    MoreCategory("Audio", Icons.Default.Headphones, listOf(
        MoreItem("Al-Noor Audio", "recitation nasheed podcast tafsir tarjuma", Icons.Default.LibraryMusic, DeenScreen.AUDIO_LIBRARY),
        MoreItem("Now Playing", "player reciter", Icons.Default.PlayCircle, DeenScreen.NOW_PLAYING),
    )),
    MoreCategory("Tools", Icons.Default.Construction, listOf(
        MoreItem("Zakat", "calculator", Icons.Default.Calculate, DeenScreen.ZAKAT),
        MoreItem("Hijri Calendar", "islamic dates events", Icons.Default.CalendarMonth, DeenScreen.CALENDAR),
        MoreItem("Islamic Events", "calendar ramadan eid ashura reminders", Icons.Default.EventNote, DeenScreen.ISLAMIC_CALENDAR, com.noorpro.app.islamic.IslamicFlags.CALENDAR),
        MoreItem("Health", "fasting wellness", Icons.Default.HealthAndSafety, DeenScreen.HEALTH_WELLNESS),
        MoreItem("Progress", "spiritual streak", Icons.Default.AutoGraph, DeenScreen.SPIRITUAL_PROGRESS),
        MoreItem("DeenPoints", "points rewards", Icons.Default.Star, DeenScreen.DEEN_POINTS),
        MoreItem("Settings", "preferences", Icons.Default.Settings, DeenScreen.SETTINGS),
    )),
    MoreCategory("Spiritual", Icons.Default.Spa, listOf(
        MoreItem("Darood Sharif", "salawat", Icons.Default.Favorite, DeenScreen.DAROOD_SHARIF),
        MoreItem("Ruhani Ilaj", "ruqyah shifa", Icons.Default.Shield, DeenScreen.RUHANI),
        MoreItem("Tasawwuf", "sufi tariqa awliya", Icons.Default.Spa, DeenScreen.TASAWWUF),
        MoreItem("99 Names", "asma ul husna allah", Icons.Default.AutoAwesomeMosaic, DeenScreen.ASMA_UL_HUSNA),
    )),
)

private val moreSeen = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

/** Categorized 3-column grid; [query] filters by title/keywords. */
@Composable
fun MoreCategoryGrid(onOpen: (DeenScreen) -> Unit, query: String = "", modifier: Modifier = Modifier) {
    val q = query.trim()
    val cats = moreCatalog.map { c ->
        c to c.items.filter { it.visible }.filter { q.isEmpty() || it.title.contains(q, true) || it.keywords.contains(q, true) || c.title.contains(q, true) }
    }.filter { it.second.isNotEmpty() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        if (cats.isEmpty()) Text("Nothing matches \"$q\".", color = stitchMutedText(), fontSize = 14.sp)
        var n = 0
        cats.forEachIndexed { ci, (cat, items) ->
            Column(Modifier.staggeredEntrance(ci, moreSeen, ci)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(cat.icon, null, tint = StitchGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(cat.title, color = stitchPrimary(), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon)
                }
                Spacer(Modifier.height(12.dp))
                items.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        row.forEach { item -> MoreTile(item, Modifier.weight(1f)) { onOpen(item.screen) } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            n += items.size
        }
    }
}

@Composable
private fun MoreTile(item: MoreItem, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(96.dp)
            .pressScale()
            .clip(RoundedCornerShape(18.dp))
            .background(stitchSurface())
            .border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(stitchSoftSurface()), contentAlignment = Alignment.Center) {
            Icon(item.icon, null, tint = stitchPrimary(), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, color = stitchText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
