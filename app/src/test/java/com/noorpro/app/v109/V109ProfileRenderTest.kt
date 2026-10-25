package com.noorpro.app.v109

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.noorpro.app.data.UmmahProfile
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V109ProfileRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val imgs = listOf(
        frame(400, 500, 0xFF0E5A42.toInt(), 0xFF0A1F33.toInt(), "Sabr"), frame(400, 500, 0xFF1B3A5C.toInt(), 0xFF0B6243.toInt(), "Dua"),
        frame(400, 500, 0xFF4A3B2A.toInt(), 0xFF0B6243.toInt(), "Seerah"), frame(400, 500, 0xFF3D2B1F.toInt(), 0xFF1B3A5C.toInt(), "Kahf"),
        frame(400, 500, 0xFF5B3A70.toInt(), 0xFF0B6243.toInt(), "Shukr"), frame(400, 500, 0xFF26473A.toInt(), 0xFFB9D7C6.toInt(), "Salah"),
    ).map { it.asImageBitmap() }
    private val avatar = frame(300, 300, 0xFF0B6243.toInt(), 0xFFD4AF37.toInt(), "").asImageBitmap()

    private val me = UmmahProfile("me", "Mohammed Tauqeer", "tauqeer", "", "Learning the Qur'an one page a day. Student of tafsir. noorpro.app/tauqeer", verified = true)
    private val posts = listOf(
        post("a", "After every hardship comes ease", type = "image", uid = "me", at = 9, pinned = true), post("b", "Dua for parents", type = "image", uid = "me", at = 8),
        post("c", "Friday reminder: Surah Al-Kahf", type = "image", uid = "me", at = 7), post("d", "Gratitude", type = "image", uid = "me", at = 6),
        post("e", "Sabr — Part 1/3", uid = "me", at = 5).copy(viewCount = 5200), post("f", "Salah tips", type = "text", uid = "me", at = 4),
    )
    private fun thumbs() = posts.mapIndexed { i, p -> p.id to imgs[i % imgs.size] }.filter { it.first != "f" }.toMap()

    private fun ownUi(tab: Profile2Tab = Profile2Tab.POSTS, f: Profile2Features = Profile2Features()) = Profile2Ui(
        profile = me, own = true, stats = profile2Stats(14, 1_530, 212, 23, own = true, hideCounts = false), hasStory = true,
        city = "Hyderabad", learning = "Surah Al-Mulk", tabs = profile2Tabs(true, f.series), tab = tab,
        grid = if (tab == Profile2Tab.POSTS) profilePosts(posts, "me", true) else posts.take(2), totalViews = 18_400, thumbs = thumbs(), avatar = avatar,
        highlights = listOf("Quran", "Ramadan", "Umrah"), deenCard = DeenCardUi(23, "Surah Al-Mulk", 42),
        recap = weeklyDeenRecap(23, emptyList(), 17, 0),
    )
    private fun show(dark: Boolean, content: @Composable () -> Unit) = rule.setContent { MyApplicationTheme(darkTheme = dark) { content() } }

    @Test fun ownLight() { show(false) { NoorProfile2(ownUi(), Profile2Actions()) }; rule.onNodeWithTag("profile2_tab_saved").assertExists(); rule.onNodeWithTag("profile2_verified").assertExists(); rule.shot("profile_own_light") }
    @Test fun ownDark() { show(true) { NoorProfile2(ownUi(), Profile2Actions()) }; rule.shot("profile_own_dark") }
    @Test fun ownSavedTabLight() { show(false) { NoorProfile2(ownUi(Profile2Tab.SAVED), Profile2Actions()) }; rule.shot("profile_saved_light") }
    @Test fun ownAllFlagsLight() { show(false) { NoorProfile2(ownUi(f = Profile2Features.ALL_ON), Profile2Actions(), Profile2Features.ALL_ON) }; rule.onNodeWithTag("profile2_deen_card").assertExists(); rule.shot("profile_own_flags_light") }
    @Test fun ownAllFlagsDark() { show(true) { NoorProfile2(ownUi(f = Profile2Features.ALL_ON).copy(accent = Profile2Accents[1]), Profile2Actions(), Profile2Features.ALL_ON) }; rule.shot("profile_own_flags_dark") }

    private val other = UmmahProfile("o", "Ustadha Maryam", "maryam.learns", "", "Teaching Seerah to kids · Hyderabad\nhttps://youtube.com/@maryam", verified = true)
    @Test fun otherFollowingLight() {
        show(false) { NoorProfile2(Profile2Ui(other, own = false, following = true, stats = profile2Stats(48, 12_400, 90, null, false, false), hasStory = true, tabs = profile2Tabs(false, false), tab = Profile2Tab.REELS, grid = posts.take(5), thumbs = thumbs()), Profile2Actions()) }
        rule.onNodeWithTag("profile2_tab_saved").assertDoesNotExist(); rule.onNodeWithTag("profile2_message").assertExists()
        rule.shot("profile_other_light")
    }
    @Test fun otherPrivateDark() {
        show(true) { NoorProfile2(Profile2Ui(other.copy(isPrivate = true, hideCounts = true, verified = false, bio = "Private notes on tazkiyah"), own = false, stats = profile2Stats(48, 12_400, 90, null, false, true), tabs = profile2Tabs(false, false), locked = true), Profile2Actions()) }
        rule.onNodeWithTag("profile2_locked").assertExists()
        rule.shot("profile_private_locked_dark")
    }

    @Composable private fun sheet(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            NoorProfile2(ownUi(), Profile2Actions())
            Box(Modifier.fillMaxSize().background(Color(0x99000000)))
            Box(Modifier.align(Alignment.BottomCenter)) { content() }
        }
    }
    @Test fun qrLight() { show(false) { sheet { Reels2SheetFrame("Share profile") { Profile2QrCard(me.name, "@tauqeer", profileShareUrl("me"), msgQrBitmap(profileShareUrl("me"), 480)?.asImageBitmap()) } } }; rule.shot("profile_qr_light") }
    @Test fun privacyDark() { show(true) { sheet { Profile2PrivacySheet(true, false, false, 6, Profile2Features.ALL_ON, {}, {}, {}, {}) } }; rule.shot("profile_privacy_dark_flags") }
    @Test fun privacyLight() { show(false) { sheet { Profile2PrivacySheet(false, true, false, 0, Profile2Features(), {}, {}, {}, {}) } }; rule.shot("profile_privacy_light") }

    @Composable private fun edit(status: UsernameStatus, sugg: List<String>, f: Profile2Features = Profile2Features(), err: String? = null) = Profile2EditForm(
        me.name, {}, if (status == UsernameStatus.TAKEN) "aisha.k" else "tauqeer.learns", {}, status, "Learning the Qur'an one page a day.", {},
        sugg, false, err, {}, {}, avatar, "", {}, Profile2Accents[0], {}, {}, f, false, {}, {},
    )
    @Test fun editLightSuggestions() {
        show(false) { edit(UsernameStatus.AVAILABLE, Profile2AiPrompts.parseBios("1. Seeking knowledge, one ayah at a time\n2. Student of the Qur'an from Hyderabad\n3. Grateful learner, sharing what benefits")) }
        rule.onNodeWithTag("profile2_bio_suggestion_2").assertExists()
        rule.shot("profile_edit_ai_light")
    }
    @Test fun editDarkTakenFlags() { show(true) { edit(UsernameStatus.TAKEN, emptyList(), Profile2Features.ALL_ON) }; rule.shot("profile_edit_taken_dark_flags") }
}
