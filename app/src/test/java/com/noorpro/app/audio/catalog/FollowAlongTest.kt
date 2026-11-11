package com.noorpro.app.audio.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowAlongTest {

    @Test
    fun parseRangeAndSingleLabels() {
        assertEquals(6 to 7, FollowAlong.parseAyahLabel("Ayat 6-7"))
        assertEquals(6 to 7, FollowAlong.parseAyahLabel("Ayat 6–7"))
        assertEquals(8 to 13, FollowAlong.parseAyahLabel("Ayat 8-13"))
        assertEquals(25 to 25, FollowAlong.parseAyahLabel("Ayah 25"))
        assertEquals(1 to 5, FollowAlong.parseAyahLabel("verses 1-5"))
        assertNull(FollowAlong.parseAyahLabel("commentary introduction"))
    }

    @Test
    fun inferEndFromNextClipWhenLabelMissing() {
        assertEquals(7, FollowAlong.endAyah("", 6, nextStart = 8, surahCount = 286))
        assertEquals(25, FollowAlong.endAyah("Ayah 25", 25, nextStart = 26, surahCount = 286))
        assertEquals(286, FollowAlong.endAyah("", 280, nextStart = null, surahCount = 286))
    }

    @Test
    fun verseByVerseTarjumaInterleavesArabicThenTranslation() {
        val q = FollowAlong.tarjumaQueue(
            surah = 2, ayahCount = 7, verseByVerse = true, fromAyah = 6,
            tarjumaId = { a -> "free-en-walk-2-$a" },
            tarjumaUrl = { a -> "https://cdn.example/$a.mp3" },
            arabicPrefix = "https://everyayah.com/data/Husary_128kbps/",
            reciterName = "Husary",
            editionTitle = "Ibrahim Walk",
            plusNextSurah = true,
            nextSurah = 3,
            nextAyahCount = 2,
            nextTarjumaId = { a -> "free-en-walk-3-$a" },
            nextTarjumaUrl = { a -> "https://cdn.example/3-$a.mp3" },
        )
        // 2 ayahs left in surah 2 (6,7) * 2 + next surah 2 ayahs * 2 = 8
        assertEquals(8, q.size)
        assertEquals(FollowAlong.Kind.ARABIC, q[0].kind)
        assertEquals(6, q[0].start)
        assertEquals("https://everyayah.com/data/Husary_128kbps/002006.mp3", q[0].url)
        assertEquals(FollowAlong.Kind.TARJUMA, q[1].kind)
        assertEquals(6, q[1].start)
        assertEquals(FollowAlong.Highlight.PER_AYAH, q[0].highlight)
        assertEquals(3, q[4].surah)
        assertEquals(1, q[4].start)
    }

    @Test
    fun fullSurahEditionIsOneGroupNoFakePerAyah() {
        val q = FollowAlong.tarjumaQueue(
            surah = 1, ayahCount = 7, verseByVerse = false, fromAyah = 1,
            tarjumaId = { s -> "free-en-alafasy-walk-$s" },
            tarjumaUrl = { s -> "https://cdn.example/surah/$s.mp3" },
            arabicPrefix = "https://everyayah.com/data/Husary_128kbps/",
            reciterName = "Husary",
            editionTitle = "Alafasy + Walk",
        )
        assertEquals(1, q.size)
        assertEquals(FollowAlong.Highlight.GROUP, q[0].highlight)
        assertEquals(1, q[0].start)
        assertEquals(7, q[0].end)
        assertEquals(FollowAlong.Kind.TARJUMA, q[0].kind)
    }

    @Test
    fun tafsirQueuePlaysArabicOfGroupThenTheClip() {
        val clips = listOf(
            FollowAlong.TafsirClip("free-tafsir-W-2-1", 2, 1, 5, "u1", "Ayat 1-5", "Wahiduddin"),
            FollowAlong.TafsirClip("free-tafsir-W-2-2", 2, 6, 7, "u2", "Ayat 6-7", "Wahiduddin"),
            FollowAlong.TafsirClip("free-tafsir-W-2-3", 2, 8, 13, "u3", "Ayat 8-13", "Wahiduddin"),
        )
        val q = FollowAlong.tafsirQueue(clips, startIndex = 1, arabicPrefix = "https://everyayah.com/data/Husary_128kbps", reciterName = "Husary")
        // clip 6-7: Ar6, Ar7, Tafsir6-7, then 8-13: 6 arabic + clip
        assertEquals(2 + 1 + 6 + 1, q.size)
        assertEquals(FollowAlong.Kind.ARABIC, q[0].kind)
        assertEquals(FollowAlong.Kind.ARABIC, q[1].kind)
        assertEquals(FollowAlong.Kind.TAFSIR, q[2].kind)
        assertEquals(FollowAlong.Highlight.GROUP, q[2].highlight)
        assertEquals(6, q[2].start)
        assertEquals(7, q[2].end)
        assertEquals("u2", q[2].url)
    }

    @Test
    fun nextAyahSkipsCompanionAudioAndGroupClip() {
        val clips = listOf(
            FollowAlong.TafsirClip("a", 2, 6, 7, "u2", "Ayat 6-7", "W"),
            FollowAlong.TafsirClip("b", 2, 8, 13, "u3", "Ayat 8-13", "W"),
        )
        val q = FollowAlong.tafsirQueue(clips, 0, "https://everyayah.com/data/Husary_128kbps/", "H")
        // 0=Ar6, 1=Ar7, 2=Tafsir6-7, 3=Ar8...
        assertEquals(1, FollowAlong.stepAyah(q, 0, +1)) // 6 -> 7 arabic
        assertEquals(3, FollowAlong.stepAyah(q, 1, +1)) // 7 arabic -> 8 arabic (not the group clip)
        assertEquals(3, FollowAlong.stepAyah(q, 2, +1)) // group 6-7 -> ayah 8
        assertEquals(0, FollowAlong.stepAyah(q, 1, -1))
        assertEquals(0, FollowAlong.indexForAyah(q, 2, 6))
    }

    @Test
    fun walkEditionMapsToSahihText() {
        val (key, label) = FollowAlong.translationKeyFor("English", "en-walk")
        assertEquals("en.sahih", key)
        assertTrue(label.contains("Sahih"))
        val ur = FollowAlong.translationKeyFor("Urdu", "ur-khan")
        assertEquals("ur.kanzuliman", ur.first)
    }
}
