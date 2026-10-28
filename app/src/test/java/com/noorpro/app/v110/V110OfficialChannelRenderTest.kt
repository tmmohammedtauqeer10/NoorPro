package com.noorpro.app.v110

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.noorpro.app.audio.catalog.OfficialChannelFlags
import com.noorpro.app.audio.catalog.OfficialChannels
import com.noorpro.app.audio.catalog.OfficialVideo
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.v109.shot
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V110OfficialChannelRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ch = OfficialChannels.all.first()

    // Real titles of recent public uploads (checked embeddable via oEmbed, Oct 2026); local stand-in thumbnails.
    private val videos = listOf(
        OfficialVideo("gL4_n4MmImQ", "Adab Pehla Qareena Hai Mohabbat Ke Qareeno Mein | Complete Lecture", "2026-10-25T10:30:00+00:00", 25_600),
        OfficialVideo("AGdiNcLIRrg", "Tune Har Shakhs Ki Qismat Mein Izzat Liki | Complete Lecture", "2026-10-20T15:00:00+00:00", 41_200),
        OfficialVideo("APnmjqHK6fM", "Falsafa e Mout o Hayat | Complete Khutba e Jumma", "2026-10-17T09:00:00+00:00", 18_900),
        OfficialVideo("B8-JkZC3p9I", "Mehboob Aur Muhib Ka Khoobsurat Rishta | Kasur | Complete Lecture", "2026-10-12T18:00:00+00:00", 33_000),
        OfficialVideo("pzo0RzZ_szI", "Allah Tak Poohanchny Ka Wahid Rasta | Complete Lecture", "2026-10-08T18:00:00+00:00", 12_450),
        OfficialVideo("6l8tgjmFUfY", "What to Do When Life Gets Difficult | Complete Khutba e Jumma", "2026-10-03T09:00:00+00:00", 52_300),
    )
    /**
     * REAL YouTube thumbnails (i.ytimg.com/vi/<id>/hqdefault.jpg) downloaded into app/build/v110-thumbs before
     * the run (build dir only - never bundled). A missing file renders the in-app fallback placeholder.
     */
    private val art = videos.associate { v ->
        v.id to java.io.File("build/v110-thumbs/${v.id}.jpg").takeIf { it.length() > 1000 }
            ?.let { android.graphics.BitmapFactory.decodeFile(it.path) }?.asImageBitmap()
    }

    @Before fun setUp() { officialPreviewArt = { art[it] }; println("v110 real thumbnails loaded: " + art.values.count { it != null } + "/" + art.size) }
    @After fun tearDown() { officialPreviewArt = null; OfficialChannelFlags.override = null }

    private fun show(dark: Boolean, content: @Composable () -> Unit) = rule.setContent { MyApplicationTheme(darkTheme = dark) { content() } }
    private fun screen(dark: Boolean, state: OfficialFeedState, playing: String? = null, err: Int? = null) =
        show(dark) { OfficialChannelVideosScreen(SpTheme(!dark), ch, state, playing, err, preview = true) }

    @Composable private fun cardPage(dark: Boolean, embed: Boolean?) {
        val t = SpTheme(!dark)
        Column(Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
            OfficialChannelsBlock(t.elevated, t.text, t.muted, Modifier.padding(16.dp))
            if (embed != null) OfficialChannelCard(ch, t.elevated, t.text, t.muted, Modifier.padding(16.dp), embedEnabled = embed)
        }
    }

    @Test fun cardFlagOffIsUnchanged() {
        show(false) { cardPage(false, null) }
        rule.onNodeWithText("Watch on YouTube").assertExists()
        rule.onNodeWithTag("oc_watch_in_app").assertDoesNotExist()
        rule.onNodeWithText("Opens the official channel on YouTube. Noor Pro does not host this content.").assertExists()
        rule.shot("v110_card_flag_off_light")
    }
    @Test fun cardFlagOnLight() {
        OfficialChannelFlags.override = mapOf(OfficialChannelFlags.EMBED to true)
        show(false) { cardPage(false, null) }
        rule.onNodeWithTag("oc_watch_in_app").assertExists()
        rule.onNodeWithText("Watch on YouTube").assertExists() // existing link-out kept
        rule.shot("v110_card_flag_on_light")
    }
    @Test fun cardFlagOnDark() {
        OfficialChannelFlags.override = mapOf(OfficialChannelFlags.EMBED to true)
        show(true) { cardPage(true, null) }
        rule.onNodeWithTag("oc_watch_in_app").assertExists()
        rule.shot("v110_card_flag_on_dark")
    }
    @Test fun listLight() {
        screen(false, OfficialFeedState.Ready(videos))
        rule.onNodeWithTag("oc_subscribe").assertExists(); rule.onNodeWithTag("oc_open_youtube").assertExists()
        rule.onNodeWithTag("oc_player").assertDoesNotExist()
        rule.onNodeWithTag("oc_video_gL4_n4MmImQ").assertExists()
        rule.shot("v110_list_light")
    }
    @Test fun listDark() { screen(true, OfficialFeedState.Ready(videos)); rule.shot("v110_list_dark") }
    @Test fun playingLight() {
        screen(false, OfficialFeedState.Ready(videos), playing = "AGdiNcLIRrg")
        rule.onNodeWithTag("oc_player").assertExists(); rule.onNodeWithText("Now playing").assertExists()
        rule.onNodeWithText("Open on YouTube").assertExists()
        rule.shot("v110_playing_light")
    }
    @Test fun playingDark() { screen(true, OfficialFeedState.Ready(videos), playing = "gL4_n4MmImQ"); rule.onNodeWithTag("oc_player").assertExists(); rule.shot("v110_playing_dark") }
    @Test fun notEmbeddableFallbackDark() {
        screen(true, OfficialFeedState.Ready(videos), playing = "APnmjqHK6fM", err = 150)
        rule.onNodeWithTag("oc_fallback").assertExists(); rule.onNodeWithTag("oc_player").assertDoesNotExist()
        rule.onNodeWithText("The channel doesn't allow this video to play outside YouTube.").assertExists()
        rule.shot("v110_fallback_dark")
    }
    @Test fun failedLight() {
        screen(false, OfficialFeedState.Failed)
        rule.onNodeWithTag("oc_failed").assertExists(); rule.onNodeWithTag("oc_retry").assertExists(); rule.onNodeWithTag("oc_open_youtube").assertExists()
        rule.shot("v110_failed_light")
    }
    @Test fun loadingDark() { screen(true, OfficialFeedState.Loading); rule.onNodeWithTag("oc_loading").assertExists(); rule.onAllNodesWithTag("oc_shimmer", useUnmergedTree = true).assertCountEquals(24); rule.shot("v110_loading_dark") }
    @Test fun loadingLight() { screen(false, OfficialFeedState.Loading); rule.onNodeWithTag("oc_loading").assertExists(); rule.shot("v110_loading_light") }
    @Test fun failedDark() { screen(true, OfficialFeedState.Failed); rule.onNodeWithTag("oc_failed").assertExists(); rule.shot("v110_failed_dark") }

    /** A thumbnail that can't load shows the pattern placeholder (no cartoon art). */
    @Test fun missingThumbFallbackLight() {
        officialPreviewArt = { id -> if (id == "AGdiNcLIRrg" || id == "B8-JkZC3p9I") null else art[id] }
        screen(false, OfficialFeedState.Ready(videos))
        rule.onAllNodesWithTag("oc_thumb_fallback", useUnmergedTree = true).assertCountEquals(2)
        rule.shot("v110_thumb_fallback_light")
    }
    @Test fun missingThumbFallbackDark() {
        officialPreviewArt = { id -> if (id == "AGdiNcLIRrg" || id == "B8-JkZC3p9I") null else art[id] }
        screen(true, OfficialFeedState.Ready(videos))
        rule.onAllNodesWithTag("oc_thumb_fallback", useUnmergedTree = true).assertCountEquals(2)
        rule.shot("v110_thumb_fallback_dark")
    }

    /** Placeholder close-ups: shimmer + pattern card, light and dark. */
    @Test fun placeholderCloseups() {
        show(false) {
            Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF4F1EA)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(true, false).forEach { light ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OcShimmer(light, Modifier.weight(1f).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)))
                        OcThumbFallback(light, Modifier.weight(1f).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)), glyph = 40.dp)
                    }
                }
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) { OcPatternBackdrop(false, Modifier.matchParentSize()) }
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) { OcPatternBackdrop(true, Modifier.matchParentSize()) }
            }
        }
        rule.shot("v110_placeholders")
    }
}
