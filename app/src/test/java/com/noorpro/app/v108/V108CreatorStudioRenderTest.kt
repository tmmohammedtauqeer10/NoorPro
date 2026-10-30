package com.noorpro.app.v108

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.UmmahStory
import com.noorpro.app.data.creator.*
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V108CreatorStudioRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun img(a: Int, b: Int): Bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawRect(0f, 0f, 200f, 200f, Paint().apply { shader = LinearGradient(0f, 0f, 200f, 200f, a, b, Shader.TileMode.CLAMP) })
    }

    private fun sample(tab: StudioTab): CreatorStudioState {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC")) // sample posts are placed at UTC hours
        UmmahFeedPreview.images = mapOf(
            "https://x/c.jpg" to img(0xFF26473A.toInt(), 0xFFB9D7C6.toInt()), "https://x/d.jpg" to img(0xFF8C5A2B.toInt(), 0xFFF0D9B5.toInt()),
            "https://x/e.jpg" to img(0xFF223B53.toInt(), 0xFFCFE3E8.toInt()), "https://x/f.jpg" to img(0xFF4A3B2A.toInt(), 0xFFD8C8A8.toInt()),
            "https://x/a.jpg" to img(0xFF0B6243.toInt(), 0xFFD4AF37.toInt()), "https://x/b.jpg" to img(0xFF1B3A5C.toInt(), 0xFF7FB8A4.toInt()),
            "https://x/r.jpg" to img(0xFF3D2B1F.toInt(), 0xFFE8C07D.toInt()), "https://x/s.jpg" to img(0xFF5B3A70.toInt(), 0xFFF3D9A4.toInt()),
        )
        return CreatorStudioState(tab).apply {
            now = NOW; loading = false; myUid = "me"
            posts = listOf(
                post("a", 1.2, 1840, likes = 212, comments = 31, shares = 18, saves = 44, caption = "After every hardship comes ease — Al-Inshirah 94:5", hour = 19, pinned = true),
                post("b", 3.4, 960, likes = 88, comments = 12, shares = 4, saves = 20, caption = "Dua for parents: Rabbi irhamhuma…", hour = 19),
                post("r", 2.1, 5230, likes = 640, comments = 75, shares = 51, saves = 120, type = "reel", caption = "60 seconds on tawakkul", hour = 20),
                post("c", 6.0, 410, likes = 30, comments = 4, caption = "Friday reminder: Surah Al-Kahf", hour = 13),
                post("d", 12.0, 700, likes = 60, comments = 9, caption = "Sabr is beautiful", hour = 19),
                post("e", 18.0, 520, likes = 41, caption = "Gratitude journal", hour = 6),
                post("f", 33.0, 600, likes = 50, caption = "Old post"),
            ).map { if (it.id == "a") it.copy(thumbnailUrl = "https://x/a.jpg") else if (it.id == "b") it.copy(thumbnailUrl = "https://x/b.jpg") else if (it.id == "r") it.copy(thumbnailUrl = "https://x/r.jpg") else it }
            followerTimes = (0 until 260).map { NOW - (it * 0.35 * DAY).toLong() }
            prayer = mapOf("Fajr" to "05:10", "Sunrise" to "06:25", "Dhuhr" to "12:20", "Asr" to "15:45", "Maghrib" to "18:05", "Isha" to "19:20")
            stories = listOf(UmmahStory("s1", "me", "Me", "@me", "", "https://x/s.jpg", "", "Jumu'ah mubarak", NOW - 3 * 3_600_000, NOW + 21 * 3_600_000))
            drafts = listOf(ComposerDraft("d1", ComposerDestination.FEED, "Reflection on Surah Yusuf — patience of Yaqub (AS)", "Quran", updatedAt = NOW - DAY))
            comments = listOf(
                UmmahComment("c1", "a", "SubhanAllah, I needed this today. JazakAllah khair!", "u1", "Aisha Rahman", NOW - 12 * 60_000),
                UmmahComment("c2", "r", "Which tafsir did you use for this? Would love to read more.", "u2", "Yusuf Khan", NOW - 2 * 3_600_000),
                UmmahComment("c3", "a", "Free crypto signals, DM me", "u3", "promo_acc", NOW - 3 * 3_600_000),
                UmmahComment("c4", "b", "Ameen 🤲", "u4", "Fatima", NOW - 5 * 3_600_000),
                UmmahComment("r4", "b", "Ameen, may Allah accept", "me", "You", NOW - 4 * 3_600_000, parentCommentId = "c4"),
            )
            keywords = setOf("crypto", "dm me")
            pins = mapOf("a" to "c1")
        }
    }

    private fun show(state: CreatorStudioState, dark: Boolean, edit: Boolean = true) {
        rule.setContent { MyApplicationTheme(darkTheme = dark) { NoorCreatorStudio(state, CreatorStudioActions(), editEnabled = edit, dailyEnabled = false) } }
    }

    @Test fun insightsLight() {
        show(sample(StudioTab.INSIGHTS), false)
        rule.onNodeWithTag("insight_views").assertTextEquals("9.6K")
        rule.saveShot("studio_insights_light")
    }

    @Test fun insightsDarkBestTime() {
        val s = sample(StudioTab.INSIGHTS); show(s, true)
        rule.onNodeWithTag("studio_insights").performScrollToNode(hasTestTag("insight_best_time"))
        rule.onAllNodesWithText("after", substring = true, ignoreCase = true).onFirst().assertExists()
        rule.saveShot("studio_insights_dark")
    }

    @Test fun rangeSwitch() {
        val s = sample(StudioTab.INSIGHTS); show(s, false)
        rule.onNodeWithTag("range_90").performClick()
        assertEquals(InsightRange.D90, s.range)
        rule.onNodeWithTag("insight_views").assertTextEquals("10K")
    }

    @Test fun contentLightAndMenu() {
        val s = sample(StudioTab.CONTENT); show(s, false)
        rule.onNodeWithTag("content_a").assertExists() // pinned first
        rule.saveShot("studio_content_light")
        rule.onNodeWithTag("content_menu_a").performClick()
        rule.onNodeWithText("Unpin from profile").assertExists(); rule.onNodeWithText("Archive").assertExists()
    }

    @Test fun contentDarkReelsAndNoEditWhenFlagOff() {
        val s = sample(StudioTab.CONTENT); s.kind = StudioKind.REEL; show(s, true, edit = false)
        rule.onNodeWithTag("content_menu_r").performClick()
        rule.onNodeWithText("Edit caption").assertDoesNotExist(); rule.onNodeWithText("Delete").assertExists()
        rule.onNodeWithText("Delete").performClick(); rule.onNodeWithTag("confirm_delete").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        s.kind = StudioKind.DRAFT
        rule.onNodeWithText("Reflection on Surah Yusuf", substring = true).assertExists()
        s.kind = StudioKind.POST
        rule.saveShot("studio_content_dark")
    }

    @Test fun commentsLightReply() {
        val s = sample(StudioTab.COMMENTS); var sent: Pair<String, String>? = null
        rule.setContent { MyApplicationTheme(darkTheme = false) { NoorCreatorStudio(s, CreatorStudioActions(onReply = { c, t -> sent = c.id to t }), true, false) } }
        rule.onNodeWithTag("inbox_c3").assertDoesNotExist() // keyword-filtered
        rule.onNodeWithTag("reply_c2").performClick()
        rule.onNodeWithTag("reply_field_c2").performTextInput("Ibn Kathir, in sha Allah I'll share a link")
        rule.saveShot("studio_comments_light")
        rule.onNodeWithTag("reply_send_c2").performClick()
        assertEquals("c2" to "Ibn Kathir, in sha Allah I'll share a link", sent)
        rule.onNodeWithTag("inbox_FILTERED").performClick()
        rule.onNodeWithTag("inbox_c3").assertExists()
    }

    @Test fun commentsDark() {
        show(sample(StudioTab.COMMENTS), true)
        rule.onNodeWithTag("inbox_c1").assertExists()
        rule.saveShot("studio_comments_dark")
    }

    private fun aiSample(s: CreatorStudioState, check: Boolean) {
        if (check) {
            s.ai.task = CreatorAiTask.CHECK
            s.ai.input = "Inna ma'al usri yusra — Al-Inshirah 94:5. Also Yusuf 12:200 and Bukhari 6018."
            s.ai.refs = listOf(
                ReferenceChecker.checkAyah("Al-Inshirah 94:5", "Al-Inshirah", 94, 5).copy(text = "فَإِنَّ مَعَ ٱلْعُسْرِ يُسْرًا\nFor indeed, with hardship [will be] ease."),
                ReferenceChecker.checkAyah("Yusuf 12:200", "Yusuf", 12, 200),
                RefCheck("Bukhari 6018", RefCheck.Kind.HADITH, true, "Sahih al-Bukhari 6018 ✓ found offline.", collection = "Sahih al-Bukhari", number = "6018",
                    text = "Whoever believes in Allah and the Last Day should not harm his neighbour…"),
            ); s.ai.ran = true
        } else {
            s.ai.input = "Reminder about patience during hard times"; s.ai.topic = "Reflection"
            s.ai.results = listOf(
                "Sabr isn't silence — it's trusting Allah while you keep walking.",
                "Hard days are written too. And so is the ease that follows them. (94:5)",
                "Hold on. The One who tested you has already prepared your relief.",
            ); s.ai.hashtags = listOf("#Sabr", "#Tawakkul", "#IslamicReminder", "#NoorPro"); s.ai.ran = true
        }
    }

    @Test fun aiHelperLight() {
        val s = sample(StudioTab.AI); aiSample(s, false); show(s, false)
        rule.onAllNodesWithTag("ai_result").assertCountEquals(3)
        rule.saveShot("studio_ai_light")
    }

    @Test fun aiHelperDarkCheck() {
        val s = sample(StudioTab.AI); aiSample(s, true); show(s, true)
        rule.onAllNodesWithTag("ref_ok").assertCountEquals(2); rule.onNodeWithTag("ref_bad").assertExists()
        rule.saveShot("studio_ai_dark")
    }

    @Test fun aiInsertCallsBack() {
        val s = sample(StudioTab.AI); aiSample(s, false); var inserted = ""
        rule.setContent { MyApplicationTheme(darkTheme = false) { NoorCreatorStudio(s, CreatorStudioActions(onInsert = { inserted = it }), true, false) } }
        rule.onAllNodesWithTag("ai_insert")[0].performClick()
        assertTrue(inserted.startsWith("Sabr isn't silence"))
    }
}
