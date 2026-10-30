package com.noorpro.app.islamic.calendar

import com.noorpro.app.data.LocalHijri
import java.util.Calendar
import java.util.TimeZone

/*
 * 1.1.6 Islamic events calendar: pure logic. Builds on [LocalHijri] (offline Umm al-Qura, the same
 * conversion the existing Hijri screen and Home date use) and the user's existing Hijri adjustment
 * (-2..+2 days, DeenViewModel "hijri_adjustment").
 */

data class HijriDate(val year: Int, val month: Int, val day: Int) : Comparable<HijriDate> {
    override fun compareTo(other: HijriDate) = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
    val monthName: String get() = HIJRI_MONTHS.getOrElse(month - 1) { "" }
    fun label(): String = "$day $monthName $year AH"
}

/** Plain (no diacritics) month names used by the new screens. */
val HIJRI_MONTHS = listOf(
    "Muharram", "Safar", "Rabi ul Awwal", "Rabi ul Akhir", "Jumada al-Ula", "Jumada al-Akhirah",
    "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhul Qa'dah", "Dhul Hijjah",
)
val HIJRI_MONTHS_AR = LocalHijri.monthsAr

/** Gregorian day as days since 1970-01-01 (UTC midnight). Keeps the logic free of time zones. */
fun epochDayOf(year: Int, month: Int, day: Int): Long {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(year, month - 1, day) }
    return Math.floorDiv(c.timeInMillis, 86_400_000L)
}

fun utcCalendar(epochDay: Long): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); timeInMillis = epochDay * 86_400_000L }

/** Gregorian -> Hijri, swappable for tests. */
fun interface HijriConverter {
    fun toHijri(epochDay: Long): HijriDate

    companion object {
        /** Offline Umm al-Qura via android.icu (same as LocalHijri). Noon avoids DST/zone edges. */
        val UmmAlQura = HijriConverter { ed ->
            val c = utcCalendar(ed).apply { set(Calendar.HOUR_OF_DAY, 12) }
            val local = Calendar.getInstance().apply { clear(); set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 12, 0) }
            val h = LocalHijri.fromGregorian(local)
            HijriDate(h.year, h.month, h.day)
        }
    }
}

/** Applies the user's sighting adjustment (+1 = the local Hijri date is one day ahead of Umm al-Qura). */
class AdjustedHijri(private val base: HijriConverter, adjustment: Int) {
    val adjustment = adjustment.coerceIn(-2, 2)
    fun hijri(epochDay: Long): HijriDate = base.toHijri(epochDay + adjustment)
}

enum class EventKind { DAY, NIGHT, SEASON }

/**
 * A key date. [nightBefore]: the night falls on the evening before the Gregorian day of [day]
 * (Islamic days begin at Maghrib). [acts] are short, neutral suggestions written for NoorPro.
 */
data class IslamicEvent(
    val id: String,
    val name: String,
    val arabic: String,
    val month: Int,
    val day: Int,
    val kind: EventKind,
    val summary: String,
    val acts: List<String>,
    val nightBefore: Boolean = false,
)

object IslamicEventCatalog {
    private const val VARY = "Observances differ between schools and regions; follow the guidance of your local scholars."

    val events: List<IslamicEvent> = listOf(
        IslamicEvent("new_year", "Islamic New Year", "رأس السنة الهجرية", 1, 1, EventKind.DAY,
            "The first day of Muharram opens the Hijri year, counted from the Prophet's ﷺ migration to Madinah.",
            listOf("Reflect on the year that has passed and renew your intentions", "Make sincere tawbah and ask Allah for a blessed year",
                "Begin Muharram, one of the four sacred months, with extra good deeds")),
        IslamicEvent("ashura", "Day of Ashura", "يوم عاشوراء", 1, 10, EventKind.DAY,
            "The 10th of Muharram. Musa (AS) and his people were saved on this day, and many Muslims also remember the martyrdom of Imam Husayn (RA) at Karbala.",
            listOf("Fast on the 10th, and many also fast the 9th or the 11th with it", "Give in charity and be generous to your family",
                "Make dua and remember those who stood firm for the truth", VARY)),
        IslamicEvent("milad", "12 Rabi ul Awwal · Milad un Nabi ﷺ", "المولد النبوي", 3, 12, EventKind.DAY,
            "The month in which the Prophet Muhammad ﷺ was born. Many Muslims mark the 12th with remembrance of his life.",
            listOf("Send abundant salawat (durood) upon the Prophet ﷺ", "Read or listen to a chapter of the Seerah with your family",
                "Revive one of his sunnahs in your daily life", VARY)),
        IslamicEvent("meraj", "Shab-e-Meraj", "ليلة الإسراء والمعراج", 7, 27, EventKind.NIGHT,
            "The night of the Isra and Mi'raj, the Night Journey and Ascension, when the five daily prayers were gifted to this Ummah.",
            listOf("Remember the gift of salah and guard all five prayers", "Read Surah Al-Isra and reflect on its opening verses",
                "Spend some of the night in dua and dhikr", VARY), nightBefore = true),
        IslamicEvent("barat", "Shab-e-Barat", "ليلة النصف من شعبان", 8, 15, EventKind.NIGHT,
            "The night of the 15th of Sha'ban, a night of seeking forgiveness as Ramadan draws near.",
            listOf("Seek forgiveness and make dua for yourself, family and the deceased", "Forgive others and mend broken ties",
                "Many spend part of the night in worship and fast the following day", VARY), nightBefore = true),
        IslamicEvent("ramadan", "Ramadan begins", "رمضان", 9, 1, EventKind.SEASON,
            "The month of fasting in which the Qur'an was revealed. Dates depend on the sighting of the moon.",
            listOf("Fast with sincerity and guard your tongue", "Set a daily Qur'an reading goal", "Pray Taraweeh and give in charity",
                "Confirm the start date with your local moon-sighting committee")),
        IslamicEvent("qadr21", "Laylatul Qadr · 21st night", "ليلة القدر", 9, 21, EventKind.NIGHT, QADR_SUMMARY, QADR_ACTS, nightBefore = true),
        IslamicEvent("qadr23", "Laylatul Qadr · 23rd night", "ليلة القدر", 9, 23, EventKind.NIGHT, QADR_SUMMARY, QADR_ACTS, nightBefore = true),
        IslamicEvent("qadr25", "Laylatul Qadr · 25th night", "ليلة القدر", 9, 25, EventKind.NIGHT, QADR_SUMMARY, QADR_ACTS, nightBefore = true),
        IslamicEvent("qadr27", "Laylatul Qadr · 27th night", "ليلة القدر", 9, 27, EventKind.NIGHT, QADR_SUMMARY, QADR_ACTS, nightBefore = true),
        IslamicEvent("qadr29", "Laylatul Qadr · 29th night", "ليلة القدر", 9, 29, EventKind.NIGHT, QADR_SUMMARY, QADR_ACTS, nightBefore = true),
        IslamicEvent("eid_fitr", "Eid al-Fitr", "عيد الفطر", 10, 1, EventKind.DAY,
            "The celebration at the end of Ramadan, a day of thanks, prayer and family.",
            listOf("Give Zakat al-Fitr before the Eid prayer", "Eat something sweet, such as dates, before leaving", "Pray the Eid salah and say the takbirat",
                "Visit family and make the day joyful for others")),
        IslamicEvent("arafah", "Day of Arafah", "يوم عرفة", 12, 9, EventKind.DAY,
            "The greatest day of Hajj, when pilgrims stand at Arafah. For those not on Hajj it is a day of fasting and dua.",
            listOf("Fast the day if you are not performing Hajj", "Make abundant dua from after Dhuhr until Maghrib",
                "Begin the takbirat of the days of Tashriq after Fajr")),
        IslamicEvent("eid_adha", "Eid al-Adha", "عيد الأضحى", 12, 10, EventKind.DAY,
            "The festival of sacrifice, remembering the devotion of Ibrahim (AS) and Isma'il (AS).",
            listOf("Pray the Eid salah and say the takbirat", "Offer the qurbani if you are able", "Share the meat with family, neighbours and those in need")),
    )

    fun byId(id: String): IslamicEvent? = events.firstOrNull { it.id == id }
    fun on(month: Int, day: Int): List<IslamicEvent> = events.filter { it.month == month && it.day == day }
}

private const val QADR_SUMMARY = "The Night of Decree is better than a thousand months (Al-Qadr 97:3). It is sought in the odd nights of the last ten of Ramadan."
private val QADR_ACTS = listOf(
    "Pray at night, even a few rak'ahs, and recite the Qur'an",
    "Repeat the dua: Allahumma innaka 'afuwwun tuhibbul-'afwa fa'fu 'anni",
    "Give charity, however small, on each of the odd nights",
    "Make long dua for yourself, your family and the Ummah",
)

/** One upcoming occurrence on the Gregorian calendar. */
data class EventOccurrence(val event: IslamicEvent, val epochDay: Long, val hijri: HijriDate) {
    /** Day the reminder should target: the evening before for night events. */
    val observeEpochDay: Long get() = if (event.nightBefore) epochDay - 1 else epochDay
}

object IslamicCalendarLogic {
    /** Upcoming events from [fromEpochDay] (inclusive) over the next [days] days, in date order. */
    fun upcoming(cal: AdjustedHijri, fromEpochDay: Long, days: Int = 400): List<EventOccurrence> =
        (0 until days).flatMap { i ->
            val ed = fromEpochDay + i
            val h = cal.hijri(ed)
            IslamicEventCatalog.on(h.month, h.day).map { EventOccurrence(it, ed, h) }
        }

    fun nextOf(cal: AdjustedHijri, id: String, fromEpochDay: Long): EventOccurrence? =
        upcoming(cal, fromEpochDay).firstOrNull { it.event.id == id }

    /** Gregorian epoch day of the 1st of the Hijri month containing [epochDay]. */
    fun monthStart(cal: AdjustedHijri, epochDay: Long): Long = epochDay - (cal.hijri(epochDay).day - 1)

    /** Moves [months] whole Hijri months from the month containing [epochDay]; returns the new month start. */
    fun shiftMonth(cal: AdjustedHijri, epochDay: Long, months: Int): Long {
        var start = monthStart(cal, epochDay)
        repeat(kotlin.math.abs(months)) {
            start = if (months > 0) monthStart(cal, start + 31) else monthStart(cal, start - 1)
        }
        return start
    }

    fun monthLength(cal: AdjustedHijri, monthStartEpochDay: Long): Int =
        if (cal.hijri(monthStartEpochDay + 29).day == 30) 30 else 29

    /** Sunday-first month grid; null cells pad the first week. */
    fun monthGrid(cal: AdjustedHijri, anyDayInMonth: Long): HijriMonth {
        val start = monthStart(cal, anyDayInMonth)
        val len = monthLength(cal, start)
        val h = cal.hijri(start)
        val weekday = utcCalendar(start).get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val days = (0 until len).map { i ->
            val ed = start + i
            val g = utcCalendar(ed)
            HijriCell(i + 1, ed, g.get(Calendar.DAY_OF_MONTH), g.get(Calendar.MONTH) + 1, IslamicEventCatalog.on(h.month, i + 1))
        }
        return HijriMonth(h.year, h.month, start, List(weekday) { null } + days)
    }

    fun daysUntil(fromEpochDay: Long, toEpochDay: Long): Int = (toEpochDay - fromEpochDay).toInt()

    fun countdown(days: Int): String = when {
        days <= 0 -> "Today"
        days == 1 -> "Tomorrow"
        days < 14 -> "In $days days"
        days < 60 -> "In ${days / 7} weeks"
        else -> "In ${days / 30} months"
    }

    private val G_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val WEEKDAYS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    fun gregorianLabel(epochDay: Long): String {
        val c = utcCalendar(epochDay)
        return "${WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1]}, ${c.get(Calendar.DAY_OF_MONTH)} ${G_MONTHS[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR)}"
    }
    fun gregorianShort(epochDay: Long): String { val c = utcCalendar(epochDay); return "${c.get(Calendar.DAY_OF_MONTH)} ${G_MONTHS[c.get(Calendar.MONTH)]}" }

    fun todayEpochDay(nowMs: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(nowMs + tz.getOffset(nowMs), 86_400_000L)

    fun adjustmentLabel(adj: Int): String = when {
        adj == 0 -> "Umm al-Qura (no adjustment)"
        adj > 0 -> "+$adj day${if (adj == 1) "" else "s"}"
        else -> "$adj day${if (adj == -1) "" else "s"}"
    }
}

data class HijriCell(val hijriDay: Int, val epochDay: Long, val gDay: Int, val gMonth: Int, val events: List<IslamicEvent>)
data class HijriMonth(val year: Int, val month: Int, val startEpochDay: Long, val cells: List<HijriCell?>) {
    val title: String get() = "${HIJRI_MONTHS[month - 1]} $year"
}
