package com.noorpro.app.ummah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerLaunch
import com.noorpro.app.data.UmmahStory
import com.noorpro.app.data.UmmahStoryGroup
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 1.1.2: the Ummah stories ("status") row in the real feed, in every tab, never hidden. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V104StoriesTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val now = System.currentTimeMillis()
    private val hour = 3_600_000L

    private fun story(uid: String, name: String, avatar: String, ageH: Long, type: String = "image") = UmmahStory(
        id = "s_$uid", authorUid = uid, authorName = name, authorHandle = "@${name.lowercase().replace(' ', '.')}",
        avatarUrl = avatar, mediaUrl = "https://example.org/$uid.jpg", mediaPath = "", caption = "",
        createdAt = now - ageH * hour, expiresAt = now + (24 - ageH) * hour, type = type)

    private fun group(s: UmmahStory) = UmmahStoryGroup(s.authorUid, s.authorName, s.authorHandle, s.avatarUrl, listOf(s))

    private val seenUids = setOf("u4", "u5")
    private fun sampleGroups(): List<UmmahStoryGroup> {
        UmmahFeedPreview.images = mapOf(
            "img://mosque" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_hero_mosque),
            "img://rehal" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_quran_rehal)
        )
        return listOf(
            group(story("u4", "Yusuf Ali", "", 1)),                       // seen, newest
            group(story("u1", "Amina Yusuf", "img://mosque", 2)),
            group(story("u2", "Bilal Hassan", "", 3, type = "video")),
            group(story("u5", "Khadija", "img://rehal", 5)),             // seen
            group(story("u3", "Sumayyah Khan", "img://rehal", 6)),
        )
    }

    private fun ui() = UmmahFeedUi(
        posts = listOf(
            up(id = "p1", creatorUid = "u1", creatorName = "Amina Yusuf", creatorHandle = "@amina.reflects",
                caption = "Started my morning with Surah Ar-Rahman. Which of the favours of your Lord will you deny? #Gratitude",
                category = "Reflection", publishedAt = now - 2 * hour, likeCount = 1240, commentCount = 86, shareCount = 41),
            up(id = "p2", type = "image", creatorUid = "u2", creatorName = "Bilal Hassan", creatorHandle = "@bilal.h",
                caption = "Evening light at the masjid tonight. #Maghrib", mediaUrl = "img://mosque", mediaWidth = 1600, mediaHeight = 1200,
                category = "Community", publishedAt = now - 4 * hour, likeCount = 3200, commentCount = 214)
        ),
        isLoggedIn = true, currentUid = "me", displayName = "Tauqeer", following = setOf("u1", "u2")
    )

    private fun save(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        val out = File(File("build/v104").apply { mkdirs() }, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 5000)
    }

    private fun feed(dark: Boolean, groups: List<UmmahStoryGroup>, onAdd: () -> Unit = {}, onOpen: (Int) -> Unit = {}) {
        UmmahFeedPreview.offline = true
        var tab by mutableStateOf(UmmahFeedTab.FOR_YOU)
        rule.setContent {
            MyApplicationTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(stitchBackground())) {
                    NoorUmmahFeed(
                        ui = ui(), actions = UmmahFeedActions(), tab = tab, onTab = { tab = it }, topic = "All", onTopic = {},
                        stories = {
                            val ordered = orderStoryGroups(groups, "me") { it.authorUid in seenUids }
                            NoorStoriesRow(ordered, "me", "Tauqeer", "", isSeen = { it.authorUid in seenUids },
                                onAddStory = onAdd, onOpen = onOpen)
                        }
                    )
                }
            }
        }
    }

    @Test fun unseenFirstOrdering() {
        val g = sampleGroups() + group(story("me", "Tauqeer", "", 1))
        val ordered = orderStoryGroups(g, "me") { it.authorUid in seenUids }
        assertEquals(listOf("me", "u1", "u2", "u3", "u4", "u5"), ordered.map { it.authorUid })
        assertEquals(listOf("u1", "u4"), orderStoryGroups(g.take(2), null) { it.authorUid in seenUids }.map { it.authorUid })
    }

    @Test fun composerLaunchIsConsumedOnce() {
        ComposerLaunch.open(ComposerDestination.STORY)
        assertEquals(ComposerDestination.STORY, ComposerLaunch.consume())
        assertNull(ComposerLaunch.consume())
    }

    @Test fun realFeedShowsStoriesInEveryTabAndOpensViewerAndComposer() {
        var added = 0; var opened = -1
        feed(dark = false, groups = sampleGroups(), onAdd = { added++ }, onOpen = { opened = it })
        rule.onNodeWithTag("ummah_stories_row").assertIsDisplayed()
        rule.onNodeWithTag("story_you").assertIsDisplayed()
        rule.onNodeWithText("3 new").assertIsDisplayed()
        save("feed_stories_light")
        rule.onNodeWithTag("story_add").performClick()
        assertEquals(1, added)
        rule.onNodeWithTag("story_u1").performClick()
        assertEquals(0, opened) // unseen Amina sorts first among others (no story of mine)
        for (t in listOf("FOLLOWING", "LATEST")) {
            rule.onNodeWithTag("ummah_tab_$t").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("ummah_stories_row").assertIsDisplayed()
            rule.onNodeWithTag("story_you").assertIsDisplayed()
        }
    }

    @Test fun realFeedStoriesDark() {
        feed(dark = true, groups = sampleGroups())
        rule.onNodeWithTag("ummah_stories_row").assertIsDisplayed()
        // scroll to the end so the grey "seen" rings are visible in the preview
        rule.onNodeWithTag("ummah_stories_list").performScrollToNode(hasTestTag("story_u5"))
        rule.onNodeWithTag("story_u5").assertIsDisplayed()
        save("feed_stories_dark")
    }

    @Test fun onlyYourStoryLight() {
        var added = 0
        feed(dark = false, groups = emptyList(), onAdd = { added++ })
        rule.onNodeWithTag("story_you").assertIsDisplayed()
        rule.onNodeWithText("Share a moment").assertIsDisplayed()
        rule.onNodeWithTag("story_you").performClick() // no story yet: tapping the bubble also starts one
        assertEquals(1, added)
        save("feed_only_you_light")
    }

    @Test fun onlyYourStoryDark() {
        feed(dark = true, groups = emptyList())
        rule.onNodeWithTag("story_you").assertIsDisplayed()
        save("feed_only_you_dark")
    }

    /** The production slot (StoriesSection) must render for guests too — 1.1.1 returned early. */
    @Test fun productionStoriesSectionRendersForGuestInRealFeed() {
        UmmahFeedPreview.offline = true
        var created = 0
        rule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(stitchBackground())) {
                    NoorUmmahFeed(
                        ui = ui().copy(isLoggedIn = false, currentUid = ""), actions = UmmahFeedActions(),
                        tab = UmmahFeedTab.FOR_YOU, onTab = {}, topic = "All", onTopic = {},
                        stories = { StoriesSection("Guest", "@guest", "", false, emptySet(), { _, _ -> }, onCreateStory = { created++ }) }
                    )
                }
            }
        }
        rule.onNodeWithTag("ummah_stories_row").assertIsDisplayed()
        rule.onNodeWithTag("story_add").performClick()
        assertEquals(1, created)
    }
}
