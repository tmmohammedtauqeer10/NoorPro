package com.noorpro.app.ummah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** UmmahPost with test defaults for the required fields. */
internal fun up(
    id: String, type: String = "text", mediaUrl: String = "", thumbnailUrl: String = "", creatorUid: String = "",
    creatorName: String = "", creatorHandle: String = "", caption: String = "", arabicText: String = "",
    sourceReference: String = "", publishedAt: Long = 0L, category: String = "Reminder", likeCount: Long = 0,
    commentCount: Long = 0, shareCount: Long = 0, saveCount: Long = 0, mediaWidth: Int = 0, mediaHeight: Int = 0,
    mediaRotationDegrees: Int = 0
) = UmmahPost(id = id, type = type, mediaUrl = mediaUrl, thumbnailUrl = thumbnailUrl, creatorUid = creatorUid,
    creatorName = creatorName, creatorHandle = creatorHandle, caption = caption, arabicText = arabicText,
    sourceReference = sourceReference, publishedAt = publishedAt, category = category, likeCount = likeCount,
    commentCount = commentCount, shareCount = shareCount, saveCount = saveCount, mediaWidth = mediaWidth,
    mediaHeight = mediaHeight, mediaRotationDegrees = mediaRotationDegrees)

/** 1.1.0 Ummah feed: tab/hashtag/count logic plus real renders -> build/v102/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V102UmmahFeedTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()
    private val now = System.currentTimeMillis()
    private val hour = 3_600_000L

    private fun post(id: String, uid: String, ageH: Long, likes: Long = 0, category: String = "Reflection", type: String = "text") =
        up(id = id, type = type, creatorUid = uid, creatorName = uid, creatorHandle = "@$uid",
            caption = "Post $id", category = category, publishedAt = now - ageH * hour, likeCount = likes)

    // ------------------------------------------------------------------ logic

    @Test fun followingTabShowsOnlyFollowedCreatorsNewestFirst() {
        val posts = listOf(post("a", "amina", 5), post("b", "bilal", 1), post("c", "amina", 2), post("r", "amina", 0, type = "reel"))
        val out = ummahTabPosts(UmmahFeedTab.FOLLOWING, posts, setOf("amina"), emptySet(), emptySet())
        assertEquals(listOf("c", "a"), out.map { it.id })
    }

    @Test fun latestTabIsChronologicalAndExcludesReels() {
        val posts = listOf(post("a", "x", 5), post("b", "y", 1), post("r", "z", 0, type = "reel"), post("c", "z", 3))
        assertEquals(listOf("b", "c", "a"), ummahTabPosts(UmmahFeedTab.LATEST, posts, emptySet(), emptySet(), emptySet()).map { it.id })
    }

    @Test fun forYouRewardsEngagementAndTopicFilters() {
        val posts = listOf(post("quiet", "x", 3), post("loved", "y", 4, likes = 500, category = "Hadith"))
        val ranked = ummahTabPosts(UmmahFeedTab.FOR_YOU, posts, emptySet(), emptySet(), emptySet())
        assertEquals("loved", ranked.first().id)
        assertEquals(listOf("loved"), ummahTabPosts(UmmahFeedTab.FOR_YOU, posts, emptySet(), emptySet(), emptySet(), "Hadith").map { it.id })
        assertTrue(matchesUmmahTopic(post("d", "x", 1, category = "Dua"), "Duas"))
    }

    @Test fun captionSegmentsSplitHashtagsAndMentions() {
        val segs = captionSegments("Peace to all #Jumuah with @amina_k, see email a@b.com #ok")
        assertEquals(listOf("#Jumuah", "@amina_k", "#ok"), segs.mapNotNull { it.tag })
        assertEquals("Peace to all #Jumuah with @amina_k, see email a@b.com #ok", segs.joinToString("") { it.text })
        assertTrue(captionSegments("").isEmpty())
    }

    @Test fun optimisticCountsAndMediaAspect() {
        assertEquals(11L, optimisticCount(10, serverActive = false, shownActive = true))
        assertEquals(9L, optimisticCount(10, serverActive = true, shownActive = false))
        assertEquals(10L, optimisticCount(10, serverActive = true, shownActive = true))
        assertEquals(0L, optimisticCount(0, serverActive = true, shownActive = false))
        assertEquals(0.8f, ummahMediaAspect(up(id = "t", mediaWidth = 1000, mediaHeight = 3000)), 0.001f)
        assertEquals(16f / 9f, ummahMediaAspect(up(id = "v", type = "video", mediaUrl = "x.mp4")), 0.001f)
        assertEquals(1.5f, ummahMediaAspect(up(id = "r", mediaWidth = 2000, mediaHeight = 3000, mediaRotationDegrees = 90)), 0.001f)
    }

    @Test fun hashtagLaunchIsConsumedOnce() {
        UmmahSearchLaunch.open(" #Sabr ")
        assertEquals("#Sabr", UmmahSearchLaunch.consume())
        assertEquals("", UmmahSearchLaunch.consume())
    }

    // ------------------------------------------------------------------ renders

    private fun save(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        val out = File(File("build/v102").apply { mkdirs() }, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 5000)
    }

    private fun samplePosts(): List<UmmahPost> {
        UmmahFeedPreview.images = mapOf(
            "img://mosque" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_hero_mosque),
            "img://rehal" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_quran_rehal)
        )
        return listOf(
            up(id = "p1", creatorUid = "u1", creatorName = "Amina Yusuf", creatorHandle = "@amina.reflects",
                caption = "Started my morning with Surah Ar-Rahman and a cup of chai. Which of the favours of your Lord will you deny? #Gratitude #FajrVibes",
                category = "Reflection", publishedAt = now - 2 * hour, likeCount = 1240, commentCount = 86, shareCount = 41, saveCount = 120),
            up(id = "p2", type = "image", creatorUid = "u2", creatorName = "Bilal Hassan", creatorHandle = "@bilal.h",
                caption = "Evening light at the masjid tonight. May Allah accept our prayers. #Maghrib #Masjid",
                mediaUrl = "img://mosque", mediaWidth = 1600, mediaHeight = 1200,
                category = "Community", publishedAt = now - 4 * hour, likeCount = 3200, commentCount = 214, shareCount = 96),
            up(id = "p3", creatorUid = "u3", creatorName = "Sumayyah Khan", creatorHandle = "@sumayyah",
                caption = "A reminder for the heart today. #Sabr",
                arabicText = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", sourceReference = "Ash-Sharh 94:5",
                category = "Quran", publishedAt = now - 6 * hour, likeCount = 860, commentCount = 32),
            up(id = "p4", type = "image", creatorUid = "u4", creatorName = "Yusuf Ali", creatorHandle = "@yusuf",
                caption = "My weekend Quran corner. Consistency over intensity. #QuranJourney",
                mediaUrl = "img://rehal", mediaWidth = 1200, mediaHeight = 1200,
                category = "Quran", publishedAt = now - 9 * hour, likeCount = 540, commentCount = 18)
        )
    }

    private fun render(dark: Boolean, startTab: UmmahFeedTab = UmmahFeedTab.FOR_YOU, posts: List<UmmahPost> = samplePosts(),
                       actions: UmmahFeedActions = UmmahFeedActions()) {
        rule.setContent {
            MyApplicationTheme(darkTheme = dark) {
                var tab by remember { mutableStateOf(startTab) }
                var topic by remember { mutableStateOf("All") }
                Box(Modifier.fillMaxSize().background(stitchBackground())) {
                    NoorUmmahFeed(
                        ui = UmmahFeedUi(posts = posts, isLoggedIn = true, currentUid = "me", displayName = "Tauqeer",
                            following = setOf("u2"), liked = setOf("p2"), saved = setOf("p3")),
                        actions = actions, tab = tab, onTab = { tab = it }, topic = topic, onTopic = { topic = it }
                    )
                }
            }
        }
    }

    @Test fun rendersLightFeedAndMediaPost() {
        render(dark = false)
        rule.onNodeWithTag("ummah_title").assertIsDisplayed()
        rule.onNodeWithTag("ummah_fab").assertIsDisplayed()
        save("feed_light")
        rule.onNodeWithTag("ummah_feed_list").performScrollToIndex(4)
        save("media_light")
    }

    @Test fun rendersDarkFeedAndMediaPost() {
        render(dark = true)
        save("feed_dark")
        rule.onNodeWithTag("ummah_feed_list").performScrollToIndex(5)
        save("media_dark")
    }

    @Test fun followingTabAndActionsAreWired() {
        val tags = mutableListOf<String>()
        val likes = mutableListOf<String>()
        render(dark = false, actions = UmmahFeedActions(onTag = { tags += it }, onLike = { likes += it.id }))
        rule.onNodeWithTag("ummah_feed_list").performScrollToNode(androidx.compose.ui.test.hasTestTag("like_p1"))
        rule.onNodeWithTag("like_p1").performClick()
        rule.onNodeWithTag("ummah_feed_list").performScrollToIndex(0)
        assertEquals(listOf("p1"), likes)
        rule.onNodeWithTag("ummah_tab_FOLLOWING").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("ummah_post_p2").assertIsDisplayed()
        assertTrue(rule.onAllNodes(androidx.compose.ui.test.hasTestTag("ummah_post_p1")).fetchSemanticsNodes().isEmpty())
        save("following_light")
    }

    @Test fun emptyFollowingAndErrorStates() {
        render(dark = false, startTab = UmmahFeedTab.FOLLOWING, posts = samplePosts().filter { it.creatorUid != "u2" })
        rule.onNodeWithTag("ummah_empty_following").assertIsDisplayed()
        rule.onNodeWithText("Find people").assertIsDisplayed()
        save("empty_following_light")
    }
}
