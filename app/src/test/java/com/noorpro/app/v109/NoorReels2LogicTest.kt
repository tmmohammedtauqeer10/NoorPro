package com.noorpro.app.v109

import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.ui.screens.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

internal fun post(
    id: String, caption: String = "", type: String = "reel", uid: String = "c1", w: Int = 0, h: Int = 0, rot: Int = 0,
    category: String = "Reminder", loc: String = "", at: Long = 1_000L, arabic: String = "", likes: Long = 0,
    pinned: Boolean = false, archived: Boolean = false,
) = UmmahPost(
    id = id, type = type, mediaUrl = "https://x/$id.mp4", thumbnailUrl = "", creatorUid = uid, creatorName = "Creator $uid",
    creatorHandle = "@$uid", caption = caption, arabicText = arabic, sourceReference = "", publishedAt = at, category = category,
    likeCount = likes, mediaWidth = w, mediaHeight = h, mediaRotationDegrees = rot, locationName = loc,
    pinnedToProfile = pinned, archived = archived,
)

class NoorReels2LogicTest {
    @After fun reset() { Reels2Flags.override = null }

    // ---- aspect detection: the 3 framing cases + rotation
    @Test fun portrait916IsFullBleed() {
        val f = reelFrameFor(1080, 1920)
        assertEquals(ReelFrameMode.FULL_BLEED, f.mode); assertEquals("9:16", f.label); assertFalse(f.showFullScreenPill)
    }
    @Test fun square11And45AreBlurFitWithoutPill() {
        val sq = reelFrameFor(1080, 1080); val p45 = reelFrameFor(1080, 1350)
        assertEquals(ReelFrameMode.FIT_BLUR, sq.mode); assertEquals("1:1", sq.label); assertFalse(sq.showFullScreenPill)
        assertEquals(ReelFrameMode.FIT_BLUR, p45.mode); assertEquals("4:5", p45.label); assertFalse(p45.showFullScreenPill)
    }
    @Test fun landscape169IsBlurFitWithFullScreenPill() {
        val f = reelFrameFor(1920, 1080)
        assertEquals(ReelFrameMode.FIT_BLUR, f.mode); assertEquals("16:9", f.label); assertTrue(f.showFullScreenPill)
    }
    @Test fun rotatedPortraitPhoneVideoStaysFullBleed() {
        assertEquals(ReelFrameMode.FULL_BLEED, reelFrameFor(1920, 1080, 90).mode)
    }
    @Test fun unknownGeometryFallsBackToFullBleed() {
        assertEquals(ReelFrameMode.FULL_BLEED, reelFrameFor(0, 0).mode)
        assertEquals(ReelFrameMode.FULL_BLEED, reelFrameForAspect(Float.NaN).mode)
    }
    @Test fun fittedHeightNeverCropsAndCapsAt70Percent() {
        assertEquals(411f / (16f / 9f), fittedReelHeightDp(411f, 891f, 16f / 9f), 0.5f)
        assertEquals(891f * 0.7f, fittedReelHeightDp(411f, 891f, 0.6f), 0.5f)
        assertEquals(548f, fittedReelHeightDp(411f, 891f, 0.75f), 0.5f)
        assertEquals(0f, fittedReelHeightDp(411f, 891f, 0f), 0f)
    }

    // ---- tabs/topics/nearby
    @Test fun feedTabsAndTopics() {
        val a = post("a", "Surah Mulk recitation", uid = "f", loc = "Hyderabad, India", at = 3)
        val b = post("b", "Beautiful naat", uid = "x", loc = "Lahore", at = 2)
        val c = post("c", "Kids qaida lesson", uid = "f", loc = "Hyderabad", at = 1)
        val all = listOf(a, b, c)
        assertEquals(listOf("a", "c"), reels2Feed(Reels2Tab.FOLLOWING, "All", all, setOf("f"), "").map { it.id })
        assertEquals(listOf("a", "c"), reels2Feed(Reels2Tab.NEARBY, "All", all, emptySet(), "hyderabad").map { it.id })
        assertEquals(listOf("b"), reels2Feed(Reels2Tab.FOR_YOU, "Naat", all, emptySet(), "").map { it.id })
        assertEquals(listOf("c"), reels2Feed(Reels2Tab.FOR_YOU, "Kids", all, emptySet(), "").map { it.id })
        assertEquals(listOf("b", "c"), reels2Feed(Reels2Tab.FOR_YOU, "All", all, emptySet(), "", hidden = setOf("a")).map { it.id })
        assertTrue(reels2Feed(Reels2Tab.NEARBY, "All", all, emptySet(), "").isEmpty())
    }

    // ---- chapters
    @Test fun chaptersParseAndStripFromCaption() {
        val cap = "Sabr in hardship\n0:00 Intro\n1:30 - The ayah\n12:05 | Story of Ayyub"
        val ch = parseReelChapters(cap)
        assertEquals(listOf(0L, 90_000L, 725_000L), ch.map { it.startMs })
        assertEquals("The ayah", ch[1].title)
        assertEquals(1, chapterIndexAt(100_000, ch)); assertEquals(0, chapterIndexAt(0, ch)); assertEquals(2, chapterIndexAt(9_999_999, ch))
        assertEquals("Sabr in hardship", captionWithoutChapters(cap))
    }
    @Test fun chaptersNeedZeroStartAndIncreasing() {
        assertTrue(parseReelChapters("0:10 a\n0:20 b").isEmpty())
        assertTrue(parseReelChapters("0:00 a\n0:00 b").isEmpty())
        assertTrue(parseReelChapters("0:00 only one").isEmpty())
        assertTrue(parseReelChapters("0:00 a\n0:75 bad").isEmpty())
    }

    // ---- series
    @Test fun seriesParsingAndNextPart() {
        assertEquals(ReelSeriesInfo("Seerah for kids", 2, 5), parseReelSeries("Seerah for kids — Part 2/5\nmore"))
        assertEquals(ReelSeriesInfo("Sabr series", 3, null), parseReelSeries("Sabr series pt 3"))
        assertEquals(ReelSeriesInfo("Tafsir", 4, 10), parseReelSeries("Tafsir Ep. 4 of 10"))
        assertNull(parseReelSeries("Part 2")); assertNull(parseReelSeries("Seerah part 6/5")); assertNull(parseReelSeries("Just a reminder"))
        val p1 = post("1", "Seerah — Part 1/3"); val p2 = post("2", "Seerah — Part 2/3"); val other = post("3", "Seerah — Part 2/3", uid = "z")
        assertEquals("2", nextSeriesPart(p1, listOf(other, p1, p2))?.id)
        assertNull(nextSeriesPart(p2, listOf(p1, p2)))
    }

    // ---- recitation
    @Test fun recitationModeAndAyahTiming() {
        val r = post("r", "Quran recitation", arabic = "بِسْمِ اللَّهِ ۝ الْحَمْدُ لِلَّهِ ۝ الرَّحْمَٰنِ الرَّحِيمِ")
        assertTrue(isRecitationReel(r)); assertFalse(isRecitationReel(post("n", "naat", arabic = "x")))
        assertEquals(3, splitAyahs(r.arabicText).size)
        assertEquals(0, ayahIndexAt(0, 30_000, 3)); assertEquals(1, ayahIndexAt(15_000, 30_000, 3)); assertEquals(2, ayahIndexAt(99_000, 30_000, 3))
        assertEquals(-1, ayahIndexAt(0, 0, 0)); assertEquals(0, ayahIndexAt(5, 0, 3))
    }

    // ---- comments
    @Test fun commentsPinnedThenCreatorLikedThenTopThenNew() {
        fun c(id: String, likes: Long, at: Long, parent: String = "", status: String = "published") = UmmahComment(id, "p", id, "u", "U", at, parent, "", likes, status)
        val list = listOf(c("old", 0, 1), c("top", 9, 2), c("new", 0, 5), c("pin", 0, 0), c("cl", 1, 0), c("reply", 99, 9, parent = "top"), c("hid", 50, 9, status = "hidden"))
        val ordered = orderReelComments(list, "pin", setOf("cl"))
        assertEquals(listOf("pin", "cl", "top", "new", "old"), ordered.map { it.comment.id })
        assertTrue(ordered[0].pinned); assertTrue(ordered[1].creatorLiked)
    }

    // ---- reactions, speed, clock, counts
    @Test fun reactionsSpeedClockCounts() {
        assertTrue(reactionCountsAsLike(HalalReaction.AMEEN)); assertFalse(reactionCountsAsLike(null))
        assertEquals(listOf(HalalReaction.AMEEN, HalalReaction.LIKE), topReactions(mapOf(HalalReaction.LIKE to 3L, HalalReaction.AMEEN to 9L, HalalReaction.LOVE to 0L)))
        assertEquals(1.25f, nextReelSpeed(1f)); assertEquals(0.5f, nextReelSpeed(2f)); assertEquals(1f, nextReelSpeed(3.3f))
        assertEquals("1x", speedLabel(1f)); assertEquals("1.5x", speedLabel(1.5f))
        assertEquals("1:05", reelClock(65_000)); assertEquals("1:01:01", reelClock(3_661_000)); assertEquals("0:00", reelClock(-5))
        assertEquals("999", reelCount(999)); assertEquals("1.2K", reelCount(1_200)); assertEquals("12K", reelCount(12_345)); assertEquals("2M", reelCount(2_000_000))
    }

    // ---- why / continue watching / smart search
    @Test fun whyThisReelReasons() {
        val p = post("p", "Surah Kahf tilawat", uid = "f", loc = "Hyderabad")
        val why = whyThisReel(p, setOf("f"), setOf("Quran"), "Hyderabad")
        assertEquals(3, why.size); assertTrue(why[0].startsWith("You follow"))
        assertEquals(1, whyThisReel(post("q"), emptySet(), emptySet(), "").size)
    }
    @Test fun continueWatchingFiltersAndOrders() {
        val posts = listOf(post("a"), post("b"), post("c"), post("d"))
        val prog = listOf(
            WatchProgress("a", 30_000, 120_000, 10), WatchProgress("b", 1_000, 120_000, 20), // b < 5%
            WatchProgress("c", 115_000, 120_000, 30), WatchProgress("d", 20_000, 40_000, 40), // c > 90%, d short
            WatchProgress("gone", 30_000, 120_000, 50),
        )
        assertEquals(listOf("a"), continueWatching(prog, posts).map { it.first.id })
    }
    @Test fun smartSearchParsesTopicAndLanguage() {
        val q = parseSmartReelQuery("find reels about patience in Urdu")
        assertEquals("sabr", q.topic); assertEquals("ur", q.language)
        val posts = listOf(post("1", "Patience in hardship"), post("2", "Sabr ka bayan"), post("3", "Naat"))
        assertEquals(listOf("1", "2"), smartReelMatches(q, posts).map { it.id })
        assertNull(parseSmartReelQuery("salah").language)
        assertTrue(smartReelMatches(parseSmartReelQuery("show me"), posts).isEmpty())
    }

    // ---- AI framing
    @Test fun aiPromptAndSections() {
        val prompt = Reels2AiPrompts.about(post("p", "Sabr reminder"))
        assertTrue(prompt.contains("SUMMARY") && prompt.contains("REFERENCE") && prompt.contains("Do not invent"))
        val ans = "SUMMARY: Be patient.\nREFERENCE: Quran 2:153\nSIMPLE: Wait calmly.\nFOLLOWUPS: x"
        assertEquals("Be patient.", Reels2AiPrompts.section(ans, "SUMMARY"))
        assertEquals("Quran 2:153", Reels2AiPrompts.section(ans, "REFERENCE"))
        assertEquals("Wait calmly.", Reels2AiPrompts.section(ans, "SIMPLE"))
        assertEquals(listOf("EN", "UR", "HI", "AR"), reels2CaptionLanguages)
    }

    // ---- flags
    @Test fun flagDefaultsKillSwitchesOnBackendOff() {
        assertTrue(Reels2Flags.default(Reels2Flags.REELS_V2)); assertTrue(Reels2Flags.default(Reels2Flags.PROFILE_V2))
        assertTrue(Reels2Flags.default(Reels2Flags.SOUND_PAGES)) // 1.1.9 sound pages on
        listOf(Reels2Flags.SERIES, Reels2Flags.VOICE_COMMENTS, Reels2Flags.REACTIONS, Reels2Flags.CREATOR_LIKED,
            Reels2Flags.LIVE, Reels2Flags.MINI_PLAYER, Reels2Flags.AI_TRANSCRIPTS, Reels2Flags.DEEN_CARD, Reels2Flags.PROFILE_FIELDS,
            Reels2Flags.HIGHLIGHTS, Reels2Flags.CLOSE_FRIENDS, Reels2Flags.AI_RECAP).forEach { assertFalse(it, Reels2Flags.default(it)) }
        assertTrue(Reels2Flags.isOn(Reels2Flags.REELS_V2))
        Reels2Flags.override = mapOf(Reels2Flags.REELS_V2 to false, Reels2Flags.SERIES to true)
        assertFalse(Reels2Flags.isOn(Reels2Flags.REELS_V2)); assertTrue(Reels2Flags.isOn(Reels2Flags.SERIES))
        assertFalse(Reels2Features().live)
    }
}
