package com.noorpro.app

import com.noorpro.app.audio.catalog.CcBayans
import com.noorpro.app.audio.search.AudioSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcBayansTest {
    @Test fun curatedListShape() {
        assertEquals(setOf("jalali", "madani"), CcBayans.channels.map { it.key }.toSet())
        CcBayans.channels.forEach { ch -> assertTrue(ch.key, CcBayans.byChannel(ch.key).size in 8..12) }
        val ids = CcBayans.all.map { it.videoId }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue(it, Regex("^[\\w-]{11}$").matches(it)) }
    }

    @Test fun creditsLinksAndThumbs() {
        assertEquals("Dr Muhammad Ashraf Asif Jalali • Official channel • CC BY", CcBayans.channel("jalali").credit)
        assertEquals("Madani Channel (Dawat-e-Islami) • CC BY", CcBayans.channel("madani").credit)
        assertEquals("https://www.youtube.com/@TheDrJalali?sub_confirmation=1", CcBayans.channel("jalali").subscribeUrl)
        assertEquals("https://www.youtube.com/@MadaniChannelOfficial?sub_confirmation=1", CcBayans.channel("madani").subscribeUrl)
        val b = CcBayans.all.first { it.videoId == "-3NlJrfVrFQ" }
        assertEquals("https://www.youtube.com/watch?v=-3NlJrfVrFQ", b.watchUrl)
        assertEquals("https://i.ytimg.com/vi/-3NlJrfVrFQ/hqdefault.jpg", b.thumbUrl)
        assertEquals("1:01:46", b.durationText)
        assertEquals("CC BY", b.licence)
    }

    @Test fun excludesPolemicalExample() {
        assertTrue(CcBayans.all.none { it.videoId == "LzWbw8o8kcU" })
    }

    @Test fun searchableByTitleScholarTopic() {
        fun hits(q: String) = AudioSearchEngine.rank(q, CcBayans.all, { it.searchFields })
        assertEquals(List(12) { "jalali" }, hits("jalali").take(12).map { it.channelKey }); assertTrue(hits("jalali").drop(12).all { AudioSearchEngine.tier("jalali", it.searchFields) < AudioSearchEngine.tier("jalali", CcBayans.all.first().searchFields) })
        assertTrue(hits("madani channel").isNotEmpty() && hits("madani channel").first().channelKey == "madani")
        assertTrue(hits("ilyas qadri").isNotEmpty())
        assertTrue(hits("namaz").map { it.videoId }.containsAll(listOf("cQ7V1--VZOk", "CzK_W-j6RRM")))
        assertTrue(hits("ramzan").size >= 4)
        assertEquals("cQ7V1--VZOk", hits("Imtiazat-e-Namaz · Fahm-e-Deen Lecture 4").first().videoId)
        assertTrue(hits("seerat").isNotEmpty() && hits("tauba").isNotEmpty() && hits("walidain").isNotEmpty() && hits("bayan").size == CcBayans.all.size)
    }

    @Test fun audioFirstSwitchIsOffByDefaultAndWorksWhenGranted() {
        // 1.0.95: every item currently uses YouTube's embedded player (no owner audio permission yet).
        assertTrue(CcBayans.all.none { it.isAudioFirst || it.toTrack() != null })
        val b = CcBayans.all.first().copy(officialAudio = com.noorpro.app.audio.catalog.BayanAudio(
            url = "https://example.org/official/bayan.mp3", licence = com.noorpro.app.audio.models.LicenseType.PERMISSION_LETTER,
            licenceLabel = "Used with permission", evidence = "Permission email from channel owner"))
        assertTrue(b.isAudioFirst)
        val tr = b.toTrack()!!
        assertEquals("https://example.org/official/bayan.mp3", tr.audioUrl)
        assertEquals(b.watchUrl, tr.sourceUrl)
        assertTrue(tr.attributionText.contains(b.channel.name) && tr.attributionText.contains("Used with permission"))
        // No evidence -> stays on the YouTube player.
        val noEv = b.copy(officialAudio = b.officialAudio!!.copy(evidence = ""))
        assertTrue(!noEv.isAudioFirst && noEv.toTrack() == null)
    }

    @Test fun reverifiedIdsMatchEvidence() {
        assertEquals(24, CcBayans.all.size)
        assertTrue(CcBayans.all.none { it.videoId == "LzWbw8o8kcU" || it.videoId == "I1BsMKbKkeY" })
        assertEquals(CcBayans.all.size, CcBayans.all.map { it.videoId }.toSet().size)
    }
}
