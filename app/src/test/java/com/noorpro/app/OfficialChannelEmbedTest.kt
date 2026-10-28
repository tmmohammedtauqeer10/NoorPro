package com.noorpro.app

import com.noorpro.app.audio.catalog.OfficialChannelEmbed
import com.noorpro.app.audio.catalog.OfficialChannelFlags
import com.noorpro.app.audio.catalog.OfficialChannels
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class OfficialChannelEmbedTest {
    private val ch = OfficialChannels.all.first { it.id == "ajmal-raza-qadri" }

    @After fun reset() { OfficialChannelFlags.override = null }

    @Test fun flagDefaultsOff() {
        assertEquals("official_channel_embed_enabled", OfficialChannelFlags.EMBED)
        assertFalse(OfficialChannelFlags.default(OfficialChannelFlags.EMBED))
        assertFalse(OfficialChannelFlags.embedEnabled)
        OfficialChannelFlags.override = mapOf(OfficialChannelFlags.EMBED to true)
        assertTrue(OfficialChannelFlags.embedEnabled)
    }

    @Test fun channelIdAndUrls() {
        assertEquals("UCxhWp1FeSSx6qbb2f4ohoBg", ch.channelId)
        assertTrue(OfficialChannelEmbed.isChannelId(ch.channelId))
        assertEquals("https://www.youtube.com/feeds/videos.xml?channel_id=UCxhWp1FeSSx6qbb2f4ohoBg", OfficialChannelEmbed.rssUrl(ch.channelId))
        assertEquals("https://www.youtube.com/channel/UCxhWp1FeSSx6qbb2f4ohoBg?sub_confirmation=1", OfficialChannelEmbed.subscribeUrl(ch))
        assertEquals("https://www.youtube.com/channel/UCxhWp1FeSSx6qbb2f4ohoBg", OfficialChannelEmbed.channelUrl(ch))
        // the 1.0.93 link-out is unchanged
        assertEquals("https://www.youtube.com/@MuhammdAjmalRazaQadriOfficial", ch.url)
        assertNull(OfficialChannelEmbed.rssUrl("not-a-channel"))
        assertNull(OfficialChannelEmbed.rssUrl(""))
    }

    @Test fun embedUrlOnlyForValidIds() {
        assertEquals("https://www.youtube.com/embed/gL4_n4MmImQ?playsinline=1&rel=0", OfficialChannelEmbed.embedUrl("gL4_n4MmImQ"))
        assertNull(OfficialChannelEmbed.embedUrl("bad'id);alert(1)"))
        assertNull(OfficialChannelEmbed.embedUrl("short"))
        assertThrows(IllegalArgumentException::class.java) { OfficialChannelEmbed.playerHtml("x');evil();//") }
    }

    @Test fun playerIsTheOfficialUnmodifiedPlayer() {
        val h = OfficialChannelEmbed.playerHtml("gL4_n4MmImQ")
        assertTrue(h.contains("https://www.youtube.com/iframe_api"))
        assertTrue(h.contains("videoId:'gL4_n4MmImQ'"))
        assertTrue(h.contains("controls:1"))
        assertTrue(h.contains("onError"))
        // no hiding/branding removal/audio-only tricks
        listOf("controls:0", "modestbranding", "display:none", "visibility:hidden", "opacity:0", "mute", "googlevideo", "iv_load_policy").forEach {
            assertFalse("player must not use $it", h.contains(it))
        }
    }

    @Test fun anyPlayerErrorFallsBackToYouTube() {
        listOf(2, 5, 100, 101, 150, 152, 153).forEach { assertTrue(OfficialChannelEmbed.shouldFallBack(it)) }
        assertTrue(OfficialChannelEmbed.errorMessage(150).contains("doesn't allow"))
        assertTrue(OfficialChannelEmbed.errorMessage(101).contains("doesn't allow"))
        assertTrue(OfficialChannelEmbed.errorMessage(100).contains("private"))
    }

    private val rss = """<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/" xmlns="http://www.w3.org/2005/Atom">
 <title>Muhammad Ajmal Raza Qadri</title>
 <entry>
  <id>yt:video:AGdiNcLIRrg</id><yt:videoId>AGdiNcLIRrg</yt:videoId><yt:channelId>UCxhWp1FeSSx6qbb2f4ohoBg</yt:channelId>
  <title>Tune Har Shakhs Ki Qismat Mein Izzat Liki | Complete Lecture</title>
  <published>2026-10-20T15:00:00+00:00</published>
  <media:group><media:title>x</media:title><media:community><media:statistics views="1234"/></media:community></media:group>
 </entry>
 <entry>
  <id>yt:video:gL4_n4MmImQ</id><yt:videoId>gL4_n4MmImQ</yt:videoId>
  <title>Adab Pehla Qareena Hai &amp; Mohabbat &quot;Ke&quot; Qareeno Mein</title>
  <published>2026-10-25T10:30:00+00:00</published>
  <media:group><media:community><media:statistics views="25600"/></media:community></media:group>
 </entry>
 <entry><yt:videoId>gL4_n4MmImQ</yt:videoId><title>dup</title><published>2026-10-25T10:30:00+00:00</published></entry>
 <entry><yt:videoId>bad</yt:videoId><title>invalid id</title></entry>
</feed>"""

    @Test fun parsesPublicRssNewestFirst() {
        val v = OfficialChannelEmbed.parseRss(rss)
        assertEquals(listOf("gL4_n4MmImQ", "AGdiNcLIRrg"), v.map { it.id })
        assertEquals("Adab Pehla Qareena Hai & Mohabbat \"Ke\" Qareeno Mein", v[0].title)
        assertEquals("25 Oct 2026", v[0].dateLabel)
        assertEquals("25.6K views", v[0].viewsLabel)
        assertEquals("25 Oct 2026 · 25.6K views", v[0].meta)
        assertEquals("1.2K views", v[1].viewsLabel)
        assertEquals("https://i.ytimg.com/vi/gL4_n4MmImQ/hqdefault.jpg", v[0].thumb)
        assertEquals("https://www.youtube.com/watch?v=gL4_n4MmImQ", v[0].watchUrl)
        assertTrue(OfficialChannelEmbed.parseRss("<html>404</html>").isEmpty())
    }
}
