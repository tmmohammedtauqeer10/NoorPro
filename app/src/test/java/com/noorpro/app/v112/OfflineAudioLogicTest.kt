package com.noorpro.app.v112

import com.noorpro.app.data.IslamicData
import com.noorpro.app.data.Reciter
import com.noorpro.app.islamic.audio.*
import com.noorpro.app.prayer.AdhanSound
import com.noorpro.app.prayer.PrayerPrefs
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class OfflineAudioLogicTest {
    private val husary = IslamicData.reciters.first { it.id == 7 }
    private val mujawwad = IslamicData.reciters.first { it.id == 6 }

    @Test fun everyBundledReciterHasALicenseDecision() {
        IslamicData.reciters.forEach {
            assertEquals(OfflinePolicy.PERSONAL_CACHE, OfflineQuranLogic.license(it).policy)
            assertTrue(OfflineQuranLogic.canDownload(it))
        }
        val unknown = Reciter(99, "X", "", "https://example.org/audio/")
        assertEquals(OfflinePolicy.STREAM_ONLY, OfflineQuranLogic.license(unknown).policy)
        assertFalse(OfflineQuranLogic.canDownload(unknown))
    }

    @Test fun packsAndCounts() {
        assertEquals(6236, OfflineQuranLogic.ayahs((1..114).toList()))
        val juz30 = OfflineQuranLogic.PACKS.first { it.id == "juz30" }
        assertEquals(37, juz30.surahs.size)
        assertEquals(564, OfflineQuranLogic.ayahs(juz30.surahs)) // matches IslamicData.juzAyahsCount[30]
        assertEquals(listOf(79, 80), OfflineQuranLogic.missing(listOf(78, 79, 80), setOf(78)))
    }

    @Test fun sizeEstimates() {
        assertEquals(128, OfflineQuranLogic.kbps(husary))
        assertEquals(192, OfflineQuranLogic.kbps(IslamicData.reciters.first { it.id == 2 }))
        val all = OfflineQuranLogic.estimateBytes(husary, (1..114).toList())
        assertTrue("whole Quran ≈ 0.6–1.2 GB, was $all", all in 600_000_000L..1_200_000_000L)
        assertTrue(OfflineQuranLogic.estimateBytes(mujawwad, listOf(1)) > OfflineQuranLogic.estimateBytes(husary, listOf(1)))
        assertEquals("0 MB", OfflineQuranLogic.formatBytes(0))
        assertEquals("450 KB", OfflineQuranLogic.formatBytes(450_000))
        assertEquals("87 MB", OfflineQuranLogic.formatBytes(87_000_000))
        assertEquals("1.2 GB", OfflineQuranLogic.formatBytes(1_230_000_000))
        assertTrue(OfflineQuranLogic.enoughSpace(100_000_000, 1_000_000_000))
        assertFalse(OfflineQuranLogic.enoughSpace(900_000_000, 1_000_000_000))
    }

    @Test fun classifiesSavedFiles() {
        val names = (1..7).map { "001%03d.mp3".format(it) } + listOf("112001.mp3", "112002.mp3", "002001.mp3.part", "readme.txt")
        val counts = OfflineQuranLogic.countByFile(names)
        assertEquals(mapOf(1 to 7, 112 to 2), counts)
        val (complete, partial) = OfflineQuranLogic.classify(counts)
        assertEquals(setOf(1), complete)
        assertEquals(setOf(112), partial)
    }

    @Test fun shortNames() {
        assertEquals("Al-Husary", OfflineQuranLogic.shortName(husary))
        assertEquals("Abdul Basit · Mujawwad", OfflineQuranLogic.shortName(mujawwad))
    }

    @Test fun listeningTools() {
        assertTrue(ListeningLogic.shouldRepeat(3, 1)); assertTrue(ListeningLogic.shouldRepeat(3, 2)); assertFalse(ListeningLogic.shouldRepeat(3, 3))
        assertTrue(ListeningLogic.shouldRepeat(0, 500))
        assertFalse(ListeningLogic.shouldRepeat(1, 1))
        assertEquals(listOf("Off", "3×", "5×", "10×", "∞"), ListeningLogic.REPEAT_CHOICES.map(ListeningLogic::repeatLabel))
        assertEquals("End of surah", ListeningLogic.sleepLabel(-1))
        assertEquals("14:05", ListeningLogic.remaining(1_000_000L + 845_000, 1_000_000L))
        assertEquals("0:00", ListeningLogic.remaining(5, 10))
        assertNull(ListeningLogic.remaining(null, 10))
    }

    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    @Test fun routineNextFire() {
        val kahf = RoutineLogic.PRESETS.first { it.id == "kahf_friday" }
        // Wed 28 Oct 2026 15:00 → Fri 30 Oct 09:00
        assertEquals(at(2026, 10, 30, 9, 0), RoutineLogic.nextFire(kahf, at(2026, 10, 28, 15, 0), zone))
        // Friday 09:00 exactly → next Friday
        assertEquals(at(2026, 11, 6, 9, 0), RoutineLogic.nextFire(kahf, at(2026, 10, 30, 9, 0), zone))
        val mulk = RoutineLogic.PRESETS.first { it.id == "mulk_night" }
        assertEquals(at(2026, 10, 28, 22, 0), RoutineLogic.nextFire(mulk, at(2026, 10, 28, 15, 0), zone))
        assertEquals(at(2026, 10, 29, 22, 0), RoutineLogic.nextFire(mulk, at(2026, 10, 28, 22, 30), zone))
        assertNull(RoutineLogic.nextFire(mulk.copy(enabled = false), 0, zone))
        assertNull(RoutineLogic.nextFire(mulk.copy(days = emptySet()), 0, zone))
        val next = RoutineLogic.next(listOf(kahf, mulk), at(2026, 10, 28, 15, 0), zone)
        assertEquals("mulk_night", next?.first?.id)
        assertEquals("Today 10:00 PM", RoutineLogic.whenLabel(next!!.second, at(2026, 10, 28, 15, 0), zone))
        assertEquals("Fri 9:00 AM", RoutineLogic.whenLabel(at(2026, 10, 30, 9, 0), at(2026, 10, 28, 15, 0), zone))
        assertEquals("Tomorrow 9:00 AM", RoutineLogic.whenLabel(at(2026, 10, 29, 9, 0), at(2026, 10, 28, 15, 0), zone))
    }

    @Test fun routineLabelsSearchValidation() {
        assertEquals("Every day", RoutineLogic.daysLabel(RoutineLogic.EVERY_DAY))
        assertEquals("Weekdays", RoutineLogic.daysLabel(setOf(1, 2, 3, 4, 5)))
        assertEquals("Mon, Fri", RoutineLogic.daysLabel(setOf(5, 1)))
        assertEquals("12:05 AM", RoutineLogic.timeLabel(5))
        assertEquals("12:30 PM", RoutineLogic.timeLabel(12 * 60 + 30))
        assertEquals("10:15 PM", RoutineLogic.timeLabel(22 * 60 + 15))
        assertEquals(listOf(18), RoutineLogic.searchSurah("18"))
        assertEquals(emptyList<Int>(), RoutineLogic.searchSurah("115"))
        assertTrue(18 in RoutineLogic.searchSurah("kahf"))
        assertTrue(36 in RoutineLogic.searchSurah("Yasin"))
        assertTrue(56 in RoutineLogic.searchSurah("waqiah"))
        assertTrue(55 in RoutineLogic.searchSurah("Rahman"))
        assertNotNull(RoutineLogic.validate("", listOf(18), setOf(5)))
        assertNotNull(RoutineLogic.validate("x", emptyList(), setOf(5)))
        assertNotNull(RoutineLogic.validate("x", listOf(18), emptySet()))
        assertNotNull(RoutineLogic.validate("x", (1..11).toList(), setOf(5)))
        assertNull(RoutineLogic.validate("Kahf", listOf(18), setOf(5)))
    }

    @Test fun adhanOptionsMatchExistingSounds() {
        assertEquals(AdhanSound.entries.toSet(), AdhanVoiceLogic.OPTIONS.map { it.sound }.toSet())
        assertEquals(PrayerPrefs.SOUND_OPTIONS.toSet(), AdhanVoiceLogic.OPTIONS.map { it.pref }.toSet())
        assertEquals(AdhanSound.ALARM, AdhanVoiceLogic.current("Mecca Adhan").sound) // legacy names still map
        assertEquals(AdhanSound.SILENT, AdhanVoiceLogic.current("Silent").sound)
        assertNull(AdhanVoiceLogic.previewUri(AdhanSound.SILENT))
        AdhanVoiceLogic.OPTIONS.forEach { assertEquals(com.noorpro.app.prayer.channels.PrayerNotificationChannels.adhanChannelFor(it.sound), it.channel) }
    }
}
