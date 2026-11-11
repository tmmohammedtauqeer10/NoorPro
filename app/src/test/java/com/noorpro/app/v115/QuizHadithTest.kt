package com.noorpro.app.v115

import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.quiz.*
import com.noorpro.app.ui.screens.HadithCollections
import com.noorpro.app.ui.viewmodel.DeenViewModel
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.zip.GZIPInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuizHadithTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val bank by lazy { QuizBank.load(ctx) }

    @Test fun bankIsCompleteAndWellFormed() {
        assertTrue("bank size ${bank.size}", bank.size >= 300)
        QuizCat.entries.forEach { c -> assertTrue("$c", bank.count { it.cat == c } >= 30) }
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        bank.forEach { q ->
            val n = q.optionsFor("en").size
            assertTrue(q.id, n in 2..4)
            QuizMadhhab.all.forEach { m -> assertTrue("${q.id} $m", q.answerFor(m) in 0 until n); assertTrue(q.explanationFor(m, "en").isNotBlank()) }
            assertTrue(q.id, q.source.isNotBlank())
            assertEquals(q.id, n, q.optionsFor("en").toSet().size)
        }
        assertTrue(bank.count { it.hasMadhhabVariants } >= 15)
    }

    @Test fun fiqhFollowsMadhhab() {
        val witr = bank.first { it.textFor("en").startsWith("What is the ruling of the Witr prayer") }
        assertEquals("Wajib", witr.optionsFor("en")[witr.answerFor(QuizMadhhab.HANAFI)])
        assertEquals("Sunnah mu'akkadah", witr.optionsFor("en")[witr.answerFor(QuizMadhhab.SHAFII)])
        val asr = bank.first { it.textFor("en").startsWith("When does the time of Asr begin") }
        assertTrue(asr.optionsFor("en")[asr.answerFor("hanafi")].contains("twice"))
        assertTrue(asr.optionsFor("en")[asr.answerFor("maliki")].contains("equals"))
        assertTrue(asr.explanationFor("shafii", "en").startsWith("Shafi'i"))
        assertEquals(QuizMadhhab.HANAFI, QuizMadhhab.norm(null))
        assertEquals(QuizMadhhab.SHAFII, QuizMadhhab.norm("Shafi'i"))
    }

    @Test fun dailyAndSessions() {
        val a = QuizEngine.daily(bank, "2026-11-11"); val b = QuizEngine.daily(bank, "2026-11-11"); val c = QuizEngine.daily(bank, "2026-11-12")
        assertEquals(10, a.size); assertEquals(a.map { it.id }, b.map { it.id }); assertNotEquals(a.map { it.id }, c.map { it.id })
        assertTrue(a.none { it.cat == QuizCat.KIDS })
        assertEquals(8, a.map { it.cat }.toSet().size)
        assertTrue(QuizEngine.daily(bank, "2026-11-11", kids = true).all { it.cat == QuizCat.KIDS })
        val hard = QuizEngine.session(bank, QuizCat.FIQH, 3, 1L)
        assertTrue(hard.isNotEmpty() && hard.all { it.cat == QuizCat.FIQH && it.level == 3 })
        val seen = QuizEngine.session(bank, QuizCat.QURAN, null, 2L).map { it.id }.toSet()
        assertTrue(QuizEngine.session(bank, QuizCat.QURAN, null, 3L, avoid = seen).take(10).none { it.id in seen })
        assertEquals(0, QuizEngine.xpFor(2, false)); assertEquals(15, QuizEngine.xpFor(2, true)); assertEquals(22, QuizEngine.xpFor(3, true, fast = true))
        assertEquals(4, QuizEngine.nextStreak("2026-11-10", 3, "2026-11-11", "2026-11-10"))
        assertEquals(3, QuizEngine.nextStreak("2026-11-11", 3, "2026-11-11", "2026-11-10"))
        assertEquals(1, QuizEngine.nextStreak("2026-11-01", 9, "2026-11-11", "2026-11-10"))
    }

    @Test fun storeTracksProgressAndMistakes() {
        val s = QuizStore(ctx)
        val q = bank.first()
        s.recordAnswer(q, false, 0)
        assertTrue(q.id in s.mistakes)
        s.recordAnswer(q, true, 15)
        assertFalse(q.id in s.mistakes); assertEquals(15, s.xp); assertEquals(2, s.answered); assertEquals(1, s.correct)
        assertEquals(1, s.finishDaily(8, 20)); assertEquals(8, s.lastDailyScore); assertEquals(35, s.xp)
        assertEquals(1, s.finishDaily(9, 20)); assertEquals(35, s.xp) // bonus once per day
        s.madhhab = "maliki"; assertEquals("maliki", s.madhhab)
    }

    @Test fun aiOutputIsValidatedAndTranslationsParse() {
        val good = """Here you go: [{"q":"Which surah is called the heart of the Quran by many scholars?","options":["Yasin","Al-Mulk","Al-Kahf","Taha"],"answer":0,"explanation":"Narrated in at-Tirmidhi.","source":"Sunan at-Tirmidhi 2887"},
            {"q":"Too short","options":["a","b","c","d"],"answer":0,"explanation":"x","source":"y"},
            {"q":"Which option is duplicated in this list of answers?","options":["A","A","B","C"],"answer":1,"explanation":"x","source":"y"},
            {"q":"Which answer index is out of range for this question?","options":["A","B","C","D"],"answer":7,"explanation":"x","source":"y"},
            {"q":"Is this question missing its source reference entirely?","options":["A","B","C","D"],"answer":1,"explanation":"x","source":""}]"""
        val parsed = QuizAi.parseGenerated(good, QuizCat.QURAN, 2)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].generated && parsed[0].source.contains("checked by Noor AI"))
        assertEquals(0, QuizAi.parseCheck(" 1"))
        assertEquals(2, QuizAi.parseCheck("The answer is 3) ..."))
        assertNull(QuizAi.parseCheck("no idea"))
        assertTrue(QuizAi.generationPrompt(QuizCat.FIQH, "shafii", 5, 2).contains("Shafi'i madhhab"))

        val q = bank.first { it.hasMadhhabVariants }
        val block = QuizAi.translationBlock(q, "hanafi")
        assertTrue(block.startsWith("[Q] ") && block.contains("[1] ") && block.contains("[E] "))
        val n = q.optionsFor("en").size
        val reply = "[Q] سوال\n" + (1..n).joinToString("\n") { "[$it] جواب $it" } + "\n[E] وضاحت"
        val tr = QuizAi.parseTranslation(reply, n)!!
        val ur = QuizAi.applyTranslation(q, "ur", "hanafi", tr)
        assertEquals("سوال", ur.textFor("ur")); assertEquals("جواب 1", ur.optionsFor("ur")[0]); assertEquals("وضاحت", ur.explanationFor("hanafi", "ur"))
        assertTrue(ur.isTranslated("ur"))
        assertNull(QuizAi.parseTranslation("[Q] only a question", n))
        // Round-trip through JSON (generated questions are stored this way)
        assertEquals(ur, QuizBank.parseQuestion(JSONObject(ur.toJson().toString())))
    }

    @Test fun everyListedHadithCollectionIsBundled() {
        val info = JSONObject(GZIPInputStream(ctx.assets.open("hadith/info.jgz")).bufferedReader().readText())
        assertEquals(10, HadithCollections.all.size)
        HadithCollections.all.forEach { b ->
            val sections = info.getJSONObject(b.id).getJSONObject("metadata").getJSONObject("sections")
            val ids = sections.keys().asSequence().filter { it != "0" && sections.getString(it).isNotBlank() }.toList()
            assertTrue(b.id, ids.isNotEmpty())
            listOf("ara", "eng").forEach { ed ->
                val files = ctx.assets.list("hadith/$ed-${b.id}").orEmpty().toSet()
                ids.forEach { assertTrue("$ed-${b.id}/$it", "$it.jgz" in files) }
            }
        }
    }

    private fun waitFor(what: String, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (!cond()) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (System.currentTimeMillis() > end) fail("timed out: $what")
            Thread.sleep(50)
        }
    }

    @Test fun hadithLibraryChaptersAndReadingLoadOffline() {
        val vm = DeenViewModel(ctx)
        for (book in listOf("bukhari", "malik", "nawawi", "qudsi", "dehlawi", "ibnmajah")) {
            vm.selectHadithBook(book)
            waitFor("$book chapters") { !vm.isLoadingHadith.value && vm.hadithChapters.value.firstOrNull()?.bookId == book }
            val first = vm.hadithChapters.value.first()
            vm.selectHadithChapter(first.chapterId)
            waitFor("$book reading") { !vm.isLoadingHadith.value && vm.hadithList.value.firstOrNull()?.chapterId == first.chapterId && vm.hadithList.value.isNotEmpty() }
            val h = vm.hadithList.value.first()
            assertTrue(book, h.arabicText.isNotBlank())
            assertFalse(book, h.translationText == "Translation not available")
        }
    }
}
