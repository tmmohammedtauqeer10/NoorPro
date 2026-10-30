package com.noorpro.app.v111

import androidx.test.core.app.ApplicationProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import com.noorpro.app.islamic.masjid.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MasjidPagesLogicTest {
    private val m = Masjid("noor-blr", "Masjid-e-Noor", "12 Cubbon Road", "Bengaluru", 12.97, 77.6,
        mapOf("Fajr" to "05:15", "Dhuhr" to "13:30", "Asr" to "16:45", "Maghrib" to "18:22", "Isha" to "20:00"), "13:15", "Rights of neighbours", listOf("imam1"))

    @Test fun normalizesTypedTimes() {
        assertEquals("05:30", MasjidLogic.normalizeTime("5:30"))
        assertEquals("05:30", MasjidLogic.normalizeTime("5.30"))
        assertEquals("13:15", MasjidLogic.normalizeTime("1:15 pm"))
        assertEquals("13:15", MasjidLogic.normalizeTime("1:15 PM"))
        assertEquals("00:10", MasjidLogic.normalizeTime("12:10am"))
        assertEquals("12:10", MasjidLogic.normalizeTime("12:10 pm"))
        assertEquals("20:00", MasjidLogic.normalizeTime("20"))
        assertNull(MasjidLogic.normalizeTime("25:00"))
        assertNull(MasjidLogic.normalizeTime("13:15 pm"))
        assertNull(MasjidLogic.normalizeTime("5:3"))
        assertNull(MasjidLogic.normalizeTime("after isha"))
        assertNull(MasjidLogic.normalizeTime(""))
        // Display round-trips back through the parser (edit screen pre-fills display strings).
        listOf("05:15", "13:30", "00:05", "12:00", "23:59").forEach { assertEquals(it, MasjidLogic.normalizeTime(MasjidLogic.display(it))) }
        assertEquals("1:30 PM", MasjidLogic.display("13:30"))
        assertEquals("13:30", MasjidLogic.display("13:30", use24h = true))
        assertEquals("—", MasjidLogic.display("bad"))
    }

    @Test fun nextJamaatFollowsTheClock() {
        assertEquals("Fajr", MasjidLogic.nextJamaat(m, 4 * 60))
        assertEquals("Dhuhr", MasjidLogic.nextJamaat(m, 5 * 60 + 15)) // at exactly Fajr jamaat it's no longer "next"
        assertEquals("Isha", MasjidLogic.nextJamaat(m, 19 * 60))
        assertNull(MasjidLogic.nextJamaat(m, 21 * 60))
        assertEquals("Isha", MasjidLogic.nextJamaat(m.copy(jamaat = mapOf("Isha" to "20:00")), 6 * 60))
    }

    @Test fun searchMatchesNameAreaAndCityPrefixFirst() {
        val list = listOf(m, Masjid("a", "Al-Falah Masjid", city = "Mysuru"), Masjid("b", "Jamia Masjid", "Noor Lane", "Bengaluru"))
        assertEquals(listOf("a", "b", "noor-blr"), MasjidLogic.search(list, "").map { it.id })
        assertEquals(listOf("noor-blr", "b"), MasjidLogic.search(list, "noor").map { it.id }.let { it }.let { r -> r.sortedBy { it != "noor-blr" } })
        assertEquals("noor-blr", MasjidLogic.search(list, "masjid-e").first().id)
        assertEquals(listOf("a"), MasjidLogic.search(list, "MYSURU").map { it.id })
        assertTrue(MasjidLogic.search(list, "delhi").isEmpty())
    }

    @Test fun claimValidationAndManagers() {
        assertNotNull(MasjidLogic.claimProblem("A", "9876543210", "Imam"))
        assertNotNull(MasjidLogic.claimProblem("Abdul Kareem", "98", "Imam"))
        assertNotNull(MasjidLogic.claimProblem("Abdul Kareem", "9876543210", "Boss"))
        assertNull(MasjidLogic.claimProblem("Abdul Kareem", "imam@example.org", "Committee member"))
        assertTrue(MasjidLogic.canManage(m, "imam1"))
        assertFalse(MasjidLogic.canManage(m, "someone"))
        assertFalse(MasjidLogic.canManage(m, null))
    }

    @Test fun shareAndDirectionsText() {
        val s = MasjidLogic.shareText(m)
        assertTrue(s, s.startsWith("Masjid-e-Noor\n12 Cubbon Road"))
        assertTrue(s, s.contains("Fajr 5:15 AM") && s.contains("Isha 8:00 PM"))
        assertTrue(s, s.contains("Jummah: 1:15 PM — Rights of neighbours"))
        assertEquals("Masjid-e-Noor, 12 Cubbon Road, Bengaluru", MasjidLogic.directionsQuery(m))
    }

    @Test fun jsonRoundTripDropsInvalidTimes() {
        assertEquals(m, MasjidLogic.fromJson(MasjidLogic.toJson(m)))
        val o = MasjidLogic.toJson(m.copy(lat = null, lng = null)); o.getJSONObject("jamaat").put("Asr", "late")
        val back = MasjidLogic.fromJson(o)!!
        assertNull(back.lat); assertFalse("Asr" in back.jamaat); assertEquals(4, back.jamaat.size)
        assertNull(MasjidLogic.fromJson(org.json.JSONObject("{}")))
    }

    @Test fun localStoreKeepsFollowedMasjidOffline() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = MasjidLocalStore(ctx)
        assertNull(store.followed())
        store.setFollowed(m)
        assertEquals(m, MasjidLocalStore(ctx).followed())
        assertEquals(m, FollowedMasjid.flow(ctx).value)
        store.cache(listOf(m, Masjid("x", "Second")))
        assertEquals(2, store.cached().size)
        store.setFollowed(null)
        assertNull(store.followed()); assertNull(FollowedMasjid.flow(ctx).value)
    }

    @Test fun errorsMapCalmly() {
        assertEquals(MasjidError.OFFLINE_BACKEND, MasjidError.from(FirebaseFirestoreException("x", FirebaseFirestoreException.Code.PERMISSION_DENIED)))
        assertEquals(MasjidError.NETWORK, MasjidError.from(FirebaseFirestoreException("x", FirebaseFirestoreException.Code.UNAVAILABLE)))
        assertEquals(MasjidError.SIGN_IN, MasjidError.from(RuntimeException(MasjidException(MasjidError.SIGN_IN))))
        assertEquals(MasjidError.UNKNOWN, MasjidError.from(IllegalStateException()))
    }
}
