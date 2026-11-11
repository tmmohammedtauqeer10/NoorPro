package com.noorpro.app.v111

import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.PrayerLogEntity
import com.noorpro.app.islamic.deen.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeenGoalsLogicTest {
    private val cfg = DeenGoalsConfig()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun active(day: Long) = DeenDay(day, done = setOf(DeenGoal.DUA))

    @Test fun progressAndCompletion() {
        val d = DeenDay(10, salah = 0b10111, pages = 1, done = setOf(DeenGoal.MORNING, DeenGoal.SADAQAH))
        assertEquals(0.8f, DeenGoalsLogic.progress(DeenGoal.SALAH, d, cfg), 1e-4f)
        assertEquals(0.5f, DeenGoalsLogic.progress(DeenGoal.QURAN, d, cfg), 1e-4f)
        assertEquals(2, DeenGoalsLogic.completed(d, cfg))
        assertTrue(DeenGoalsLogic.complete(DeenGoal.QURAN, d.copy(pages = 5), cfg))
        assertEquals(DeenGoal.SALAH, DeenGoalsLogic.nextGoal(d, cfg))
        val few = DeenGoalsConfig(setOf(DeenGoal.MORNING, DeenGoal.SADAQAH))
        assertEquals(1f, DeenGoalsLogic.score(d, few), 1e-4f); assertNull(DeenGoalsLogic.nextGoal(d, few))
    }

    @Test fun salahReusesPrayerTrackerLogs() {
        val log = PrayerLogEntity("2026-10-28", fajr = true, asr = true, isha = true)
        assertEquals(0b10101, DeenGoalsLogic.salahMask(log)); assertEquals(3, DeenGoalsLogic.salahCount(0b10101))
        assertEquals(java.time.LocalDate.of(2026, 10, 28).toEpochDay(), DeenGoalsLogic.dayOf("2026-10-28"))
        assertNull(DeenGoalsLogic.dayOf("bad"))
    }

    @Test fun streaksAreGentle() {
        val t = 1000L
        // empty today never breaks the run
        val days = (t - 5..t - 1).associateWith { active(it) }
        assertEquals(5, DeenGoalsLogic.streak(days, cfg, t))
        assertEquals(6, DeenGoalsLogic.streak(days + (t to active(t)), cfg, t))
        // a single quiet day is forgiven, two end it
        val gap1 = days - (t - 3)
        assertEquals(4, DeenGoalsLogic.streak(gap1, cfg, t))
        val gap2 = days - (t - 3) - (t - 2)
        assertEquals(1, DeenGoalsLogic.streak(gap2, cfg, t))
        assertEquals(0, DeenGoalsLogic.streak(emptyMap(), cfg, t))
        // one prayer is enough to show up
        assertEquals(1, DeenGoalsLogic.streak(mapOf(t to DeenDay(t, salah = 1)), cfg, t))
    }

    @Test fun copyNeverShames() {
        val banned = listOf("fail", "missed", "lazy", "broke", "shame", "lost", "should have")
        val lines = (0..10).flatMap { s -> (0..3).map { DeenGoalsLogic.streakLine(s, it) } } +
            (0..7).map { n -> DeenGoalsLogic.week((1000L - n + 1..1000L).associateWith { active(it) }, cfg, 1000L).headline }
        lines.forEach { l -> banned.forEach { b -> assertFalse("'$l' contains '$b'", l.lowercase().contains(b)) } }
        assertTrue(DeenGoalsLogic.week(emptyMap(), cfg, 1000).headline.contains("open"))
        val p = DeenGoalsLogic.aiPrompt(DeenGoalsLogic.week(emptyMap(), cfg, 1000))
        assertTrue(p.contains("never shaming"))
    }

    @Test fun weeklySummary() {
        val t = 500L
        val days = mapOf(t to DeenDay(t, 0b11111, 2, setOf(DeenGoal.MORNING, DeenGoal.EVENING, DeenGoal.SADAQAH, DeenGoal.DUA)),
            t - 2 to DeenDay(t - 2, 0b00011, 1), t - 9 to active(t - 9))
        val w = DeenGoalsLogic.week(days, cfg, t)
        assertEquals((t - 6..t).toList(), w.days)
        assertEquals(2, w.activeDays); assertEquals(7, w.salahPrayed); assertEquals(3, w.pagesRead); assertEquals(t, w.bestDay)
        assertEquals(1, w.perGoal.first { it.goal == DeenGoal.SALAH }.count)
        assertEquals(1f, w.dayScores.last(), 1e-4f)
    }

    @Test fun storePersistsMergesAndWritesWidgetSnapshot() {
        val s = DeenGoalsStore(ctx)
        s.toggle(20_000, DeenGoal.SADAQAH); s.toggle(20_000, DeenGoal.DUA); s.toggle(20_000, DeenGoal.DUA)
        s.toggle(20_000, DeenGoal.SALAH) // salah lives in the prayer tracker: ignored here
        s.addPages(20_000, 3); s.addPages(20_000, -5)
        val d = DeenGoalsStore(ctx).days()[20_000]!!
        assertEquals(setOf(DeenGoal.SADAQAH), d.done); assertEquals(0, d.pages)
        val m = s.merged(listOf(PrayerLogEntity(java.time.LocalDate.ofEpochDay(20_000).toString(), fajr = true, dhuhr = true)))
        assertEquals(0b11, m[20_000]!!.salah)
        s.setConfig(DeenGoalsConfig(setOf(DeenGoal.SALAH, DeenGoal.QURAN), 99))
        assertEquals(DeenGoalsLogic.MAX_PAGES_TARGET, s.config().quranPages); assertEquals(2, s.config().goals.size)
        val snap = DeenGoalsLogic.snapshot(m[20_000]!!, s.config(), 4)
        s.saveWidget(snap)
        assertEquals(snap, DeenGoalsStore(ctx).widgetSnapshot())
        assertNotNull(ctx.getSharedPreferences(DeenGoalsStore.WIDGET_PREFS, 0).getString("snapshot", null))
        assertNull(DeenGoalsLogic.snapshotFrom("{oops"))
    }
}
