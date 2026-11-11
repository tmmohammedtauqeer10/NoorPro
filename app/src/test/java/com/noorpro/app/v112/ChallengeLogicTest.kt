package com.noorpro.app.v112

import com.noorpro.app.islamic.social.*
import org.junit.Assert.*
import org.junit.Test

class ChallengeLogicTest {
    private val c = Challenge("ABCD2345", "A page a day", ChallengeKind.QURAN, "Read one page", 21, "2026-10-20", "aisha", "Aisha", listOf("aisha", "bilal"))
    private fun days(vararg d: Int) = d.map { "2026-10-%02d".format(it) }.toSet()

    @Test fun codes() {
        repeat(50) { assertTrue(ChallengeLogic.validCode(ChallengeLogic.newCode())) }
        assertEquals("ABCD2345", ChallengeLogic.normalizeCode("abcd-2345"))
        assertEquals("ABCD", ChallengeLogic.normalizeCode("aBcD o0i1"))
        assertFalse(ChallengeLogic.validCode("ABCD234O"))
        assertFalse(ChallengeLogic.validCode("ABC"))
    }

    @Test fun phases() {
        assertEquals(9, ChallengeLogic.dayNumber(c, "2026-10-28"))
        assertEquals(ChallengePhase.UPCOMING, ChallengeLogic.phase(c, "2026-10-19"))
        assertEquals(ChallengePhase.ACTIVE, ChallengeLogic.phase(c, "2026-10-20"))
        assertEquals(ChallengePhase.ACTIVE, ChallengeLogic.phase(c, "2026-11-09"))
        assertEquals(ChallengePhase.FINISHED, ChallengeLogic.phase(c, "2026-11-10"))
        assertEquals(21, ChallengeLogic.window(c).size)
        assertEquals("2026-11-09", ChallengeLogic.window(c).last())
    }

    @Test fun gentleStreak() {
        assertEquals(Streak(3, false), ChallengeLogic.streak(days(26, 27, 28), "2026-10-28"))
        // Today not done yet doesn't break it.
        assertEquals(Streak(2, false), ChallengeLogic.streak(days(26, 27), "2026-10-28"))
        // One missed day is forgiven…
        assertEquals(Streak(5, true), ChallengeLogic.streak(days(23, 24, 25, 27, 28), "2026-10-28"))
        // …but not two in a row,
        assertEquals(Streak(2, false), ChallengeLogic.streak(days(23, 24, 27, 28), "2026-10-28"))
        // …nor a second one within a week.
        assertEquals(2, ChallengeLogic.streak(days(23, 24, 26, 28), "2026-10-28").days)
        assertEquals(Streak(0, false), ChallengeLogic.streak(emptySet(), "2026-10-28"))
        assertTrue(ChallengeLogic.streakLine(Streak(5, true)).contains("grace"))
    }

    @Test fun progressAndTogether() {
        val mine = ChallengeLogic.toggle(c, emptySet(), "2026-10-28", true)
        assertEquals(setOf("2026-10-28"), mine)
        assertTrue(ChallengeLogic.toggle(c, mine, "2026-12-01", true).none { it == "2026-12-01" })
        assertTrue(ChallengeLogic.toggle(c, mine, "2026-10-28", false).isEmpty())
        val p = listOf(ChallengeProgress("aisha", "Aisha", days(20, 21, 22)), ChallengeProgress("bilal", "Bilal", days(20, 28) + "2026-09-01"))
        assertEquals(5, ChallengeLogic.together(p, c))
        assertEquals(3f / 21, ChallengeLogic.fraction(c, p[0].done), 1e-4f)
        assertEquals("5 deeds done together by 2 people, alhamdulillah", ChallengeLogic.togetherLine(5, 2))
        assertTrue(ChallengeLogic.inviteText(c).contains("ABCD2345"))
    }

    @Test fun docShapeMatchesRules() {
        val d = ChallengeLogic.newDoc("ABCD2345", "  A page a day ", ChallengeKind.QURAN, "Read", 21, "2026-10-28", "u1", "Aisha")
        assertEquals(setOf("title", "kind", "goal", "days", "startDay", "createdBy", "creatorName", "members"), d.keys)
        assertEquals("A page a day", d["title"]); assertEquals(listOf("u1"), d["members"])
        assertEquals(c.copy(code = "X"), ChallengeLogic.fromFirestore("X", mapOf("title" to c.title, "kind" to "QURAN", "goal" to c.goal, "days" to 21L,
            "startDay" to c.startDay, "createdBy" to "aisha", "creatorName" to "Aisha", "members" to c.members)))
        assertNotNull(ChallengeLogic.validateTitle("Hi"))
        assertNull(ChallengeLogic.validateTitle("Salawat week"))
    }

    @Test fun eveningReminderOnlyWhenOpen() {
        val active = listOf(Triple("A", "2026-10-20", 21), Triple("B", "2026-09-01", 7))
        assertNotNull(ChallengeReminder.pending(active, { emptySet() }, "2026-10-28"))
        assertNull(ChallengeReminder.pending(active, { if (it == "A") setOf("2026-10-28") else emptySet() }, "2026-10-28"))
        assertNull(ChallengeReminder.pending(emptyList(), { emptySet() }, "2026-10-28"))
        val d = ChallengeReminder.initialDelayMs()
        assertTrue(d in 1..24 * 3_600_000L)
    }
}
