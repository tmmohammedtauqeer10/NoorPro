package com.noorpro.app.ummah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.*
import com.noorpro.app.data.search.PeopleRanker
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 1.1.1 composer: text helpers, poll/flags, drafts, plus real renders -> build/v103/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V103ComposerTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()
    private val allOn = mapOf(ComposerFlags.KEY_POLLS to true, ComposerFlags.KEY_VISIBILITY to true,
        ComposerFlags.KEY_LOCATION to true, ComposerFlags.KEY_AI_LABEL to true)

    @Before fun setUp() { ComposerFlags.override = allOn; UmmahFeedPreview.offline = true }
    @After fun tearDown() { ComposerFlags.override = null; ComposerPreview.images = emptyMap(); ComposerTagSource.captions = emptyList() }

    // ------------------------------------------------------------------ logic

    @Test fun activeTokenFindsHashtagAndMentionAtCursor() {
        assertEquals(ComposerToken('#', "sab", 6, 10), ComposerText.activeToken("Today #sab", 10))
        assertEquals(ComposerToken('@', "", 3, 4), ComposerText.activeToken("Hi @", 4))
        assertNull(ComposerText.activeToken("email@host", 10))
        assertNull(ComposerText.activeToken("plain words", 5))
        assertNull(ComposerText.activeToken("#tag done", 9))
    }

    @Test fun completeReplacesTokenAndAddsSpace() {
        val tok = ComposerText.activeToken("Read #qu today", 8)!!
        val (text, cursor) = ComposerText.complete("Read #qu today", tok, "#Quran")
        assertEquals("Read #Quran today", text)
        assertEquals(11, cursor)
        val (t2, c2) = ComposerText.complete("cc @am", ComposerText.activeToken("cc @am", 6)!!, "@amina")
        assertEquals("cc @amina ", t2); assertEquals(t2.length, c2)
    }

    @Test fun insertSymbolPadsWithSpace() {
        assertEquals("Salam #" to 7, ComposerText.insertSymbol("Salam", 5, '#'))
        assertEquals("@" to 1, ComposerText.insertSymbol("", 0, '@'))
    }

    @Test fun hashtagSuggestionsPreferFeedTagsThenStarters() {
        val caps = listOf("Patience #Sabr", "more #sabr and #Shukr", "#SabrAlways")
        val s = ComposerText.hashtagSuggestions("sa", caps)
        assertEquals("#Sabr", s.first())
        assertTrue("#SabrAlways" in s && "#Salah" in s)
        assertEquals(s.map { it.lowercase() }.distinct().size, s.size)
    }

    @Test fun pollValidationAndExtraFieldsRespectFlags() {
        assertFalse(ComposerPoll(listOf("Fajr", " ")).isValid)
        assertFalse(ComposerPoll(listOf("Fajr", "fajr")).isValid)
        assertTrue(ComposerPoll(listOf("Fajr", "Isha", "")).isValid)
        val on = composerExtraFields(ComposerPoll(listOf("Fajr", "Isha", "")), aiLabel = true, location = " Makkah ", followersOnly = true, now = 0L)
        assertEquals(setOf("poll", "aiLabel", "locationName", "visibility"), on.keys)
        assertEquals(listOf("Fajr", "Isha"), (on["poll"] as Map<*, *>)["options"])
        assertEquals("Makkah", on["locationName"]); assertEquals("followers", on["visibility"])
        ComposerFlags.override = emptyMap()
        assertTrue(composerExtraFields(ComposerPoll(listOf("a", "b")), true, "Makkah", true).isEmpty())
        assertFalse(ComposerFlags.polls)
    }

    @Test fun blockerExplainsWhyPostIsDisabled() {
        val p = ComposerPoll()
        assertEquals("Write something to share", composerBlocker(ComposerDestination.FEED, " ", "", 0, false, false, p))
        assertNull(composerBlocker(ComposerDestination.FEED, "Salam", "", 0, false, false, p))
        assertNull(composerBlocker(ComposerDestination.FEED, "", "الحمد لله", 0, false, false, p))
        assertNotNull(composerBlocker(ComposerDestination.FEED, "x".repeat(2001), "", 0, false, false, p))
        assertNotNull(composerBlocker(ComposerDestination.FEED, "Vote", "", 0, false, true, p, pollsEnabled = true))
        assertNull(composerBlocker(ComposerDestination.FEED, "Vote", "", 0, false, true, p, pollsEnabled = false))
        assertEquals("Choose or record a video", composerBlocker(ComposerDestination.REEL, "cap", "", 0, false, false, p))
        assertEquals("Keep at least 1 second", composerBlocker(ComposerDestination.REEL, "cap", "", 0, true, false, p, 5000, 5500))
        assertNull(composerBlocker(ComposerDestination.REEL, "cap", "", 0, true, false, p, 0, 9000))
        assertNotNull(composerBlocker(ComposerDestination.STORY, "", "", 0, false, false, p))
        assertNotNull(composerBlocker(ComposerDestination.STORY, "x".repeat(301), "", 1, false, false, p))
        assertNull(composerBlocker(ComposerDestination.STORY, "", "", 1, false, false, p))
    }

    @Test fun trimAndClockHelpers() {
        assertFalse(ReelEditor.needsTrim(0, 30_000, 30_000))
        assertTrue(ReelEditor.needsTrim(2_000, 30_000, 30_000))
        assertTrue(ReelEditor.needsTrim(0, 20_000, 30_000))
        assertEquals("1:05", formatClock(65_400))
        assertEquals(listOf(75, 25, 0), pollPercents(mapOf(0 to 3, 1 to 1), 3))
        assertEquals(listOf(0, 0), pollPercents(emptyMap(), 2))
    }

    @Test fun draftJsonRoundTripAndStoreSaveResume() {
        val d = ComposerDraft("d1", ComposerDestination.REEL, "Caption #Sabr", "Quran", "بِسْمِ اللَّهِ", "1:1",
            pollOptions = listOf("a", "b"), pollHours = 72, aiLabel = true, location = "Madinah", followersOnly = true,
            trimStartMs = 1000, trimEndMs = 9000, coverMs = 4000, updatedAt = 5)
        assertEquals(d, ComposerDraft.fromJson(d.toJson()))
        val src = File(ctx.cacheDir, "pic.jpg").apply { writeBytes(ByteArray(64) { it.toByte() }) }
        val store = ComposerDraftStore(ctx)
        val saved = store.save(d.copy(media = listOf(Uri.fromFile(src).toString()), isVideo = false))
        src.delete()
        val back = store.all().single { it.id == "d1" }
        assertEquals("Caption #Sabr", back.text)
        assertTrue(File(Uri.parse(back.media.single()).path!!).exists()) // private copy survives the source
        assertEquals(saved.media, back.media)
        val state = ComposerUiState(ComposerDestination.FEED).apply { load(back) }
        assertEquals(ComposerDestination.REEL, state.destination)
        assertTrue(state.verseOpen && state.pollOpen && state.aiLabel)
        store.delete("d1")
        assertTrue(store.all().none { it.id == "d1" })
    }

    // ------------------------------------------------------------------ renders

    private fun save(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        val out = File(File("build/v103").apply { mkdirs() }, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 5000)
    }

    private fun bmp(id: Int) = BitmapFactory.decodeResource(ctx.resources, id)

    private val people = listOf(
        PeopleRanker.Person("u1", "Amina Yusuf", "amina.reflects", "", following = true, followsMe = true),
        PeopleRanker.Person("u2", "Amir Khan", "amir.k", "", reels = 4),
        PeopleRanker.Person("u3", "Bilal Hassan", "bilal.h", "")
    )

    private fun postState() = ComposerUiState(ComposerDestination.FEED).apply {
        ComposerPreview.images = mapOf("content://t/1" to bmp(R.drawable.noor_home_hero_mosque), "content://t/2" to bmp(R.drawable.noor_home_quran_rehal))
        val t = "Which time do you find easiest for your daily Quran reading? Sharing my routine after Fajr. #Quran #Routine"
        text = TextFieldValue(t, TextRange(t.length))
        topic = "Quran"
        photos = listOf(Uri.parse("content://t/1"), Uri.parse("content://t/2"))
        pollOpen = true
        poll = ComposerPoll(listOf("After Fajr", "After Isha", "Lunch break"), 72)
        aiLabel = false
        location = "Hyderabad"
    }

    private fun render(state: ComposerUiState, dark: Boolean, drafts: List<ComposerDraft> = emptyList()) {
        rule.setContent {
            MyApplicationTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(stitchBackground())) {
                    NoorComposer(state, ComposerActions(), "Mohammed Tauqeer", "", people, drafts)
                }
            }
        }
    }

    @Test fun rendersPostComposerLight() {
        val s = postState()
        render(s, dark = false)
        rule.onNodeWithText("New post").assertIsDisplayed()
        rule.onNodeWithTag("composer_counter").assertIsDisplayed()
        rule.onNodeWithText("${s.text.text.length}/2000").assertIsDisplayed()
        rule.onNodeWithTag("composer_photos").assertIsDisplayed()
        save("post_light")
        rule.onNodeWithTag("composer_scroll").performScrollToNode(hasTestTag("composer_poll"))
        rule.onNodeWithTag("composer_poll").assertIsDisplayed()
        save("post_poll_light")
    }

    @Test fun rendersPostComposerDark() {
        render(postState(), dark = true)
        save("post_dark")
        rule.onNodeWithTag("composer_scroll").performScrollToNode(hasTestTag("composer_poll"))
        save("post_poll_dark")
    }

    @Test fun flagsOffHideBackendFeatures() {
        ComposerFlags.override = emptyMap()
        val s = postState()
        render(s, dark = false)
        rule.onNodeWithTag("tool_Poll").assertDoesNotExist()
        rule.onNodeWithTag("composer_audience").assertDoesNotExist()
        rule.onNodeWithTag("composer_poll").assertDoesNotExist()
        rule.onNodeWithTag("composer_location").assertDoesNotExist()
        rule.onNodeWithTag("composer_ai").assertDoesNotExist()
        rule.onNodeWithTag("composer_post").assertIsDisplayed()
    }

    @Test fun mentionAutocompleteAndHashtagChipInsert() {
        val s = ComposerUiState(ComposerDestination.FEED).apply { text = TextFieldValue("Jazakallah @am", TextRange(14)) }
        render(s, dark = false)
        rule.onNodeWithTag("composer_mention_suggestions").assertIsDisplayed()
        rule.onNodeWithText("Amina Yusuf").assertIsDisplayed()
        save("mention_light")
        rule.onNodeWithText("Amina Yusuf").performClick()
        rule.waitForIdle()
        assertEquals("Jazakallah @amina.reflects ", s.text.text)
        rule.onNodeWithTag("tool_Hashtag").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("composer_tag_suggestions").assertIsDisplayed()
        rule.onNodeWithText("#Quran").performClick()
        rule.waitForIdle()
        assertEquals("Jazakallah @amina.reflects #Quran ", s.text.text)
    }

    @Test fun rendersReelComposerWithTrimAndCover() {
        val s = ComposerUiState(ComposerDestination.REEL).apply {
            val t = "Sunset dhikr by the sea. SubhanAllah 🌅 #Dhikr #Reflections"
            text = TextFieldValue(t, TextRange(t.length))
            video = Uri.parse("content://t/v.mp4"); videoDurationMs = 42_000; trimStartMs = 3_000; trimEndMs = 33_000
            coverMs = 12_000; coverTouched = true; coverFrame = bmp(R.drawable.noor_home_hero_mosque)
            credit = "Original audio"; topic = "Reflections"
        }
        render(s, dark = false)
        rule.onNodeWithText("New reel").assertIsDisplayed()
        rule.onNodeWithTag("composer_trim").assertIsDisplayed()
        rule.onNodeWithText("0:03 – 0:33 · 0:30").assertIsDisplayed()
        save("reel_light")
    }

    @Test fun rendersReelDarkEmptyAndDrafts() {
        val s = ComposerUiState(ComposerDestination.REEL)
        val drafts = listOf(ComposerDraft("a", ComposerDestination.FEED, "A reminder about patience in hard days #Sabr", "Reminders", updatedAt = System.currentTimeMillis() - 3_600_000),
            ComposerDraft("b", ComposerDestination.REEL, "", "Quran", media = listOf("file:///x.mp4"), isVideo = true, updatedAt = System.currentTimeMillis() - 86_400_000))
        render(s, dark = true, drafts = drafts)
        rule.onNodeWithText("Start your reel").assertIsDisplayed()
        save("reel_empty_dark")
        rule.onNodeWithTag("composer_drafts").performClick()
        rule.onNodeWithTag("composer_drafts_panel").assertIsDisplayed()
        save("drafts_dark")
    }

    @Test fun rendersFeedCardWithPollAndAiBadge() {
        val post = up(id = "pp", creatorUid = "u1", creatorName = "Amina Yusuf", creatorHandle = "@amina.reflects",
            caption = "Which time works best for your daily Quran reading? #Quran", category = "Quran",
            publishedAt = System.currentTimeMillis() - 3_600_000).copy(
            pollOptions = listOf("After Fajr", "After Isha", "Lunch break"), pollEndsAt = System.currentTimeMillis() + 50 * 3_600_000,
            aiLabel = true, locationName = "Hyderabad")
        rule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(stitchBackground())) {
                    androidx.compose.foundation.layout.Column {
                        UmmahPostCard(post, isOwn = true, isFollowing = false, isLiked = false, isSaved = false, likeCount = 12, saveCount = 0, actions = UmmahFeedActions())
                        UmmahPollBlock(post.copy(id = "pp2"), previewTally = mapOf(0 to 18, 1 to 7, 2 to 3), previewMine = 0,
                            modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp))
                    }
                }
            }
        }
        rule.onNodeWithText("AI-assisted").assertIsDisplayed()
        rule.onNodeWithTag("ummah_poll_pp").assertIsDisplayed()
        rule.onNodeWithText("64%").assertIsDisplayed()
        save("feed_poll_light")
    }
}
