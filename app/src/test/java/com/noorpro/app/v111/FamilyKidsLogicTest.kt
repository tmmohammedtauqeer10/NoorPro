package com.noorpro.app.v111

import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.islamic.kids.*
import com.noorpro.app.ui.screens.Reels2Flags
import com.noorpro.app.ui.viewmodel.DeenScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FamilyKidsLogicTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    @After fun reset() { KidsMode.active = false; Reels2Flags.override = null }

    @Test fun pinIsSaltedHashedAndLocksAfterFiveTries() {
        var t = 1_000_000L
        val s = KidsStore(ctx) { t }
        assertFalse(s.hasPin())
        assertFalse(s.setPin("1234")); assertFalse(s.setPin("1111")); assertFalse(s.setPin("12a4")); assertFalse(s.setPin("123"))
        assertTrue(s.setPin("2580"))
        val raw = ctx.getSharedPreferences("family_kids_v1", 0).all.values.joinToString()
        assertFalse("PIN must never be stored in clear", raw.contains("2580"))
        assertTrue(s.checkPin("2580"))
        repeat(4) { assertFalse(s.checkPin("0000")) }
        assertEquals(0L, s.lockedForMs())
        assertFalse(s.checkPin("0000"))
        assertTrue(s.lockedForMs() > 0)
        assertFalse("locked even with the right PIN", s.checkPin("2580"))
        t += KidsPin.LOCK_MS + 1
        assertTrue(s.checkPin("2580"))
        assertNotEquals(KidsPin.hash("2580", "a"), KidsPin.hash("2580", "b"))
    }

    @Test fun kidsModeBlocksSocialOnlyWhileFlagOn() {
        val s = KidsStore(ctx)
        s.setActive(true)
        assertTrue(KidsMode.active)
        Reels2Flags.override = mapOf("family_kids_enabled" to true)
        listOf(DeenScreen.REELS, DeenScreen.REELS_EXPLORE, DeenScreen.UMMAH, DeenScreen.UMMAH_CHAT, DeenScreen.UMMAH_MESSAGES, DeenScreen.CREATOR_STUDIO, DeenScreen.AI_HUB)
            .forEach { assertEquals(it.name, DeenScreen.FAMILY_KIDS, KidsMode.redirect(it)) }
        listOf(DeenScreen.QURAN, DeenScreen.PRAYER_TIMES, DeenScreen.DUA_HUB, DeenScreen.DASHBOARD, DeenScreen.FAMILY_KIDS)
            .forEach { assertEquals(it, KidsMode.redirect(it)) }
        Reels2Flags.override = mapOf("family_kids_enabled" to false)
        assertEquals("flag off never traps the device", DeenScreen.REELS, KidsMode.redirect(DeenScreen.REELS))
        s.setActive(false); Reels2Flags.override = mapOf("family_kids_enabled" to true)
        assertEquals(DeenScreen.REELS, KidsMode.redirect(DeenScreen.REELS))
    }

    @Test fun starsOncePerActivityPerDay() {
        val s = KidsStore(ctx)
        assertTrue(s.earn("s112", 100)); assertFalse(s.earn("s112", 100)); assertTrue(s.earn("d_eat", 100))
        assertEquals(2, s.stars()); assertEquals(2, s.todayCount(100)); assertTrue(s.earnedToday("s112", 100))
        assertTrue(s.earn("s112", 101)); assertEquals(3, s.stars()); assertEquals(1, s.todayCount(101))
    }

    @Test fun parentGateIsHardForSmallChildren() {
        val r = Random(3)
        repeat(50) { val g = KidsPin.gate(r); assertTrue(g.answer in 47..110); assertEquals(g.a * g.b + g.c, g.answer) }
    }

    @Test fun familyTrackerStoresBitmasksLocally() {
        val f = FamilyStore(ctx)
        val a = f.addMember("  Ayesha  ")!!; val b = f.addMember("Yusuf")!!
        assertNull(f.addMember("   "))
        assertEquals(listOf("Ayesha", "Yusuf"), f.members().map { it.name })
        f.toggle(200, a.id, 0); f.toggle(200, a.id, 4); f.toggle(200, b.id, 2); f.toggle(200, b.id, 2)
        val log = FamilyStore(ctx).log()
        assertEquals(2, FamilyLogic.count(log[200]!![a.id]!!)); assertEquals(0, FamilyLogic.count(log[200]!![b.id]!!))
        assertTrue(FamilyLogic.has(log[200]!![a.id]!!, 4))
        assertEquals(listOf(0, 2), FamilyLogic.week(log, a.id, listOf(199, 200)))
        f.toggle(300, a.id, 1)
        assertNull("old days are trimmed", f.log()[200])
        f.removeMember(b.id); assertEquals(1, f.members().size)
        repeat(20) { f.addMember("M$it") }; assertEquals(FamilyStore.MAX_MEMBERS, f.members().size)
    }

    @Test fun contentIsCompleteAndReferenced() {
        assertEquals(10, KIDS_SURAHS.size); assertTrue(KIDS_SURAHS.all { it.id == 1 || it.id in 103..114 })
        assertEquals(5, PROPHET_STORIES.size)
        PROPHET_STORIES.forEach { assertTrue(it.paragraphs.size >= 4); assertTrue(it.refs.startsWith("Qur'an")); assertTrue(it.lesson.isNotBlank()) }
        assertTrue(KIDS_DUAS.size >= 6); assertTrue(KIDS_DUAS.all { it.source.isNotBlank() && it.arabic.isNotBlank() })
        assertTrue(MANNERS_CARDS.size >= 6); assertTrue(MANNERS_CARDS.all { it.ref.isNotBlank() })
        assertEquals(KIDS_SURAHS.size, KIDS_SURAHS.map { it.id }.toSet().size)
    }
}
