package com.noorpro.app.data

import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import java.util.Calendar
import java.util.Locale
import com.noorpro.app.ui.viewmodel.DeenViewModel.HijriDay

/** Offline Hijri (Umm al-Qura) conversion; no network required. */
object LocalHijri {
    val monthsEn = listOf(
        "Muḥarram", "Ṣafar", "Rabīʿ al-awwal", "Rabīʿ al-thānī", "Jumādá al-ūlá", "Jumādá al-ākhirah",
        "Rajab", "Shaʿbān", "Ramaḍān", "Shawwāl", "Dhū al-Qaʿdah", "Dhū al-Ḥijjah"
    )
    val monthsAr = listOf(
        "مُحَرَّم", "صَفَر", "رَبيع الأوَّل", "رَبيع الثاني", "جُمادى الأولى", "جُمادى الآخرة",
        "رَجَب", "شَعْبان", "رَمَضان", "شَوّال", "ذوالقعدة", "ذوالحجة"
    )
    private val weekdaysEn = listOf("Al Ahad", "Al Athnayn", "Al Thalaata", "Al Arbiaa", "Al Khamees", "Al Juma'a", "Al Sabt")
    private val weekdaysAr = listOf("الاحد", "الاثنين", "الثلاثاء", "الاربعاء", "الخميس", "الجمعة", "السبت")

    data class H(val day: Int, val month: Int, val year: Int, val weekday: Int)

    fun fromGregorian(cal: Calendar, adjustmentDays: Int = 0): H {
        val g = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, adjustmentDays) }
        val ic = IslamicCalendar(ULocale("@calendar=islamic-umalqura"))
        ic.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        ic.timeInMillis = g.timeInMillis
        return H(
            ic.get(IslamicCalendar.DAY_OF_MONTH),
            ic.get(IslamicCalendar.MONTH) + 1,
            ic.get(IslamicCalendar.EXTENDED_YEAR),
            cal.get(Calendar.DAY_OF_WEEK) - 1
        )
    }

    fun holidays(day: Int, month: Int): List<String> = when (month to day) {
        1 to 1 -> listOf("Islamic New Year")
        1 to 10 -> listOf("Ashura")
        3 to 12 -> listOf("Mawlid al-Nabi")
        7 to 27 -> listOf("Lailat-ul-Miraj")
        8 to 15 -> listOf("Lailat-ul-Bara'at")
        9 to 1 -> listOf("1st Day of Ramadan")
        9 to 27 -> listOf("Lailat-ul-Qadr")
        10 to 1 -> listOf("Eid-ul-Fitr")
        12 to 8 -> listOf("Hajj")
        12 to 9 -> listOf("Arafa")
        12 to 10 -> listOf("Eid-ul-Adha")
        else -> emptyList()
    }

    /** Month grid in the same shape the AlAdhan gToHCalendar API returned. */
    fun gregorianMonth(month: Int, year: Int, adjustmentDays: Int = 0): List<HijriDay> {
        val cal = Calendar.getInstance().apply { clear(); set(year, month - 1, 1, 12, 0) }
        val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..days).map { d ->
            cal.set(Calendar.DAY_OF_MONTH, d)
            val h = fromGregorian(cal, adjustmentDays)
            HijriDay(
                gregorianDate = String.format(Locale.US, "%02d-%02d-%04d", d, month, year),
                hijriDate = String.format(Locale.US, "%02d-%02d-%04d", h.day, h.month, h.year),
                hijriDay = String.format(Locale.US, "%02d", h.day),
                hijriMonthEn = monthsEn[h.month - 1],
                hijriMonthAr = monthsAr[h.month - 1],
                hijriYear = h.year.toString(),
                hijriWeekdayEn = weekdaysEn[h.weekday],
                hijriWeekdayAr = weekdaysAr[h.weekday],
                holidays = holidays(h.day, h.month)
            )
        }
    }
}
