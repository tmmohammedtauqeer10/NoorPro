package com.noorpro.app.v108

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.ComposerLaunch
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.data.UmmahStory
import com.noorpro.app.data.creator.*
import com.noorpro.app.ui.screens.insertAiText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

internal const val DAY = 86_400_000L
internal val NOW = 1_761_400_000_000L // fixed clock

internal fun post(id: String, daysAgo: Double, views: Long, likes: Long = 0, comments: Long = 0, type: String = "image", hour: Int? = null,
                  caption: String = "Post $id", pinned: Boolean = false, archived: Boolean = false, shares: Long = 0, saves: Long = 0): UmmahPost {
    var t = NOW - (daysAgo * DAY).toLong()
    if (hour != null) {
        val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = t; set(java.util.Calendar.HOUR_OF_DAY, hour); set(java.util.Calendar.MINUTE, 10) }
        t = c.timeInMillis
    }
    return UmmahPost(id, type, "https://x/$id.jpg", thumbnailUrl = "", creatorUid = "me", creatorName = "Me", creatorHandle = "@me", caption = caption,
        sourceReference = "", publishedAt = t, likeCount = likes, commentCount = comments, viewCount = views, shareCount = shares, saveCount = saves,
        pinnedToProfile = pinned, archived = archived)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CreatorStudioLogicTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()

    // ---------------------------------------------------------------- insights

    @Test fun insightsWindowTotalsAndChange() {
        val posts = listOf(
            post("a", 1.0, 100, likes = 10, comments = 2, shares = 1, saves = 2),
            post("b", 5.0, 300, likes = 30, type = "reel"),
            post("c", 20.0, 50),              // outside 7d, inside 28d
            post("d", 9.0, 80),               // previous 7-day window
        )
        val ins = CreatorInsightsCalc.compute(posts, listOf(NOW - DAY, NOW - 3 * DAY, NOW - 40 * DAY), InsightRange.D7, NOW, zone = utc)
        assertEquals(1, ins.posts); assertEquals(1, ins.reels)
        assertEquals(400, ins.views); assertEquals(300, ins.uniqueReelViewers)
        assertEquals(40, ins.likes); assertEquals(2, ins.comments); assertEquals(1, ins.shares); assertEquals(2, ins.saves)
        assertEquals(45.0 / 400, ins.engagementRate, 1e-9)
        assertEquals(80, ins.previousViews); assertEquals(400, ins.viewsChangePct) // (400-80)/80
        assertEquals(3, ins.followers); assertEquals(2, ins.followerGain)
        assertEquals(7, ins.viewsSeries.size); assertEquals(400, ins.viewsSeries.sumOf { it.value })
        assertEquals("b", ins.topReels.single().id); assertEquals("a", ins.topPosts.single().id)

        val d28 = CreatorInsightsCalc.compute(posts, emptyList(), InsightRange.D28, NOW, zone = utc)
        assertEquals(530, d28.views); assertEquals(4, d28.viewsSeries.size); assertEquals("W4", d28.viewsSeries.last().label)
        assertNull(d28.viewsChangePct) // nothing before → no ▲/▼
        assertEquals(13, CreatorInsightsCalc.compute(posts, emptyList(), InsightRange.D90, NOW, zone = utc).viewsSeries.size)
    }

    @Test fun emptyAccountIsSafe() {
        val ins = CreatorInsightsCalc.compute(emptyList(), emptyList(), InsightRange.D28, NOW, zone = utc)
        assertEquals(0, ins.views); assertEquals(0.0, ins.engagementRate, 0.0); assertNull(ins.viewsChangePct)
        assertFalse(ins.bestFromHistory); assertTrue(ins.bestSlots.isNotEmpty())
    }

    @Test fun followerSeriesBucketsByDay() {
        val s = CreatorInsightsCalc.followerSeries(listOf(NOW - 10_000, NOW - 20_000, NOW - 2 * DAY), InsightRange.D7, NOW, utc)
        assertEquals(7, s.size); assertEquals(2, s.last().value); assertEquals(3, s.sumOf { it.value })
    }

    @Test fun bestTimesFromHistoryAndPrayerContext() {
        val prayer = mapOf("Fajr" to "05:10", "Sunrise" to "06:25", "Dhuhr" to "12:20", "Asr" to "15:45", "Maghrib" to "18:05", "Isha" to "19:20")
        val posts = (1..3).map { post("m$it", it.toDouble(), 500, likes = 50, hour = 19) } + (1..3).map { post("n$it", it + 3.0, 50, hour = 8) } +
            listOf(post("x", 2.0, 9999, hour = 14)) // single post: below the 3-post threshold
        val (slots, history) = CreatorInsightsCalc.bestTimes(posts, prayer, utc)
        assertTrue(history)
        assertEquals(18, slots.first().hour)               // 18–20 band wins
        assertEquals("after Maghrib", slots.first().prayerNote)
        assertEquals(2, slots.size)
        assertEquals("morning, after Fajr", CreatorInsightsCalc.prayerNote(8, prayer))
        assertEquals("after Isha", CreatorInsightsCalc.prayerNote(21, prayer))
        assertEquals("after Maghrib", CreatorInsightsCalc.prayerNote(18, emptyMap())) // heuristic
        val (general, fromHistory) = CreatorInsightsCalc.bestTimes(emptyList(), prayer, utc)
        assertFalse(fromHistory); assertEquals(listOf(18, 20, 6), general.map { it.hour })
        assertEquals("6–8 PM", CreatorInsightsCalc.hourLabel(18)); assertEquals("10 AM–12 PM", CreatorInsightsCalc.hourLabel(10))
    }

    @Test fun dailyDeltasNeverNegative() {
        val d = CreatorInsightsCalc.dailyDeltas(listOf("20261020" to 100L, "20261021" to 160L, "20261022" to 150L))
        assertEquals(listOf(60L, 0L), d.map { it.value }); assertEquals("20261021", d.first().label)
    }

    // ---------------------------------------------------------------- content manager

    @Test fun contentFilterSortPinArchive() {
        val items = listOf(
            post("p1", 1.0, 10, caption = "Sabr reminder"), post("p2", 3.0, 900, likes = 1, caption = "Dua for parents", pinned = true),
            post("p3", 2.0, 50, likes = 80, caption = "Old one", archived = true), post("r1", 1.0, 40, type = "reel", caption = "Reel"),
        ).map(StudioContent::of) +
            StudioContent.of(UmmahStory("s1", "me", "Me", "@me", "", "https://x/s.jpg", "", "", NOW - 3_600_000, NOW + 20 * 3_600_000)) +
            StudioContent.of(ComposerDraft("d1", ComposerDestination.FEED, "Draft about tawakkul", "Reminder", updatedAt = NOW))
        assertEquals(listOf("p2", "p1"), StudioContent.filter(items, StudioKind.POST, StudioSort.NEWEST, "", false).map { it.id }) // pinned first
        assertEquals(listOf("p2", "p1"), StudioContent.filter(items, StudioKind.POST, StudioSort.MOST_VIEWED, "", false).map { it.id })
        assertEquals(listOf("p3"), StudioContent.filter(items, StudioKind.POST, StudioSort.NEWEST, "", true).map { it.id })
        assertEquals(listOf("p1"), StudioContent.filter(items, StudioKind.POST, StudioSort.NEWEST, "SABR", false).map { it.id })
        assertEquals(listOf("r1"), StudioContent.filter(items, StudioKind.REEL, StudioSort.NEWEST, "", false).map { it.id })
        assertEquals("Photo story", StudioContent.filter(items, StudioKind.STORY, StudioSort.NEWEST, "", true).single().title)
        assertEquals("Draft about tawakkul", StudioContent.filter(items, StudioKind.DRAFT, StudioSort.NEWEST, "", true).single().title)
    }

    // ---------------------------------------------------------------- comments

    private fun c(id: String, uid: String, text: String, post: String = "p1", parent: String = "", t: Long = NOW, status: String = "published") =
        UmmahComment(id, post, text, uid, uid.uppercase(), t, parentCommentId = parent, status = status)

    @Test fun inboxBuildAndFilters() {
        val comments = listOf(
            c("c1", "ali", "JazakAllah khair!", t = NOW - 1), c("c2", "spam", "Free c.r.y.p.t.o gains, DM me", t = NOW - 2),
            c("c3", "zaid", "Beautiful reminder", t = NOW - 3), c("r3", "me", "BarakAllahu feek", parent = "c3", t = NOW - 1),
            c("c4", "troll", "meh", t = NOW - 4), c("c5", "blocked", "hi", t = NOW - 5), c("c6", "omar", "ok", status = "hidden", t = NOW - 6),
        )
        val inbox = CommentInbox.build(comments, "me", mapOf("p1" to "Sabr"), hiddenIds = emptySet(), keywords = setOf("crypto"),
            restricted = setOf("troll"), blocked = setOf("blocked"), pinned = mapOf("p1" to "c1"))
        assertEquals(listOf("c1", "c2", "c3", "c4", "c6"), inbox.map { it.comment.id }) // mine + blocked excluded, newest first
        assertTrue(inbox.first { it.comment.id == "c3" }.replied)
        assertEquals("crypto", inbox.first { it.comment.id == "c2" }.keywordHit)
        assertTrue(inbox.first { it.comment.id == "c1" }.pinned)
        assertEquals(listOf("c1", "c3"), CommentInbox.filter(inbox, InboxFilter.ALL).map { it.comment.id })
        assertEquals(listOf("c1"), CommentInbox.filter(inbox, InboxFilter.UNREPLIED).map { it.comment.id })
        assertEquals(listOf("c2", "c4"), CommentInbox.filter(inbox, InboxFilter.FILTERED).map { it.comment.id })
        assertEquals(listOf("c6"), CommentInbox.filter(inbox, InboxFilter.HIDDEN).map { it.comment.id })
        assertEquals(listOf("c3"), CommentInbox.filter(inbox, InboxFilter.ALL, "zaid").map { it.comment.id })
    }

    @Test fun keywordMatchingBoundaries() {
        assertNull(CommentInbox.keywordHit("Assalamu alaikum", setOf("ass")))      // no substring false positive
        assertEquals("spam", CommentInbox.keywordHit("this is SPAM!", setOf("spam")))
        assertEquals("scam", CommentInbox.keywordHit("total s c a m here", setOf("scam")))
        assertNull(CommentInbox.keywordHit("anything", emptySet()))
    }

    @Test fun moderationStorePersists() {
        val s = CreatorModerationStore(ctx)
        s.setHidden("c1", true); s.addKeyword("  Crypto "); s.setRestricted("u9", true); s.setPin("p1", "c1"); s.setPin("p2", "c7")
        val again = CreatorModerationStore(ctx)
        assertEquals(setOf("c1"), again.hidden()); assertEquals(setOf("crypto"), again.keywords()); assertEquals(setOf("u9"), again.restricted())
        assertEquals(mapOf("p1" to "c1", "p2" to "c7"), again.pins())
        again.setPin("p1", null); again.removeKeyword("crypto"); again.setHidden("c1", false)
        assertEquals(mapOf("p2" to "c7"), again.pins()); assertTrue(again.keywords().isEmpty()); assertTrue(again.hidden().isEmpty())
    }

    // ---------------------------------------------------------------- AI helper

    @Test fun promptParsing() {
        val ans = """
            Here are some options:
            1. "Sabr is not silence — it's trusting Allah while you keep walking."
            2) **Every hardship is followed by ease** (94:5).
            - Small deeds, done daily, are beloved to Allah.
            Sources:
            - Al-Inshirah 94:5
            FOLLOWUPS: More? | Hashtags | Translate
        """.trimIndent()
        val list = CreatorAiPrompts.parseList(ans)
        assertEquals(3, list.size)
        assertEquals("Sabr is not silence — it's trusting Allah while you keep walking.", list[0])
        assertEquals("Every hardship is followed by ease (94:5).", list[1])
        assertEquals(listOf("#Sabr", "#tawakkul", "#صبر"), CreatorAiPrompts.parseHashtags("#Sabr #tawakkul... #Tawakkul #صبر #a\nFOLLOWUPS: #x"))
        assertEquals("مرحبا", CreatorAiPrompts.cleanTranslation("مرحبا\nFOLLOWUPS: a | b"))
        assertTrue(CreatorAiPrompts.translate("Hello", "ur").contains("Urdu"))
        assertTrue(CreatorAiPrompts.captions("", "Dua").contains("Topic: Dua"))
        val local = CreatorAiPrompts.localHashtags("A dua after salah", "Quran")
        assertTrue(local.containsAll(listOf("#Quran", "#Dua", "#Salah", "#NoorPro"))); assertTrue(local.size <= 8)
    }

    @Test fun referenceChecker() {
        val refs = ReferenceChecker.find("Read Surah Al-Baqarah 2:286 and Yusuf 12:200, also 115:1 and Al-Ikhlas 2:255. Bukhari 6018 · Sahih Muslim #2564")
        val quran = refs.filter { it.kind == RefCheck.Kind.QURAN }
        assertEquals(4, quran.size)
        assertTrue(quran[0].ok); assertEquals(2, quran[0].surah); assertEquals(286, quran[0].ayah)
        assertFalse(quran[1].ok); assertTrue(quran[1].message.contains("111 ayahs"))
        assertFalse(quran[2].ok); assertTrue(quran[2].message.contains("114"))
        assertFalse(quran[3].ok); assertTrue(quran[3].message.contains("surah 112"))
        val hadith = refs.filter { it.kind == RefCheck.Kind.HADITH }
        assertEquals(listOf("Sahih al-Bukhari" to "6018", "Sahih Muslim" to "2564"), hadith.map { it.collection to it.number })
        assertEquals(2, ReferenceChecker.surahByName("Baqara")); assertEquals(112, ReferenceChecker.surahByName("Surah Ikhlaas"))
        assertTrue(ReferenceChecker.checkAyah("2:1-5", null, 2, 1, 5).ok); assertFalse(ReferenceChecker.checkAyah("1:7-9", null, 1, 7, 9).ok)
    }

    @Test fun referenceOfflineText() {
        val ayah = ReferenceChecker.withText(ctx, ReferenceChecker.checkAyah("112:1", null, 112, 1))
        assertTrue(ayah.text, ayah.text.contains("قُلْ") || ayah.text.contains("قل"))
        val hadith = ReferenceChecker.withText(ctx, ReferenceChecker.find("Bukhari 1").single())
        assertTrue(hadith.message, hadith.ok); assertTrue(hadith.text.isNotBlank())
        val missing = ReferenceChecker.withText(ctx, ReferenceChecker.find("Bukhari 99999").single())
        assertFalse(missing.ok)
    }

    @Test fun composerPrefillAndInsert() {
        ComposerLaunch.prefill("Hello"); assertEquals("Hello", ComposerLaunch.consumePrefill()); assertNull(ComposerLaunch.consumePrefill())
        ComposerLaunch.openDraft("d1", ComposerDestination.REEL)
        assertEquals("d1", ComposerLaunch.consumeDraft()); assertEquals(ComposerDestination.REEL, ComposerLaunch.consume())
        assertEquals("Caption", insertAiText(TextFieldValue(""), "  Caption ").text)
        val tags = insertAiText(TextFieldValue("My post", TextRange(7)), "#Sabr #Dua")
        assertEquals("My post #Sabr #Dua", tags.text); assertEquals(18, tags.selection.start)
        assertEquals("My post\n\nNew line", insertAiText(TextFieldValue("My post", TextRange(7)), "New line").text)
        assertEquals("Replaced", insertAiText(TextFieldValue("old", TextRange(0, 3)), "Replaced").text)
    }

    @Test fun flagsDefaultOff() {
        CreatorFlags.override = null
        assertFalse(CreatorFlags.edit); assertFalse(CreatorFlags.daily)
        CreatorFlags.override = mapOf(CreatorFlags.KEY_EDIT to true)
        assertTrue(CreatorFlags.edit); assertFalse(CreatorFlags.daily)
        CreatorFlags.override = null
    }
}
