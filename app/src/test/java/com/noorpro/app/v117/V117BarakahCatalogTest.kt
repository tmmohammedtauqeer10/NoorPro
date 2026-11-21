package com.noorpro.app.v117

import com.noorpro.app.audio.catalog.BarakahMelodies
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 1.2.0 (117): Barakah Melodies track list from Remote Config JSON (org.json needs Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class V117BarakahCatalogTest {
    @Test fun barakahRemoteCatalogueAndRemoval() {
        val day = 86_400_000L; val t0 = 1_800_000_000_000L
        BarakahMelodies.now = { t0 }
        try {
            val json = """{"version":2,"tracks":[
                {"key":"zikr","title":"Zikr of the Heart","url":"https://firebasestorage.googleapis.com/v0/b/noor/o/bm%2Fzikr.m4a","durationMs":215000},
                {"key":"rip","title":"Ripped","url":"https://p.scdn.co/mp3-preview/x"},
                {"key":"gone","title":"Removed one","url":"https://cdn.noorpro.app/bm/gone.m4a"},
                {"key":"","title":"No key","url":"https://cdn.noorpro.app/bm/x.m4a"}],"removed":["gone"]}"""
            val c = BarakahMelodies.parse(json, t0)!!
            BarakahMelodies.override = c
            assertEquals(listOf("zikr"), BarakahMelodies.entries.map { it.key }) // Spotify rip, removed key and bad rows dropped
            val zikr = BarakahMelodies.tracks.single()
            assertTrue(BarakahMelodies.isPlayable(zikr))
            val gone = BarakahMelodies.toTrack(BarakahMelodies.Entry("gone", "Removed one", "https://cdn.noorpro.app/bm/gone.m4a"))
            assertFalse(BarakahMelodies.isPlayable(gone)) // removed remotely → never plays, even from a saved queue
            // Not refreshed for more than 7 days → hidden (removal guaranteed within 7 days).
            BarakahMelodies.now = { t0 + 8 * day }
            assertTrue(BarakahMelodies.entries.isEmpty()); assertFalse(BarakahMelodies.isPlayable(zikr))
            BarakahMelodies.now = { t0 + 6 * day }
            assertEquals(1, BarakahMelodies.entries.size)
            assertNull(BarakahMelodies.parse("not json", t0)); assertNull(BarakahMelodies.parse("", t0))
        } finally { BarakahMelodies.override = null; BarakahMelodies.now = { System.currentTimeMillis() } }
    }
}
