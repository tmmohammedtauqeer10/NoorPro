package com.noorpro.app.v108

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.recs.PersonSuggestion
import com.noorpro.app.data.recs.SuggestionKind
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.ummah.up
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V108FeedRecsRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()
    private val now = System.currentTimeMillis()
    private val hour = 3_600_000L

    private fun posts() = listOf(
        up(id = "p1", creatorUid = "u1", creatorName = "Amina Yusuf", creatorHandle = "@amina.reflects", caption = "Morning with Surah Ar-Rahman. #Gratitude", category = "Reflection", publishedAt = now - 2 * hour, likeCount = 1240, commentCount = 86),
        up(id = "p2", creatorUid = "u2", creatorName = "Bilal Hassan", creatorHandle = "@bilal.h", caption = "Evening light at the masjid tonight. #Maghrib", category = "Community", publishedAt = now - 4 * hour, likeCount = 320),
        up(id = "p3", creatorUid = "u3", creatorName = "Sumayyah Khan", creatorHandle = "@sumayyah", caption = "A reminder for the heart today. #Sabr", arabicText = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", sourceReference = "Ash-Sharh 94:5", category = "Quran", publishedAt = now - 6 * hour, likeCount = 860),
        up(id = "p4", creatorUid = "u4", creatorName = "Yusuf Ali", creatorHandle = "@yusuf", caption = "Consistency over intensity. #QuranJourney", category = "Quran", publishedAt = now - 9 * hour, likeCount = 540),
        up(id = "p5", creatorUid = "u5", creatorName = "Maryam S", creatorHandle = "@maryam", caption = "Dua for parents, every day after Asr.", category = "Dua", publishedAt = now - 11 * hour, likeCount = 90),
        up(id = "p6", creatorUid = "u6", creatorName = "Omar F", creatorHandle = "@omar", caption = "Jumuah reminder: Surah al-Kahf.", category = "Reminder", publishedAt = now - 13 * hour, likeCount = 70),
    ) + (7..12).map { up(id = "p$it", creatorUid = "u$it", creatorName = "Member $it", creatorHandle = "@member$it", caption = "Alhamdulillah for another day.", category = "Community", publishedAt = now - (14 + it) * hour, likeCount = 10) }

    private val recs = listOf(
        PersonSuggestion("s1", "Sara Rahman", "@sara.reads", "img://a1", true, 12_400, "Followed by Bilal + 2", SuggestionKind.MUTUAL, 90.0),
        PersonSuggestion("s2", "Zaid Qureshi", "@zaid.tafsir", "", false, 1_830, "Posts about Quran", SuggestionKind.INTEREST, 60.0),
        PersonSuggestion("s3", "Hafsa Noor", "@hafsa", "img://a2", false, 640, "New near you · Hyderabad", SuggestionKind.NEARBY, 40.0),
        PersonSuggestion("s4", "Ibrahim K", "@ibrahim", "", false, null, "Active creator · 5 posts lately", SuggestionKind.ACTIVE, 30.0),
    )

    private fun render(dark: Boolean, onFollow: (PersonSuggestion) -> Unit = {}, onDismiss: (PersonSuggestion) -> Unit = {}) {
        UmmahFeedPreview.images = mapOf(
            "img://a1" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_hero_mosque),
            "img://a2" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_quran_rehal),
        )
        rule.setContent {
            MyApplicationTheme(darkTheme = dark) {
                var following by remember { mutableStateOf(setOf("u2")) }
                var list by remember { mutableStateOf(recs) }
                Box(Modifier.fillMaxSize().background(stitchBackground())) {
                    NoorUmmahFeed(
                        ui = UmmahFeedUi(posts = posts(), isLoggedIn = true, currentUid = "me", displayName = "Tauqeer",
                            following = following, suggestions = list),
                        actions = UmmahFeedActions(
                            onSuggestionFollow = { s -> following = if (s.uid in following) following - s.uid else following + s.uid; onFollow(s) },
                            onSuggestionDismiss = { s -> list = list - s; onDismiss(s) },
                        ),
                        tab = UmmahFeedTab.FOR_YOU, onTab = {}, topic = "All", onTopic = {}
                    )
                }
            }
        }
    }

    private fun scrollToRow(nudge: Boolean = true) {
        rule.onNodeWithTag("ummah_feed_list").performScrollToNode(hasTestTag("ummah_suggestions"))
        if (nudge) rule.onNodeWithTag("ummah_feed_list").performTouchInput { swipeUp(startY = centerY + 300f, endY = centerY - 300f, durationMillis = 3000) }
        rule.waitForIdle()
    }

    @Test fun carouselLight() {
        render(false)
        scrollToRow()
        rule.onNodeWithText("Recommended for you").assertIsDisplayed()
        rule.onNodeWithText("Followed by Bilal + 2").assertIsDisplayed()
        rule.saveShot("feed_recs_light")
    }

    @Test fun carouselDark() {
        render(true)
        scrollToRow()
        rule.onNodeWithText("Recommended for you").assertIsDisplayed()
        rule.saveShot("feed_recs_dark")
    }

    @Test fun followFlipsInstantlyAndDismissRemovesCard() {
        val followed = mutableListOf<String>(); val dismissed = mutableListOf<String>()
        render(false, { followed += it.uid }, { dismissed += it.uid })
        scrollToRow(nudge = false)
        rule.onNodeWithTag("suggestion_follow_s1").performClick()
        rule.onNodeWithTag("suggestion_follow_s1").assert(hasText("Following"))
        rule.onNodeWithTag("suggestion_dismiss_s2").performClick()
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodesWithTag("suggestion_s2").fetchSemanticsNodes().size)
        assertEquals(listOf("s1"), followed); assertEquals(listOf("s2"), dismissed)
    }
}
