package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndoPakQuranPaginatorTest {
    private val widthByCharacter: (String) -> Float = { it.length * 10f }

    @Test
    fun pagesAlwaysContainSixteenRowsAndKeepEveryAyahMarkerOnce() {
        val ayahs = (1..24).map { number ->
            IndoPakAyah(2, number, "word${number}a word${number}b")
        }

        val pages = buildMushafPages(
            ayahs = ayahs,
            surahNameArabic = "البقرة",
            maxWidthPx = 120f,
            measureText = widthByCharacter
        )

        assertTrue(pages.size > 1)
        assertTrue(pages.all { it.lines.size == 16 })
        val rendered = pages.flatMap { it.lines }.joinToString(" ") { it.text }
        (1..24).forEach { number ->
            assertEquals(1, rendered.windowed(toArabicIndic(number).length + 2)
                .count { it == "﴿${toArabicIndic(number)}﴾" })
            assertTrue(rendered.contains("word${number}a"))
            assertTrue(rendered.contains("word${number}b"))
        }
    }

    @Test
    fun alFatihaFirstAyahBecomesBismillahWithoutDuplication() {
        val ayahs = (1..7).map { IndoPakAyah(1, it, "ayah$it") }

        val page = buildMushafPages(ayahs, "الفاتحة", 1_000f, widthByCharacter).single()

        assertEquals(16, page.lines.size)
        assertEquals(MushafLineKind.SURAH_TITLE, page.lines[0].kind)
        assertEquals(MushafLineKind.BISMILLAH, page.lines[1].kind)
        assertTrue(page.lines[1].text.contains("﴿١﴾"))
        val quranText = page.lines.filter { it.kind == MushafLineKind.QURAN }.joinToString(" ") { it.text }
        assertFalse(quranText.contains("﴿١﴾"))
        (2..7).forEach { assertTrue(quranText.contains("﴿${toArabicIndic(it)}﴾")) }
    }

    @Test
    fun atTawbahDoesNotInsertBismillah() {
        val pages = buildMushafPages(
            ayahs = listOf(IndoPakAyah(9, 1, "بَرَاءَةٌ")),
            surahNameArabic = "التوبة",
            maxWidthPx = 1_000f,
            measureText = widthByCharacter
        )

        assertEquals(MushafLineKind.SURAH_TITLE, pages.first().lines[0].kind)
        assertEquals(MushafLineKind.QURAN, pages.first().lines[1].kind)
        assertTrue(pages.first().lines.none { it.kind == MushafLineKind.BISMILLAH })
    }

    @Test
    fun unsupportedSourceControlGlyphsAreRemovedWithoutChangingArabic() {
        assertEquals("بِسْمِ الله", normalizeWord("\u200Fبِسْمِ\uE022 \u200Bالله\uFEFF"))
    }
}
