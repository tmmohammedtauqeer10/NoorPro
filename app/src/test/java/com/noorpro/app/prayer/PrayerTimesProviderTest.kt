package com.noorpro.app.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerTimesProviderTest {
    private val ok = """{"code":200,"data":{"timings":{"Fajr":"04:51 (IST)","Sunrise":"06:03","Dhuhr":"12:03","Asr":"15:25","Sunset":"18:01","Maghrib":"18:01","Isha":"19:21"}}}"""

    @Test fun parsesAlAdhanTimingsAndStripsTimezoneSuffix() {
        val t = PrayerTimesProvider.parseTimings(ok)!!
        assertEquals("04:51", t["Fajr"])
        assertEquals("18:01", t["Maghrib"])
        assertEquals(6, t.size)
    }

    @Test fun rejectsMalformedPayloads() {
        assertNull(PrayerTimesProvider.parseTimings("""{"data":{"timings":{"Fajr":"soon"}}}"""))
        assertNull(PrayerTimesProvider.parseTimings("not json"))
    }
}
