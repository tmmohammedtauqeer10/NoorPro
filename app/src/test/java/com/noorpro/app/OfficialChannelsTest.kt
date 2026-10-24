package com.noorpro.app

import com.noorpro.app.audio.catalog.OfficialChannels
import com.noorpro.app.audio.search.AudioSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialChannelsTest {
    private val ch = OfficialChannels.all.first { it.id == "ajmal-raza-qadri" }

    @Test fun linkIsOfficialHandleOnly() {
        assertEquals("https://www.youtube.com/@MuhammdAjmalRazaQadriOfficial", ch.url)
        assertEquals("Muhammad Ajmal Raza Qadri", ch.name)
        assertEquals("Official YouTube channel", ch.subtitle)
        assertEquals("AR", ch.initials)
    }

    @Test fun findableBySearch() {
        listOf("ajmal", "ajmal raza", "qadri", "Ajmal Raza Qadri", "ajmal qadri", "اجمل").forEach { q ->
            assertTrue("no match for $q", AudioSearchEngine.tier(q, ch.searchFields) > AudioSearchEngine.NONE)
        }
    }
}
