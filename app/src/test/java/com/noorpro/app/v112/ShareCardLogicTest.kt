package com.noorpro.app.v112

import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.islamic.social.*
import org.junit.Assert.*
import org.junit.Test

class ShareCardLogicTest {
    @Test fun ayahRefs() {
        assertEquals(2 to 255, ShareCardLogic.parseAyah("2:255"))
        assertEquals(94 to 6, ShareCardLogic.parseAyah(" 94 . 6 "))
        assertNull(ShareCardLogic.parseAyah("2:287"))
        assertNull(ShareCardLogic.parseAyah("115:1"))
        assertNull(ShareCardLogic.parseAyah("Bukhari 13"))
        ShareCardLogic.AYAH_PICKS.forEach { assertNotNull(it.ref, ShareCardLogic.parseAyah(it.ref)) }
    }

    @Test fun hadithRefsOnlySahihCollections() {
        assertEquals("muslim" to 6592, ShareCardLogic.parseHadith("Sahih Muslim #6592"))
        assertEquals("bukhari" to 13, ShareCardLogic.parseHadith("bukhari 13"))
        assertNull(ShareCardLogic.parseHadith("Tirmidhi 5"))
        assertNull(ShareCardLogic.parseHadith("Bukhari"))
        ShareCardLogic.HADITH_PICKS.forEach { assertNotNull(it.ref, ShareCardLogic.parseHadith(it.ref)) }
    }

    @Test fun captionsKeepTheReference() {
        val c = CardContent(CardKind.HADITH, "", "x".repeat(400), "Sahih al-Bukhari 1", "Bukhari 1")
        assertEquals("Sahih al-Bukhari 1", ShareCardLogic.caption(c, ComposerDestination.STORY))
        assertTrue(ShareCardLogic.caption(c, ComposerDestination.FEED).endsWith("— Sahih al-Bukhari 1"))
        assertEquals("Hadith", c.topic)
    }

    @Test fun basmalaStripped() {
        val b = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", ShareCardLogic.stripBasmala("$b قُلْ هُوَ ٱللَّهُ أَحَدٌ", b, 112, 1))
        assertEquals(b, ShareCardLogic.stripBasmala(b, b, 1, 1))
        assertEquals("$b x y z w", ShareCardLogic.stripBasmala("$b x y z w", b, 2, 5))
    }

    @Test fun tidiesBundledText() {
        assertEquals("Those who believe", ShareCardLogic.unquote("\"\"Those who believe\"\""))
        assertEquals("the believers' hearts", ShareCardLogic.unquote("“the believers' hearts”"))
        assertEquals("Narrated 'Aisha: Allah's Messenger said, \"Do good deeds\"", ShareCardLogic.tidyHadith("Narrated 'Aisha:Allah's Messenger  said, \"Do good deeds"))
        assertEquals("Ratio 3:2 kept", ShareCardLogic.tidyHadith("Ratio 3:2 kept"))
    }
}
