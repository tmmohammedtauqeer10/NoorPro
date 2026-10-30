package com.noorpro.app.v112

import com.noorpro.app.audio.podcast.Episode
import com.noorpro.app.audio.podcast.EpisodeChapter
import com.noorpro.app.audio.podcast.Podcast
import com.noorpro.app.audio.podcast.PodcastRepository
import com.noorpro.app.islamic.audio.*
import org.junit.Assert.*
import org.junit.Test

class CarBayanLogicTest {
    private fun pod(id: String, name: String, publisher: String = "Pub", cat: String = "Bayan", kind: String = "podcast", feed: String = "https://example.org/$id.rss") =
        Podcast(id, name, publisher, "Urdu", cat, feed, "", kind)

    @Test fun islahEMuashraIsNeverAPodcast() {
        assertTrue(BayanLogic.isExcluded(pod("x", "Bayanat", publisher = "Idara Islah-e-Muashra")))
        assertTrue(BayanLogic.isExcluded(pod("x", "Muhammad Ajmal Raza Qadri bayans")))
        assertTrue(BayanLogic.isExcluded(pod("x", "Talks", feed = "https://anchor.fm/s/islah-e-muashra/podcast/rss")))
        assertTrue(BayanLogic.isExcluded(pod("x", "Islah e Muashra official")))
        assertFalse(BayanLogic.isExcluded(pod("x", "Tariq Jamil Podcast")))
        val withBad = PodcastRepository.podcasts + pod("bad", "Ajmal Raza Qadri", publisher = "Islah-e-Muashra")
        val shows = BayanLogic.shows(withBad)
        assertTrue(shows.none { it.id == "bad" })
        assertTrue(BayanLogic.shows().none { BayanLogic.isExcluded(it) })
    }

    @Test fun shelfIsTalksOnlyFromPublisherFeeds() {
        val shows = BayanLogic.shows()
        assertTrue(shows.isNotEmpty())
        assertTrue(shows.all { it.kind == "podcast" })
        assertTrue(shows.none { it.id == "qc-urdu" || it.id == "hindi-minshawi" })
        assertTrue(shows.any { it.id == "tariq-jamil" } && shows.any { it.id == "yaqeen" })
        assertEquals(setOf(com.noorpro.app.audio.models.LicenseType.PUBLISHER_RSS), BayanLogic.ALLOWED_LICENSES)
        val e = Episode("g1", "Ep", "https://x/a.mp3", 0, 0, null)
        assertTrue(shows.all { PodcastRepository.toTrack(it, e, null).license in BayanLogic.ALLOWED_LICENSES })
        assertEquals(listOf("tariq-jamil"), BayanLogic.filter(shows, "Urdu", "tariq").map { it.id })
        assertTrue(BayanLogic.filter(shows, "Arabic", "").all { it.language == "Arabic" })
    }

    @Test fun chaptersFromShowNotes() {
        val notes = """Today's bayan.
            00:00 Opening dua
            [05:12] Patience in hardship
            12:30 - The story of Ayyub (AS)
            12:10 out of order line
            1:02:03 Closing du'a.
            visit 2:3 nothing""".trimIndent()
        val ch = BayanLogic.chaptersFromNotes(notes)
        assertEquals(listOf(0L, 312_000L, 750_000L, 3_723_000L), ch.map { it.startMs })
        assertEquals("The story of Ayyub (AS)", ch[2].title)
        assertEquals("Closing du'a", ch[3].title)
        assertTrue(BayanLogic.chaptersFromNotes("only 00:00 one").isEmpty())
        assertTrue(BayanLogic.chaptersFromNotes("00:00 a\n10:00 b", durationMs = 300_000).isEmpty())
        assertNull(BayanLogic.parseClock("12:75"))
        assertEquals(3_723_000L, BayanLogic.parseClock("1:02:03"))
        assertEquals(1, BayanLogic.chapterAt(ch, 400_000))
        assertEquals(-1, BayanLogic.chapterAt(listOf(EpisodeChapter(10_000, "a")), 0))
        val inline = Episode("g", "t", "u", 0, 0, null, notes = notes, chapters = listOf(EpisodeChapter(0, "A"), EpisodeChapter(60_000, "B")))
        assertEquals(listOf("A", "B"), BayanLogic.offlineChapters(inline).map { it.title })
        assertEquals("1:02:03", BayanLogic.clock(3_723_000)); assertEquals("5:07", BayanLogic.clock(307_000))
    }

    @Test fun resumeRules() {
        assertEquals(0L, BayanLogic.resumeFrom(20_000, 3_600_000))          // barely started
        assertEquals(595_000L, BayanLogic.resumeFrom(600_000, 3_600_000))   // rewinds 5 s
        assertEquals(0L, BayanLogic.resumeFrom(3_570_000, 3_600_000))       // finished → start over
        assertEquals(595_000L, BayanLogic.resumeFrom(600_000, 0))           // unknown duration
        assertEquals("50 min left", BayanLogic.remainingLabel(600_000, 3_600_000))
        assertEquals("Finished", BayanLogic.remainingLabel(3_570_000, 3_600_000))
        assertEquals("Resume at 10:00", BayanLogic.remainingLabel(600_000, 0))
        val pts = listOf(
            ResumePoint("a", "p", "A", "u", 600_000, 3_600_000, 3), ResumePoint("b", "p", "B", "u", 10_000, 3_600_000, 9),
            ResumePoint("c", "p", "C", "u", 3_590_000, 3_600_000, 8), ResumePoint("d", "p", "D", "u", 900_000, 3_600_000, 5))
        assertEquals(listOf("d", "a"), BayanLogic.inProgress(pts).map { it.trackId })
    }

    @Test fun carIdsAndTree() {
        assertEquals(CarTarget.Surah(18), CarModeLogic.parse(CarModeLogic.surahId(18)))
        assertNull(CarModeLogic.parse("car_surah:115"))
        assertEquals(CarTarget.Routine("r1"), CarModeLogic.parse(CarModeLogic.routineId("r1")))
        assertEquals(CarTarget.Episode("tariq-jamil", "pod-tariq-jamil-123"), CarModeLogic.parse(CarModeLogic.episodeId("tariq-jamil", "pod-tariq-jamil-123")))
        assertNull(CarModeLogic.parse("car_ep:broken"))
        assertEquals("yaqeen", CarModeLogic.showIdOf(CarModeLogic.showId("yaqeen")))

        assertEquals(listOf(CarModeLogic.QURAN, CarModeLogic.ALNOOR), CarModeLogic.root(false, 0, 3).map { it.id })
        assertEquals(listOf(CarModeLogic.QURAN), CarModeLogic.root(false, 0, 3, alNoor = false).map { it.id })
        assertEquals(listOf(CarModeLogic.CONTINUE, CarModeLogic.QURAN, CarModeLogic.ALNOOR, CarModeLogic.ROUTINES, CarModeLogic.BAYANS), CarModeLogic.root(true, 2, 1).map { it.id })
        // 1.1.9 Al-Noor Audio in the car
        assertEquals(CarTarget.AlNoor(2, 5), CarModeLogic.parse(CarModeLogic.alNoorTrackId(2, 5)))
        assertNull(CarModeLogic.parse("car_alnoor_t:x|1"))
        assertEquals(3, CarModeLogic.alNoorGroupOf(CarModeLogic.alNoorGroupId(3)))
        val g = CarModeLogic.alNoorGroups(listOf("Morning" to 12, "Evening" to 9))
        assertTrue(g.none { it.playable }); assertEquals("12 tracks · Al-Noor Audio", g[0].subtitle)
        val tr = CarModeLogic.alNoorTracks(1, listOf("Dua 1" to "Hisn", "Dua 2" to "Hisn"))
        assertTrue(tr.all { it.playable }); assertEquals(CarTarget.AlNoor(1, 1), CarModeLogic.parse(tr[1].id))
        assertTrue(CarModeLogic.root(true, 2, 1).none { it.playable })

        val q = CarModeLogic.quran(setOf(112, 2))
        assertEquals(listOf(2, 112, 18, 36), q.take(4).map { (CarModeLogic.parse(it.id) as CarTarget.Surah).surah })
        assertEquals("Saved · plays offline", q[0].subtitle)
        assertTrue(q.size <= CarModeLogic.MAX_ITEMS && q.all { it.playable })
        assertEquals(q.size, q.map { it.id }.distinct().size)

        val src = CarModeLogic.ayahSources("https://everyayah.com/data/Husary_128kbps/", 112) { a -> if (a == 2) "file:///x/112002.mp3" else null }
        assertEquals(4, src.size)
        assertEquals("https://everyayah.com/data/Husary_128kbps/112001.mp3", src[0])
        assertEquals("file:///x/112002.mp3", src[1])

        val p = PodcastRepository.podcasts.first { it.id == "yaqeen" }
        val eps = listOf(Episode("g1", "One", "u1", 0, 3_600_000, null), Episode("g2", "Two", "u2", 0, 1_200_000, null))
        val tid = BayanLogic.trackId(p, eps[0])
        val nodes = CarModeLogic.episodes(p, eps, mapOf(tid to ResumePoint(tid, p.id, "One", "u1", 600_000, 3_600_000, 1)))
        assertEquals("50 min left", nodes[0].subtitle); assertEquals("20 min", nodes[1].subtitle)
        assertEquals(CarTarget.Episode("yaqeen", tid), CarModeLogic.parse(nodes[0].id))
        assertEquals(listOf("car_show:yaqeen"), CarModeLogic.shows(listOf(p, PodcastRepository.podcasts.first { it.id == "nak" }), mapOf("yaqeen" to 4)).map { it.id })
        assertEquals("Good morning", CarModeLogic.greeting(7)); assertEquals("Safe journey", CarModeLogic.greeting(23))
    }
}
