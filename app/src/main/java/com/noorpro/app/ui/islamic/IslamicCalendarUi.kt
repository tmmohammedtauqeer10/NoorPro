package com.noorpro.app.ui.islamic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.islamic.calendar.*
import com.noorpro.app.ui.components.QuranArabicFont
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenViewModel

/*
 * 1.1.6 Islamic events calendar (Remote Config islamic_calendar_enabled, default OFF).
 * Fully offline: Umm al-Qura via LocalHijri + the existing Hijri adjustment; local reminders.
 */

data class CalendarUi(
    val todayEpochDay: Long,
    val today: HijriDate,
    val month: HijriMonth,
    val upcoming: List<EventOccurrence>,
    val adjustment: Int,
    val reminders: Set<String>,
)

class CalendarActions(
    val onBack: () -> Unit = {},
    val onMonth: (Int) -> Unit = {},
    val onAdjust: (Int) -> Unit = {},
    val onOpen: (EventOccurrence) -> Unit = {},
    val onReminder: (String, Boolean) -> Unit = { _, _ -> },
)

fun buildCalendarUi(conv: HijriConverter, adjustment: Int, todayEpochDay: Long, monthOffset: Int, reminders: Set<String>): CalendarUi {
    val cal = AdjustedHijri(conv, adjustment)
    val monthStart = IslamicCalendarLogic.shiftMonth(cal, todayEpochDay, monthOffset)
    return CalendarUi(
        todayEpochDay, cal.hijri(todayEpochDay), IslamicCalendarLogic.monthGrid(cal, monthStart),
        IslamicCalendarLogic.upcoming(cal, todayEpochDay, 370), cal.adjustment, reminders,
    )
}

@Composable
fun IslamicCalendarScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val adjustment by viewModel.hijriAdjustment.collectAsState()
    val store = remember { EventReminderStore(ctx) }
    var reminders by remember { mutableStateOf(store.enabled()) }
    var offset by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf<EventOccurrence?>(null) }
    val today = remember { IslamicCalendarLogic.todayEpochDay() }
    val ui = remember(adjustment, offset, reminders) { buildCalendarUi(HijriConverter.UmmAlQura, adjustment, today, offset, reminders) }
    LaunchedEffect(adjustment, reminders) { runCatching { EventReminderScheduler.sync(ctx, adjustment) } }
    val actions = CalendarActions(
        onBack = { viewModel.goBack() },
        onMonth = { offset += it },
        onAdjust = { viewModel.updateHijriAdjustment(it) },
        onOpen = { open = it },
        onReminder = { id, on -> reminders = store.set(id, on) },
    )
    val sel = open
    if (sel != null) {
        BackHandler { open = null }
        IslamicEventDetail(sel, ui.todayEpochDay, sel.event.id in reminders, onBack = { open = null }, onReminder = { actions.onReminder(sel.event.id, it) })
    } else IslamicCalendarContent(ui, actions)
}

@Composable
fun IslamicCalendarContent(ui: CalendarUi, a: CalendarActions, modifier: Modifier = Modifier) {
    LazyColumn(modifier.ikScreen().testTag("islamic_calendar"), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { IkHeader("Islamic Calendar", IslamicCalendarLogic.gregorianLabel(ui.todayEpochDay), a.onBack) }
        item { TodayHero(ui, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        item { MonthCard(ui, a, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item { AdjustCard(ui.adjustment, a.onAdjust, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        item { IkSection("Upcoming events", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        items(ui.upcoming.take(14), key = { it.event.id + it.epochDay }) { o ->
            EventRow(o, ui.todayEpochDay, o.event.id in ui.reminders, { a.onOpen(o) }, { a.onReminder(o.event.id, it) }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        item {
            Text("Dates are calculated (Umm al-Qura) and may differ by a day from local moon sighting. Use the adjustment above to match your community.",
                color = stitchMutedText(), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun TodayHero(ui: CalendarUi, modifier: Modifier) {
    val next = ui.upcoming.firstOrNull { it.epochDay >= ui.todayEpochDay }
    IkHero(modifier.testTag("cal_today")) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("TODAY", color = IkGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(4.dp))
                Text("${ui.today.day} ${ui.today.monthName}", color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp)
                Text("${ui.today.year} AH", color = IkCream.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            Text(HIJRI_MONTHS_AR[ui.today.month - 1], color = IkGold, fontFamily = QuranArabicFont, fontSize = 24.sp, modifier = Modifier.padding(top = 6.dp))
        }
        if (next != null) {
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.14f)))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(IkGold.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(if (next.event.kind == EventKind.NIGHT) Icons.Outlined.DarkMode else Icons.Default.NotificationsActive, null, tint = IkGold, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Next · ${next.event.name}", color = IkCream, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(IslamicCalendarLogic.gregorianLabel(next.epochDay), color = IkCream.copy(alpha = 0.72f), fontSize = 12.sp)
                }
                Text(IslamicCalendarLogic.countdown(IslamicCalendarLogic.daysUntil(ui.todayEpochDay, next.epochDay)), color = IkGreenDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(IkGold).padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun MonthCard(ui: CalendarUi, a: CalendarActions, modifier: Modifier) {
    val m = ui.month
    IkCard(modifier.testTag("cal_month"), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", "cal_prev") { a.onMonth(-1) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(m.title, color = stitchText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val firstG = m.cells.firstNotNullOfOrNull { it }?.epochDay ?: m.startEpochDay
                val lastG = m.cells.lastOrNull()?.epochDay ?: firstG
                Text("${IslamicCalendarLogic.gregorianShort(firstG)} – ${IslamicCalendarLogic.gregorianShort(lastG)}", color = stitchMutedText(), fontSize = 11.sp)
            }
            NavArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", "cal_next") { a.onMonth(1) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { i, d ->
                Text(d, color = if (i == 5) IkGoldDeep else stitchMutedText(), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        m.cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEach { c -> DayCell(c, ui.todayEpochDay, Modifier.weight(1f)) { c?.let { cell -> cell.events.firstOrNull()?.let { e -> a.onOpen(EventOccurrence(e, cell.epochDay, HijriDate(m.year, m.month, cell.hijriDay))) } } } }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(IkGreen)); Text("  Today", color = stitchMutedText(), fontSize = 11.sp)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(IkGold)); Text("  Event", color = stitchMutedText(), fontSize = 11.sp)
        }
    }
}

@Composable
private fun NavArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tag: String, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(ikSoft()).clickable(onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = ikAccentText())
    }
}

@Composable
private fun DayCell(c: HijriCell?, today: Long, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).padding(2.dp), contentAlignment = Alignment.Center) {
        if (c == null) return@Box
        val isToday = c.epochDay == today
        val hasEvent = c.events.isNotEmpty()
        val bg = when { isToday -> IkGreen; hasEvent -> IkGold.copy(alpha = if (isStitchLight()) 0.16f else 0.20f); else -> Color.Transparent }
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(bg).clickable(enabled = hasEvent, onClick = onClick)
                .testTag(if (isToday) "cal_cell_today" else "cal_cell_${c.hijriDay}"),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Text("${c.hijriDay}", color = if (isToday) Color.White else stitchText(), fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = LibreCaslon)
            Text("${c.gDay}", color = if (isToday) Color.White.copy(alpha = 0.8f) else stitchMutedText(), fontSize = 9.sp)
            if (hasEvent) Box(Modifier.padding(top = 1.dp).size(4.dp).clip(CircleShape).background(if (isToday) IkGold else IkGoldDeep))
        }
    }
}

@Composable
private fun AdjustCard(adj: Int, onAdjust: (Int) -> Unit, modifier: Modifier) {
    IkCard(modifier.testTag("cal_adjust"), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkIconTile(Icons.Outlined.Tune, size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Hijri date adjustment", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Match your local moon sighting · ${IslamicCalendarLogic.adjustmentLabel(adj)}", color = stitchMutedText(), fontSize = 11.sp, maxLines = 2)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (-2..2).forEach { v ->
                val label = if (v > 0) "+$v" else "$v"
                Box(
                    Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(12.dp)).background(if (v == adj) IkGreen else ikSoft())
                        .clickable { onAdjust(v) }.testTag("cal_adj_$v"),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (v == adj) Color.White else ikAccentText(), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun EventRow(o: EventOccurrence, today: Long, reminder: Boolean, onOpen: () -> Unit, onReminder: (Boolean) -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen).padding(10.dp).testTag("cal_event_${o.event.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(ikSoft()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("${o.hijri.day}", color = ikAccentText(), fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp)
            Text(o.hijri.monthName.take(7), color = stitchMutedText(), fontSize = 9.sp, maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(o.event.name, color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (o.event.nightBefore) "From Maghrib, " + IslamicCalendarLogic.gregorianLabel(o.observeEpochDay) else IslamicCalendarLogic.gregorianLabel(o.epochDay), color = stitchMutedText(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(IslamicCalendarLogic.countdown(IslamicCalendarLogic.daysUntil(today, o.epochDay)), color = IkGoldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (reminder) IkGold.copy(alpha = 0.18f) else Color.Transparent).clickable { onReminder(!reminder) }
            .testTag("cal_bell_${o.event.id}"), contentAlignment = Alignment.Center) {
            Icon(if (reminder) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone, if (reminder) "Reminder on" else "Remind me",
                tint = if (reminder) IkGoldDeep else stitchMutedText(), modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
fun IslamicEventDetail(o: EventOccurrence, today: Long, reminder: Boolean, onBack: () -> Unit, onReminder: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val e = o.event
    Column(modifier.ikScreen().verticalScroll(rememberScrollState()).testTag("cal_detail")) {
        IkHeader(e.name, o.hijri.label(), onBack)
        IkHero(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(e.arabic, color = IkGold, fontFamily = QuranArabicFont, fontSize = 26.sp, modifier = Modifier.align(Alignment.End))
            Spacer(Modifier.height(4.dp))
            Text(e.name, color = IkCream, fontFamily = LibreCaslon, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(IslamicCalendarLogic.gregorianLabel(o.epochDay) + " · " + o.hijri.label(), color = IkCream.copy(alpha = 0.85f), fontSize = 13.sp)
            if (e.nightBefore) Text("Begins at Maghrib on " + IslamicCalendarLogic.gregorianLabel(o.observeEpochDay), color = IkGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(10.dp))
            Text(IslamicCalendarLogic.countdown(IslamicCalendarLogic.daysUntil(today, o.epochDay)), color = IkGreenDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(IkGold).padding(horizontal = 10.dp, vertical = 4.dp))
        }
        Text(e.summary, color = stitchText(), fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        IkSection("Recommended acts", Modifier.padding(horizontal = 16.dp))
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            e.acts.forEachIndexed { i, act ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(ikSoft()), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = ikAccentText(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(act, color = stitchText(), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f).padding(top = 2.dp))
                }
            }
        }
        IkCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("cal_detail_reminder"), padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IkIconTile(Icons.Default.NotificationsActive, size = 36.dp, tint = IkGoldDeep, bg = IkGold.copy(alpha = 0.16f))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Remind me", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(if (e.nightBefore) "At 5 pm on the evening it begins" else "At 8 pm the evening before", color = stitchMutedText(), fontSize = 12.sp)
                }
                Switch(reminder, onReminder, colors = SwitchDefaults.colors(checkedTrackColor = IkGreen, checkedThumbColor = Color.White))
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
