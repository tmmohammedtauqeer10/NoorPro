package com.noorpro.app.v117

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.data.UmmahProfile
import com.noorpro.app.data.onboarding.*
import com.noorpro.app.islamic.social.CardContent
import com.noorpro.app.islamic.social.CardKind
import com.noorpro.app.ui.islamic.ShareCardsActions
import com.noorpro.app.ui.islamic.ShareCardsContent
import com.noorpro.app.ui.islamic.ShareCardsUi
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 1.2.0 (117) previews -> build/v117/<name>.png (6 key screens only). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class PreviewV117ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val content = mutableStateOf<@Composable () -> Unit>({})
    private val gen = mutableIntStateOf(0)
    private var started = false

    private fun show(c: @Composable () -> Unit) {
        content.value = c; gen.intValue++
        if (!started) { started = true; rule.setContent { MyApplicationTheme(darkTheme = false) { key(gen.intValue) { content.value() } } } }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
    }
    private fun cap(name: String) { rule.waitForIdle(); rule.onRoot().captureRoboImage(filePath = "build/v117/$name.png") }

    private val today = SimpleDate(2026, 11, 11)
    private val draft = OnboardingDraft(fullName = "Mohammed Tauqeer", username = "tauqeer", reservedUsername = "tauqeer", gender = OnbGender.MALE,
        profileType = ProfileType.STUDENT, dob = SimpleDate(1998, 4, 2), country = "India", state = "Telangana", city = "Hyderabad", language = "ur")

    @Test fun keyScreens() {
        // 1. Profile › Edit profile with the sign-up details (private).
        show {
            Profile2EditForm(name = draft.fullName, onName = {}, username = draft.username, onUsername = {}, status = UsernameStatus.UNCHANGED,
                bio = "Student of tafsir · Hyderabad", onBio = {}, suggestions = emptyList(), aiLoading = false, aiError = null, onAskAi = {}, onPick = {},
                avatar = null, photoUrl = "", onPhoto = {}, accent = R2Green, onAccent = {}, onCover = {}, f = Profile2Features(extendedFields = false),
                saving = false, onSave = {}, onBack = {}, details = { ProfileDetailsEditor(draft, today, onChange = {}) })
        }
        rule.onNodeWithTag("profile_dob_year").performScrollTo(); cap("1_profile_edit_details")

        // 2. Another member's profile: no phone number anywhere.
        val other = UmmahProfile("u2", "Aisha Siddiqui", "aisha.s", "", "Qur'an teacher · Hyderabad")
        show { NoorProfile2(Profile2Ui(profile = other, own = false, stats = profile2Stats(8, 412, 120, null, own = false, hideCounts = false),
            tabs = profile2Tabs(false, false), tab = Profile2Tab.POSTS, grid = emptyList()), Profile2Actions()) }
        assertEquals(0, rule.onAllNodesWithText("Phone", substring = true).fetchSemanticsNodes().size)
        cap("2_other_profile_no_phone")

        // 3. Noor AI chat: Fast by default, no madhhab picker on the screen, answer streaming in.
        show {
            NoorAiChatContent(mode = com.noorpro.app.data.NoorAiMode.AskNoor,
                messages = listOf(NoorAiChatItem("حج کے فرائض کیا ہیں؟", true, 1_760_000_000_000L, id = 1)), input = "", isTyping = true, showTip = false, revealId = null,
                onBack = {}, onInput = {}, onSend = {}, onSuggestion = {}, onStop = {}, onModeChange = {}, onClearChat = {},
                onDismissTip = {}, onCopy = {}, onShare = {}, onRegenerate = {}, onFeedback = { _, _ -> }, onRevealDone = {},
                tier = com.noorpro.app.data.NoorAiOptions.FAST,
                streamingText = "حج کے بنیادی فرائض (ارکان) تین ہیں:\n\n**۱. احرام:** حج کی نیت کے ساتھ احرام باندھنا اور تلبیہ پڑھنا۔\n**۲. وقوفِ عرفات:** ۹ ذوالحجہ کو عرفات میں")
        }
        assertEquals(0, rule.onAllNodesWithText("Hanafi", substring = true).fetchSemanticsNodes().size)
        cap("3_noorai_fast_stream_no_fiqh_picker")

        // 4. Al-Noor Audio player with a Barakah Melodies track and its credit.
        show {
            NpFull(SpTheme(false), NpState("Tala'al Badru Alayna", "Barakah Melodies", null, null, BarakahTint, true, 0.34f, "1:12", "3:31",
                false, false, emptyList(), 0, "Barakah Melodies", true, attribution = com.noorpro.app.audio.catalog.BarakahMelodies.CREDIT), NpActions(), Modifier) {}
        }
        cap("4_alnoor_player_barakah_credit")

        // 5. Barakah Melodies section: credit, terms, Spotify link, files arriving.
        show {
            val t = SpTheme(true)
            LazyColumn(Modifier.fillMaxSize().background(t.bg)) {
                item { Box(Modifier.fillMaxSize()) { BarakahShelfCard(t) {} } }
                barakahPageItems(t) {}
            }
        }
        cap("5_barakah_melodies_section")

        // 6. Share card: selectable text, Copy, and a caption you can paste into.
        val c = CardContent(CardKind.AYAH, "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "Allah does not burden a soul beyond what it can bear.", "Qur'an 2:286", "2:286")
        show { ShareCardsContent(ShareCardsUi(ref = "2:286", content = c), null, emptyMap(), ShareCardsActions()) }
        rule.onNodeWithTag("cards_caption").performScrollTo(); cap("6_share_card_copy_paste")
    }
}
