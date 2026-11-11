package com.noorpro.app.prayer

import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerNotificationsTest {
    private val tz = TimeZone.getTimeZone("Asia/Kolkata")
    private val maghrib = GregorianCalendar(tz).apply { clear(); set(2026, java.util.Calendar.OCTOBER, 1, 18, 29, 0) }.timeInMillis

    @Test fun titleHasPrayerAndTwelveHourTime() {
        val c = PrayerNotifications.content("Maghrib", maghrib, "29 Rabi al-Awwal 1448 AH", "Hagari Bommanahalli, IN", tz)
        assertEquals("Maghrib \u2014 6:29 PM", c.title)
    }

    @Test fun bodyHasGregorianHijriAndLocation() {
        val c = PrayerNotifications.content("Maghrib", maghrib, "29 Rabi al-Awwal 1448 AH", "Hagari Bommanahalli, IN", tz)
        assertEquals("Thu, 1 Oct 2026 \u00B7 29 Rabi al-Awwal 1448 AH", c.dateLine)
        assertTrue(c.bigText.contains("Hagari Bommanahalli, IN"))
    }

    @Test fun missingHijriStillShowsGregorian() {
        val c = PrayerNotifications.content("Fajr", maghrib, "", "", tz)
        assertEquals("Thu, 1 Oct 2026", c.dateLine)
        assertTrue(!c.bigText.contains("\uD83D\uDCCD"))
    }

    @Test fun clockConvertsHhmm() {
        assertEquals("12:05 AM", PrayerDisplay.clock12("00:05"))
        assertEquals("12:20 PM", PrayerDisplay.clock12("12:20"))
        assertEquals("6:29 PM", PrayerDisplay.clock12("18:29"))
    }
}
