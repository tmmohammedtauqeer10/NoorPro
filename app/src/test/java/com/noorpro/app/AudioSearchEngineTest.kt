package com.noorpro.app

import com.noorpro.app.audio.search.AudioSearchEngine as E
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSearchEngineTest {
    @Test fun transliterationVariants() {
        assertTrue(E.tier("Rehman", listOf("Ar-Rahman")) >= E.FUZZY)
        assertTrue(E.tier("yasin", listOf("Ya-Sin")) >= E.FUZZY)
        assertTrue(E.tier("Yaseen", listOf("Ya-Sin")) >= E.FUZZY)
        assertTrue(E.tier("fatihah", listOf("Al-Fatiha")) >= E.PREFIX)
        assertTrue(E.tier("mishary", listOf("Mishari Rashid al-Afasy")) >= E.FUZZY)
    }
    @Test fun arabicDiacriticsInsensitive() {
        assertEquals(E.EXACT, E.tier("الرحمن", listOf("الرَّحْمَٰن")))
        assertTrue(E.tier("يس", listOf("يسٓ")) >= E.EXACT)
        assertTrue(E.tier("امید", listOf("ابھی اُمید باقی ہے")) >= E.CONTAINS)
    }
    @Test fun typos() {
        assertTrue(E.tier("kahff", listOf("Al-Kahf")) >= E.FUZZY)
        assertTrue(E.tier("baqrah", listOf("Al-Baqarah")) >= E.FUZZY)
        assertEquals(E.NONE, E.tier("zzz", listOf("Al-Kahf")))
    }
    @Test fun rankingOrder() {
        val items = listOf("Ar-Rahman Explained", "Ar-Rahman", "Surah Rahman Tafsir", "Rehmat")
        val r = E.rank("rahman", items, { listOf(it) })
        assertEquals("Ar-Rahman", r.first())
        assertEquals(listOf("Ar-Rahman", "Ar-Rahman Explained"), r.take(2))
    }
    @Test fun numberExact() { assertEquals(E.EXACT, E.tier("36", listOf("Ya-Sin", "36"))) }
}
