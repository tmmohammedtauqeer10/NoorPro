package com.noorpro.app.v111

import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.islamic.hifz.*
import com.noorpro.app.quran.HifzCard
import com.noorpro.app.quran.HifzStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HifzCoachLogicTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val ikhlas1 = "قُلْ هُوَ ٱللَّهُ أَحَدٌ"
    private val kursiStart = "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ سِنَةٌۭ وَلَا نَوْمٌۭ"

    @Test fun normalizerStripsTashkeelAndUnifiesLetters() {
        assertEquals("قل", ArabicNormalizer.normalize("قُلْ"))
        assertEquals("الله", ArabicNormalizer.normalize("ٱللَّهُ"))
        assertEquals("احد", ArabicNormalizer.normalize("أَحَدٌ"))
        assertEquals("اله", ArabicNormalizer.normalize("إِلَٰهَ"))
        assertEquals("الحي", ArabicNormalizer.normalize("ٱلْحَىُّ"))
        assertEquals("تاخذه", ArabicNormalizer.normalize("تَأْخُذُهُۥ"))
        assertEquals("", ArabicNormalizer.normalize("ۚ"))
        assertEquals("رحمه", ArabicNormalizer.normalize("رَحْمَةً"))
        assertEquals(listOf("الله", "لا", "اله", "الا", "هو", "الحي", "القيوم", "لا", "تاخذه", "سنه", "ولا", "نوم"), ArabicNormalizer.words(kursiStart))
        assertEquals(12, ArabicNormalizer.displayWords(kursiStart).size) // pause mark dropped, 1:1 with words()
    }

    @Test fun uthmaniVsStandardSpelling() {
        assertTrue(ArabicNormalizer.sameWord(ArabicNormalizer.normalize("ٱلسَّمَٰوَٰتِ"), "السماوات"))
        assertTrue(ArabicNormalizer.sameWord(ArabicNormalizer.normalize("يَـُٔودُهُۥ"), ArabicNormalizer.normalize("يؤوده")))
        assertTrue(ArabicNormalizer.sameWord(ArabicNormalizer.normalize("ٱلْعَٰلَمِينَ"), "العالمين"))
        assertFalse(ArabicNormalizer.sameWord("الناس", "الفلق"))
        assertFalse(ArabicNormalizer.sameWord("قل", "هو"))
    }

    @Test fun bismillahIsStrippedExceptFatiha() {
        val withB = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ قُلْ هُوَ ٱللَّهُ أَحَدٌ"
        assertEquals(ikhlas1, ArabicNormalizer.stripBismillah(112, 1, withB))
        assertEquals(withB, ArabicNormalizer.stripBismillah(1, 1, withB))
        assertEquals("ٱللَّهُ ٱلصَّمَدُ", ArabicNormalizer.stripBismillah(112, 2, "ٱللَّهُ ٱلصَّمَدُ"))
    }

    @Test fun recitationCheckerMarksMissedAndWrong() {
        val perfect = RecitationChecker.check(ikhlas1, "قل هو الله احد")
        assertEquals(1f, perfect.score); assertTrue(perfect.passed)
        val missed = RecitationChecker.check(ikhlas1, "قل الله احد")
        assertEquals(listOf(WordStatus.CORRECT, WordStatus.MISSED, WordStatus.CORRECT, WordStatus.CORRECT), missed.words.map { it.status })
        val wrong = RecitationChecker.check(ikhlas1, "قل هو الرحمن احد")
        assertEquals(WordStatus.WRONG, wrong.words[2].status)
        assertEquals("الرحمن", wrong.words[2].heard)
        val extra = RecitationChecker.check(ikhlas1, "قل يا هو الله احد")
        assertEquals(1, extra.extra); assertEquals(1f, extra.score)
        val k = RecitationChecker.check(kursiStart, "الله لا إله إلا هو الحي القيوم لا تأخذه سنة ولا نوم")
        assertEquals(12, k.correct)
        val empty = RecitationChecker.check(ikhlas1, "")
        assertEquals(4, empty.missed); assertFalse(empty.passed)
    }

    @Test fun hidingIsProgressive() {
        val n = 40
        val counts = (0..4).map { HifzHide.hiddenCount(n, it) }
        assertEquals(listOf(0, 10, 20, 30, 40), counts)
        for (l in 1..3) for (i in 0 until n) if (HifzHide.hidden(i, l)) assertTrue(HifzHide.hidden(i, l + 1))
        assertEquals(5, HifzHide.labels.size)
    }

    @Test fun planGroupsDueAyahsAndForecasts() {
        val today = 20_000L
        val cards = mapOf(
            (112 to 1) to HifzCard(1, 1, 2.5, today - 1), (112 to 2) to HifzCard(1, 1, 2.5, today), (112 to 4) to HifzCard(1, 1, 2.5, today),
            (1 to 3) to HifzCard(3, 7, 2.6, today),
            (114 to 1) to HifzCard(3, 8, 2.6, today + 2), (114 to 2) to HifzCard(1, 3, 2.5, today + 2), (114 to 3) to HifzCard(1, 1, 2.5, today + 7),
        )
        val p = HifzPlanner.plan(cards, today)
        assertEquals(listOf(1, 112), p.due.map { it.surah })
        assertEquals("1–2, 4", p.due[1].label)
        assertEquals(4, p.dueCount)
        assertEquals(2, p.memorised); assertEquals(5, p.learning)
        assertEquals(listOf(0, 2, 0, 0, 0, 0, 1), p.forecast)
        assertEquals("1–3, 7", DueGroup(1, listOf(1, 2, 3, 7)).label)
    }

    @Test fun storeExposesCardsForThePlan() {
        val s = HifzStore(ctx)
        s.grade(112, 1, true); s.grade(112, 2, false)
        val all = s.allCards()
        assertEquals(setOf(112 to 1, 112 to 2), all.keys)
        assertEquals(1, s.streak())
        assertEquals(s.todayEpochDay() + 1, all[112 to 2]!!.due)
    }

    @Test fun bundledQuranRangesAndAiPrompt() {
        val surahs = HifzQuran.surahs(ctx)
        assertEquals(114, surahs.size)
        assertEquals(7, surahs[0].ayahs); assertEquals(286, surahs[1].ayahs)
        val ay = HifzQuran.ayahs(ctx, HifzRange(112, 1, 4))
        assertEquals(4, ay.size)
        assertEquals(listOf("قل", "هو", "الله", "احد"), ArabicNormalizer.words(ay[0].text))
        assertEquals(HifzRange(112, 3, 4), HifzQuran.clamp(HifzRange(112, 3, 9), 4))
        val prompt = HifzAiPrompt.tips("Al-Ikhlaas", HifzRange(112, 1, 4), RecitationChecker.check(ikhlas1, "قل الله احد"))
        assertTrue(prompt.contains("3 of 4") && prompt.contains("qualified teacher"))
    }
}
