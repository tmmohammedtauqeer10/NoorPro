package com.noorpro.app.notify

import android.app.Notification
import androidx.media3.common.C
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.audio.session.AlNoorSessionRouter
import com.noorpro.app.audio.session.NoorMediaLayout
import com.noorpro.app.data.PrayerLogEntity
import com.noorpro.app.prayer.PrayerNotifications
import com.noorpro.app.prayer.channels.PrayerNotificationChannels
import com.noorpro.app.receiver.NotificationActionReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoorNotifyTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun phaseMapping() {
        assertEquals(NoorNotify.Phase.DAWN, NoorNotify.phaseFor("Fajr"))
        assertEquals(NoorNotify.Phase.NOON, NoorNotify.phaseFor("Dhuhr"))
        assertEquals(NoorNotify.Phase.NOON, NoorNotify.phaseFor("Jumuah"))
        assertEquals(NoorNotify.Phase.AFTERNOON, NoorNotify.phaseFor("Asr"))
        assertEquals(NoorNotify.Phase.SUNSET, NoorNotify.phaseFor("Maghrib"))
        assertEquals(NoorNotify.Phase.SUNSET, NoorNotify.phaseFor("Iftar"))
        assertEquals(NoorNotify.Phase.NIGHT, NoorNotify.phaseFor("Isha"))
        assertEquals(NoorNotify.Phase.NIGHT, NoorNotify.phaseFor("Suhoor"))
        assertEquals(NoorNotify.Phase.NOON, NoorNotify.phaseFor(null))
    }

    @Test fun artResourcesDecode() {
        NoorNotify.Phase.values().forEach { ph ->
            assertNotNull(NoorNotify.bitmap(ctx, NoorNotify.tileRes(ph)))
            assertNotNull(NoorNotify.bitmap(ctx, NoorNotify.bannerRes(ph, true)))
            assertNotNull(NoorNotify.bitmap(ctx, NoorNotify.bannerRes(ph, false)))
        }
        assertNotNull(NoorNotify.bitmap(ctx, R.drawable.noor_cover_quran))
        assertNotNull(NoorNotify.bitmap(ctx, R.drawable.noor_cover_audio))
    }

    @Test fun initialsAndAvatar() {
        assertEquals("AR", NoorNotify.initials("Abdul Rahman"))
        assertEquals("F", NoorNotify.initials("fatima"))
        assertEquals("N", NoorNotify.initials("  "))
        assertEquals(192, NoorNotify.avatar("Abdul Rahman").width)
    }

    @Test fun adhanNotificationIsModern() {
        PrayerNotificationChannels.ensureAll(ctx)
        val c = PrayerNotifications.Content("Maghrib \u2014 6:29 PM", "Thu, 1 Oct 2026", "Bengaluru, IN")
        val n = PrayerNotifications.adhanBuilder(ctx, PrayerNotificationChannels.REMINDER, "Maghrib", c, 0L, false, true, 7001).build()
        assertEquals(R.drawable.ic_stat_noorpro, n.smallIcon.resId)
        assertEquals("Maghrib \u2014 6:29 PM", n.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(n.extras.getCharSequence(Notification.EXTRA_TEXT).toString().startsWith("It's time for Maghrib"))
        assertEquals(listOf("Stop adhan", "Prayed \u2713", "Qibla"), n.actions.map { it.title.toString() })
        assertNotNull(n.extras.get(Notification.EXTRA_PICTURE) ?: n.extras.get("android.pictureIcon"))
    }

    @Test fun silentNonFardHasOnlyQibla() {
        val c = PrayerNotifications.Content("Jumuah \u2014 1:15 PM", "Fri", "")
        val n = PrayerNotifications.adhanBuilder(ctx, PrayerNotificationChannels.REMINDER, "Jumuah", c, 0L, false, false, 7002).build()
        assertEquals(listOf("Qibla"), n.actions.map { it.title.toString() })
    }

    @Test fun markPrayedTicksOnlyThatPrayer() {
        val log = PrayerLogEntity("2026-10-24")
        assertEquals(log.copy(asr = true), NotificationActionReceiver.withPrayed(log, "Asr"))
        assertEquals(log, NotificationActionReceiver.withPrayed(log, "Jumuah"))
        assertEquals("2026-10-24", NotificationActionReceiver.today(java.util.GregorianCalendar(2026, 9, 24, 12, 0).timeInMillis))
    }

    @Test fun mediaLayoutPerSource() {
        val alNoor = NoorMediaLayout.layoutFor(AlNoorSessionRouter.Source.AL_NOOR, false)
        assertEquals(listOf(NoorMediaLayout.SEEK_BACK, NoorMediaLayout.SEEK_FWD), alNoor.map { it.sessionCommand?.customAction })
        val quran = NoorMediaLayout.layoutFor(AlNoorSessionRouter.Source.QURAN, true)
        assertEquals("Unlike", quran.single().displayName.toString())
        assertEquals(42, NoorMediaLayout.surahId("surah:42"))
        assertEquals(null, NoorMediaLayout.surahId("track:1"))
        assertEquals(0L, NoorMediaLayout.stepped(4_000, 60_000, -10_000))
        assertEquals(60_000L, NoorMediaLayout.stepped(55_000, 60_000, 10_000))
        assertEquals(25_000L, NoorMediaLayout.stepped(15_000, C.TIME_UNSET, 10_000))
        assertFalse(NoorNotify.isFardPrayer("Jumuah"))
    }
}
