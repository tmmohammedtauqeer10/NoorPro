package com.noorpro.app.v116

import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.quiz.QuizBank
import com.noorpro.app.data.spiritual.BookLibrary
import com.noorpro.app.data.spiritual.SpiritualContentRepository
import com.noorpro.app.data.spiritual.TasawwufPath
import com.noorpro.app.data.spiritual.TasawwufPathStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 1.2.0 (116): Deenflow quiz merge, Tasawwuf path, Arabic books, and the referenced Adhkar / Ruhani / Darood content. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContentV116Test {
    private val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test fun quizMergesDeenflowWithNativeTranslations() {
        val bank = QuizBank.load(ctx)
        assertTrue("bank ${bank.size}", bank.size >= 420)
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        val df = bank.filter { it.id.startsWith("df_") }
        assertTrue("deenflow ${df.size}", df.size >= 100)
        df.forEach { q -> assertTrue(q.id, q.source.isNotBlank() && q.source.contains("Deenflow")) }
        val native = df.filter { q -> listOf("ur", "hi", "ar").all { q.isTranslated(it) } }
        assertTrue("native ${native.size}", native.size >= 95)
        // no Deenflow question repeats an existing question's English text
        val norm = { s: String -> s.lowercase().replace(Regex("[^a-z0-9]"), "") }
        val existing = bank.filter { !it.id.startsWith("df_") }.map { norm(it.textFor("en")) }.toSet()
        assertTrue(df.none { norm(it.textFor("en")) in existing })
        // madhhab-sensitive Deenflow questions were dropped rather than given a single answer
        assertTrue(bank.none { it.id == "df_fiq_002" || it.id == "df_fiq_004" })
    }

    @Test fun tasawwufPathParsesAndTracksProgress() {
        val p = TasawwufPath.load(ctx)
        assertEquals(12, p.weeks); assertEquals(24, p.lessons.size); assertEquals(12, p.modules.size)
        assertTrue(p.glossary.size >= 20 && p.faqs.size >= 7 && p.books.size >= 9)
        assertTrue(p.orientationBody.contains("Ahl as-Sunnah"))
        assertTrue(p.lessons.all { it.summary.isNotBlank() && it.check != null && it.check!!.answer in it.check!!.options.indices })
        assertEquals(p.ordered.first(), p.resume(emptySet()))
        val first = p.ordered.first().id
        assertEquals(p.ordered[1], p.nextAfter(first))
        val store = TasawwufPathStore(ctx); store.reset()
        store.setDone(first, true); store.setAnswer(first, 1)
        assertEquals(setOf(first), store.done()); assertEquals(1, store.answer(first))
        assertEquals(1f / 24, p.progress(store.done()), 1e-4f)
        p.books.filter { it.reader.isNotBlank() }.forEach { b -> assertNotNull(b.reader, BookLibrary.find(ctx, b.reader)) }
    }

    @Test fun arabicBooksAreBundledWithAttribution() {
        val bid = BookLibrary.load(ctx, "bidaya", "ar"); val nw = BookLibrary.load(ctx, "nawawi40", "ar")
        assertTrue(bid.rtl && bid.sections.size >= 30 && bid.attribution.contains("CC BY-SA"))
        assertTrue(nw.sections.size >= 80 && nw.attribution.contains("CC BY-SA"))
    }

    @Test fun adhkarAreCompleteAndReferenced() {
        val s = SpiritualContentRepository.load(ctx, "adhkar")
        assertEquals(listOf("morning", "evening", "after_salah", "daily"), s.categories.map { it.id })
        s.categories.forEach { c -> assertTrue(c.id, c.items.size >= 10) }
        s.categories.flatMap { it.items }.forEach { i ->
            listOf(i.arabic, i.translit, i.english, i.urdu, i.hindi, i.source).forEach { assertTrue(i.id, it.isNotBlank()) }
            assertTrue(i.id, Regex("Quran|Sahih|Sunan|Jami'|an-Nasa'i|Tabarani").containsMatchIn(i.source))
        }
        val m = s.categories.first { it.id == "morning" }.items
        assertEquals(100, m.first { it.id.endsWith("m_subhan100") }.count)
        assertEquals(7, m.first { it.id.endsWith("m_hasbi") }.count)
        assertEquals(33, s.categories.first { it.id == "after_salah" }.items.first { it.id.endsWith("s_tasbih") }.count)
    }

    @Test fun ruhaniAndDaroodUseOwnTranslations() {
        val ru = SpiritualContentRepository.load(ctx, "ruhani"); val da = SpiritualContentRepository.load(ctx, "darood")
        listOf(ru, da).flatMap { it.categories }.flatMap { it.items }.forEach { i ->
            assertTrue(i.id, i.english.isNotBlank() && i.urdu.isNotBlank() && i.hindi.isNotBlank() && i.source.isNotBlank())
            assertFalse(i.id, i.source.contains("Ahmed Raza Khan translation") || i.source.contains("Kandhlawi") || i.source.contains("Arabic and English"))
        }
        assertTrue(ru.disclaimer.contains("doctor") && ru.disclaimer.contains("amulet"))
        val fri = da.categories.first { it.id == "friday" }
        assertTrue(fri.items.any { it.source.contains("Abi Dawud 1047") })
        assertEquals(10, ru.categories.first { it.id == "burda" }.items.size)
    }
}
