package com.noorpro.app.audio.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class Mp3QuranCatalogTest {

    private val sampleJson = """
    {
      "reciters": [
        {
          "id": 123,
          "name": "Mishary Alafasi",
          "moshaf": [
            {
              "id": 123,
              "name": "Rewayat Hafs A'n Assem - Murattal",
              "server": "https://cdn.mp3quran.net/audio/mishary-alafasy/r1",
              "surah_total": 114,
              "surah_list": "1,2,3,4,5"
            },
            {
              "id": 124,
              "name": "Other",
              "server": "https://cdn.mp3quran.net/audio/mishary-alafasy/r2/",
              "surah_total": 6,
              "surah_list": "12,14"
            }
          ]
        },
        {
          "id": 51,
          "name": "Abdulbasit Abdulsamad",
          "moshaf": [
            {
              "id": 53,
              "name": "Murattal",
              "server": "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/",
              "surah_total": 114,
              "surah_list": "1,2,3"
            },
            {
              "id": 51,
              "name": "Mojawwad",
              "server": "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r1/",
              "surah_total": 114,
              "surah_list": "1,2"
            }
          ]
        }
      ]
    }
    """.trimIndent()

    @Test fun audioUrlPadsSurah() {
        assertEquals(
            "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/001.mp3",
            Mp3QuranCatalog.audioUrl("https://cdn.mp3quran.net/audio/mishary-alafasy/r1/", 1),
        )
        assertEquals(
            "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/114.mp3",
            Mp3QuranCatalog.audioUrl("https://cdn.mp3quran.net/audio/mishary-alafasy/r1", 114),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun audioUrlRejectsOutOfRange() {
        Mp3QuranCatalog.audioUrl("https://cdn.example/", 0)
    }

    @Test fun parseSurahListFiltersAndDedups() {
        assertEquals(listOf(1, 2, 114), Mp3QuranCatalog.parseSurahList("1, 2, 2, 999, 114, x"))
        assertTrue(Mp3QuranCatalog.parseSurahList("").isEmpty())
    }

    @Test fun parseRecitersJsonReadsCuratedFields() {
        val list = Mp3QuranCatalog.parseRecitersJson(sampleJson)
        assertEquals(2, list.size)
        val mishary = list.first { it.id == 123 }
        assertEquals("Mishary Alafasi", mishary.name)
        assertEquals(2, mishary.moshafs.size)
        val m = mishary.moshafs.first { it.id == 123 }
        assertTrue(m.server.endsWith("/"))
        assertEquals(listOf(1, 2, 3, 4, 5), m.surahList)
    }

    @Test fun parseRecitersJsonIgnoresNonHttps() {
        val bad = """{"reciters":[{"id":1,"name":"X","moshaf":[{"id":1,"name":"n","server":"http://insecure/","surah_total":1,"surah_list":"1"}]}]}"""
        assertTrue(Mp3QuranCatalog.parseRecitersJson(bad).first().moshafs.isEmpty())
    }

    @Test fun resolveUsesApiWhenPresentElseFallback() {
        val api = Mp3QuranCatalog.parseRecitersJson(sampleJson)
        val resolved = Mp3QuranCatalog.resolve(api)
        assertEquals(Mp3QuranCatalog.CURATED.size, resolved.size)
        val mishary = resolved.first { it.curated.key == "mishary-murattal" }
        assertTrue(mishary.fromApi)
        assertEquals(listOf(1, 2, 3, 4, 5), mishary.surahs)
        assertTrue(mishary.server.contains("mishary-alafasy"))
        val sudais = resolved.first { it.curated.key == "sudais-murattal" }
        assertFalse(sudais.fromApi)
        assertEquals(114, sudais.surahs.size)
        assertEquals(sudais.curated.fallbackServer, sudais.server)
    }

    @Test fun fallbackResolvedAlways114() {
        val fb = Mp3QuranCatalog.fallbackResolved()
        assertEquals(Mp3QuranCatalog.CURATED.size, fb.size)
        assertTrue(fb.all { !it.fromApi && it.surahs.size == 114 })
    }

    @Test fun tracksForBuildsMp3qIdsAndCredit() {
        val r = Mp3QuranCatalog.fallbackResolved().first { it.curated.key == "mishary-murattal" }
        val tracks = Mp3QuranCatalog.tracksFor(r.copy(surahs = listOf(1, 2)))
        assertEquals(2, tracks.size)
        assertEquals("mp3q-mishary-murattal-1", tracks[0].id)
        assertTrue(tracks[0].audioUrl.endsWith("001.mp3"))
        assertTrue(tracks[0].attributionText.contains(Mp3QuranCatalog.CREDIT))
        assertEquals(Mp3QuranCatalog.SOURCE_URL, tracks[0].sourceUrl)
    }

    @Test fun findResolvedByKey() {
        val list = Mp3QuranCatalog.fallbackResolved()
        assertNotNull(Mp3QuranCatalog.findResolved(list, "dosari-murattal"))
        assertNull(Mp3QuranCatalog.findResolved(list, "nope"))
    }

    @Test fun cacheFreshnessAndRoundTrip() {
        val dir = createTempDir(prefix = "mp3q-test")
        try {
            val f = File(dir, "cache.json")
            assertFalse(Mp3QuranCatalog.isCacheFresh(f, nowMs = 1_000_000L))
            Mp3QuranCatalog.writeCacheText(f, sampleJson)
            assertEquals(sampleJson, Mp3QuranCatalog.readCacheText(f))
            assertTrue(Mp3QuranCatalog.isCacheFresh(f, nowMs = f.lastModified() + 1_000L, ttlMs = 10_000L))
            assertFalse(Mp3QuranCatalog.isCacheFresh(f, nowMs = f.lastModified() + 20_000L, ttlMs = 10_000L))
            val parsed = Mp3QuranCatalog.loadCachedReciters(f)
            assertNotNull(parsed)
            assertEquals(2, parsed!!.size)
            val via = Mp3QuranCatalog.resolvedFromCacheOrFallback(f, nowMs = f.lastModified() + 100L)
            assertTrue(via.first { it.curated.key == "mishary-murattal" }.fromApi)
            // Stale cache → fallback (no API in unit test)
            val stale = Mp3QuranCatalog.resolvedFromCacheOrFallback(f, nowMs = f.lastModified() + Mp3QuranCatalog.CACHE_TTL_MS + 1)
            assertFalse(stale.first { it.curated.key == "mishary-murattal" }.fromApi)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun curatedContainsRequestedReciters() {
        val names = Mp3QuranCatalog.CURATED.map { it.displayName }.toSet()
        assertTrue(names.any { it.contains("Alafasy") || it.contains("Alafasi") })
        assertTrue(names.any { it.contains("Basit") })
        assertTrue(names.any { it.contains("Sudais") })
        assertTrue(names.any { it.contains("Shuraim") })
        assertTrue(names.any { it.contains("Ghamdi") })
        assertTrue(names.any { it.contains("Muaiqly") || it.contains("Meaqli") })
        assertTrue(names.any { it.contains("Minshawi") })
        assertTrue(names.any { it.contains("Husary") })
        assertTrue(names.any { it.contains("Dosari") })
        assertTrue(Mp3QuranCatalog.CURATED.any { it.style == "Mujawwad" && it.displayName.contains("Basit") })
        assertTrue(Mp3QuranCatalog.CURATED.any { it.style == "Murattal" && it.displayName.contains("Basit") })
    }

    @Test fun jsonLiteHandlesEscapesAndNumbers() {
        val v = JsonLite.parse("""{"a":"x\"y","b":12,"c":true,"d":null,"e":[1,2]}""") as Map<*, *>
        assertEquals("x\"y", v["a"])
        assertEquals(12, v["b"])
        assertEquals(true, v["c"])
        assertNull(v["d"])
        assertEquals(listOf(1, 2), v["e"])
    }
}
