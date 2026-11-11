package com.noorpro.app.v109

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.noorpro.app.data.UmmahComment
import com.noorpro.app.data.creator.RefCheck
import com.noorpro.app.ui.components.stitchBackground
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
class V109ReelsRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val green = 0xFF0B6243.toInt(); private val deep = 0xFF06291D.toInt()
    private val f916 = frame(540, 960, 0xFF0E5A42.toInt(), 0xFF0A1F33.toInt(), "9:16")
    private val f169 = frame(960, 540, 0xFF1B3A5C.toInt(), 0xFF0B6243.toInt(), "16:9")
    private val f11 = frame(720, 720, 0xFF4A3B2A.toInt(), 0xFF0B6243.toInt(), "1:1")
    private val f45 = frame(720, 900, 0xFF3D2B1F.toInt(), 0xFF1B3A5C.toInt(), "4:5")

    private val caption = "Sabr in hardship — Part 2/4\nWhat the Qur'an says about patience when life gets heavy. #sabr #quran\n0:00 Intro\n0:40 The ayah\n1:50 Story of Ayyub (AS)"

    private fun ui(w: Int, h: Int, extra: (Reels2Ui) -> Reels2Ui = { it }) = extra(Reels2Ui(
        post = post("p", caption, uid = "c1", w = w, h = h, category = "Bayan", likes = 1240).copy(
            creatorName = "Ustadha Maryam", creatorHandle = "@maryam.learns", commentCount = 86, shareCount = 41, viewCount = 18_300, sourceReference = "Qur'an 2:153"),
        likeCount = 1240, positionMs = 52_000, durationMs = 150_000, verified = true, refCount = 1,
    ))

    private fun bmp(b: Bitmap) = b.asImageBitmap()

    @Composable private fun page(u: Reels2Ui, img: Bitmap, overlay: Reels2Overlay = Reels2Overlay.NONE, f: Reels2Features = Reels2Features(), expanded: Boolean = false, recitation: Int = -1, next: Boolean = false) {
        Box(Modifier.fillMaxSize()) {
            Reels2Page(
                u, Reels2Actions(),
                background = { m -> Image(bmp(img), null, contentScale = ContentScale.Crop, modifier = m) },
                foreground = { m -> Image(bmp(img), null, contentScale = if (u.frame.mode == ReelFrameMode.FULL_BLEED) ContentScale.Crop else ContentScale.Fit, modifier = m) },
                overlay = overlay, features = f, captionExpanded = expanded, recitationIndex = recitation,
                aboveInfo = if (next) ({ Reels2NextPartCard(post("n", "Sabr in hardship — Part 3/4"), u.series!!, bmp(f45), {}) }) else null,
            )
            Reels2TopBar(Reels2Tab.FOR_YOU, {}, "Bayan", {})
        }
    }

    private fun show(dark: Boolean, content: @Composable () -> Unit) = rule.setContent { MyApplicationTheme(darkTheme = dark) { content() } }

    @Composable private fun sheetOver(img: Bitmap, content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            Image(bmp(img), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0x99000000)))
            Box(Modifier.align(Alignment.BottomCenter)) { content() }
        }
    }

    // ---------- framing: 3 cases (+ 9:16)
    @Test fun frame916FullBleed() {
        show(true) { page(ui(1080, 1920) { it.copy(captionsOn = true, captionLine = "Indeed, Allah is with the patient.") }, f916) }
        rule.onNodeWithTag("reels2_fullscreen").assertDoesNotExist()
        rule.shot("reels_916_fullbleed")
    }
    @Test fun frame169BlurFitWithPill() {
        show(false) { page(ui(1920, 1080), f169) }
        rule.onNodeWithTag("reels2_fit_16:9").assertExists(); rule.onNodeWithTag("reels2_fullscreen").assertExists()
        rule.shot("reels_169_blurfit")
    }
    @Test fun frame11BlurFit() {
        show(true) { page(ui(1080, 1080), f11) }
        rule.onNodeWithTag("reels2_fit_1:1").assertExists(); rule.onNodeWithTag("reels2_fullscreen").assertDoesNotExist()
        rule.shot("reels_11_blurfit")
    }
    @Test fun frame45BlurFit() {
        show(false) { page(ui(1080, 1350), f45) }
        rule.onNodeWithTag("reels2_fit_4:5").assertExists()
        rule.shot("reels_45_blurfit")
    }

    // ---------- overlays
    @Test fun doubleTapHeart() { show(true) { page(ui(1080, 1920) { it.copy(liked = true) }, f916, Reels2Overlay.HEART) }; rule.onNodeWithTag("reels2_heart").assertExists(); rule.shot("reels_heart") }
    @Test fun holdToPause() { show(true) { page(ui(1080, 1920) { it.copy(speed = 1.5f, captionsOn = true) }, f916, Reels2Overlay.HOLD) }; rule.shot("reels_hold") }
    @Test fun reactionsFlagged() { show(true) { page(ui(1080, 1920) { it.copy(reaction = HalalReaction.MASHAALLAH, liked = true) }, f916, Reels2Overlay.REACTIONS, Reels2Features.ALL_ON) }; rule.shot("reels_reactions_flag") }
    @Test fun captionExpandedSeriesNext() { show(true) { page(ui(1080, 1920) { it.copy(following = true) }, f916, f = Reels2Features.ALL_ON, expanded = true, next = true) }; rule.onNodeWithTag("reels2_next_part").assertExists(); rule.shot("reels_series_next_flag") }
    @Test fun recitationOverlay() {
        show(true) {
            val u = ui(1080, 1920) { it.copy(post = it.post.copy(caption = "Surah Al-Mulk recitation · Qari Yusuf", category = "Quran", arabicText = "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ ۝ الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا")) }
            page(u, f916, recitation = 0)
        }
        rule.shot("reels_recitation")
    }

    // ---------- sheets (light + dark)
    private val comments = listOf(
        UmmahComment("c1", "p", "JazakAllah khair, this is exactly what I needed today.", "u1", "Aisha Rahman", 1_000, likeCount = 34),
        UmmahComment("c2", "p", "Which tafsir did you quote at 0:40?", "u2", "Yusuf Khan", 2_000, likeCount = 12),
        UmmahComment("c3", "p", "Ameen. Please make a part on shukr too.", "u3", "Fatima", 3_000, likeCount = 3),
        UmmahComment("c4", "p", "SubhanAllah", "u4", "Bilal", 4_000),
    )
    @Composable private fun commentsSheet(f: Reels2Features) = sheetOver(f916) {
        Reels2CommentsSheet(orderReelComments(comments, "c2", if (f.creatorLiked) setOf("c1") else emptySet()), 86, "Ustadha Maryam", f, likedIds = setOf("c1"), modifier = Modifier.heightIn(max = 620.dp))
    }
    @Test fun commentsLight() { show(false) { commentsSheet(Reels2Features()) }; rule.shot("reels_comments_light") }
    @Test fun commentsDarkAllFlags() { show(true) { commentsSheet(Reels2Features.ALL_ON) }; rule.shot("reels_comments_dark_flags") }
    @Test fun longPressMenu() { show(false) { sheetOver(f916) { Reels2LongPressMenu({}, {}, {}, {}, showReasons = true) } }; rule.shot("reels_menu_report_light") }
    @Test fun whyDark() { show(true) { sheetOver(f916) { Reels2WhySheet(whyThisReel(ui(1, 1).post.copy(locationName = "Hyderabad"), setOf("c1"), setOf("Bayan"), "Hyderabad")) } }; rule.shot("reels_why_dark") }
    @Test fun askAiLight() {
        show(false) { sheetOver(f916) { Reels2AskAiSheet(Reels2AiAnswer("The speaker explains that patience (sabr) is active trust in Allah during hardship, not giving up.", "Qur'an 2:153", "يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ", "When something hard happens, keep doing good and keep praying — Allah is with people who stay patient."), simple = true, onSimple = {}, keyPoints = parseReelChapters(caption)) } }
        rule.shot("reels_ask_ai_light")
    }
    @Test fun askAiDarkError() { show(true) { sheetOver(f916) { Reels2AskAiSheet(Reels2AiAnswer("", "", "", "", error = "Noor AI isn't reachable right now. Please try again later."), false, {}) } }; rule.shot("reels_ask_ai_dark_offline") }
    @Test fun verifyLight() {
        show(false) { sheetOver(f916) { Reels2VerifySheet(listOf(RefCheck("Qur'an 2:153", RefCheck.Kind.QURAN, true, "Found: Al-Baqarah 2:153", text = "O you who believe, seek help through patience and prayer."), RefCheck("Bukhari 9999", RefCheck.Kind.HADITH, false, "No hadith with this number in the bundled collection"))) } }
        rule.shot("reels_verify_light")
    }
    @Test fun collectionsDark() { show(true) { sheetOver(f916) { Reels2CollectionSheet(listOf("Ramadan prep", "For my kids"), "Watch later", {}) } }; rule.shot("reels_watch_later_dark") }
    @Test fun soundPageFlag() { show(false) { Reels2SoundPage(Reels2Sound("Surah Ar-Rahman (1–13)", "Qari Abdul Basit · Al-Noor", 214, true), listOf(post("a") to bmp(f916), post("b") to bmp(f45), post("c") to bmp(f11)), {}) }; rule.shot("reels_sound_flag") }
    @Test fun searchContinueLiveDark() {
        show(true) {
            Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Reels2SmartSearch("find reels about patience in Urdu", {}, parseSmartReelQuery("find reels about patience in Urdu"))
                Reels2ContinueRow(listOf(Triple(post("a", "Tafsir of Surah Yusuf · lecture 3"), WatchProgress("a", 420_000, 1_500_000, 1), bmp(f169)), Triple(post("b", "Seerah for kids Part 1"), WatchProgress("b", 60_000, 240_000, 1), bmp(f11))), {})
                Reels2LiveCard("Friday bayan: Sabr & Shukr", "Mufti Ismail · Hyderabad", "Fri 9:30 PM IST")
                Reels2MiniPlayer("Sabr in hardship — Part 2/4", "Ustadha Maryam", bmp(f916), true, {}, {}, {})
            }
        }
        rule.shot("reels_search_continue_live_mini")
    }

    // ---------- landscape player (rotated device)
    @Test @Config(qualifiers = "w891dp-h411dp-xxhdpi") fun landscapePlayer() {
        show(true) {
            val u = ui(1920, 1080) { it.copy(captionsOn = true, speed = 1.25f) }
            Reels2LandscapePlayer(u, { m -> Image(bmp(f169), null, contentScale = ContentScale.Fit, modifier = m) }, {}, {}, true, {}, {}, {}, post("n", "Sabr in hardship — Part 3/4"), {})
        }
        rule.shot("reels_landscape_player")
    }
}
