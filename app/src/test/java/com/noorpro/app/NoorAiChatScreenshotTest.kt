package com.noorpro.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.data.NoorAiMode
import com.noorpro.app.ui.screens.NoorAiChatContent
import com.noorpro.app.ui.screens.NoorAiChatItem
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class NoorAiChatScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val answer = """
        Renewing your intention keeps every deed for Allah alone.

        ### Renew Your Intention
        - **Before acting:** pause and remind yourself why you do it.
        - **During:** remember that *Allah sees you*.

        ### Dua
        - **Arabic:** اللَّهُمَّ إِنِّي أَسْأَلُكَ الْإِخْلَاصَ
        - **Meaning:** O Allah, I ask You for sincerity.

        > "Actions are only by intentions." (Sahih al-Bukhari 1)

        ---
        Allah knows best.

        Sources:
        - Sahih al-Bukhari 1
        - Al-Bayyinah 98:5
    """.trimIndent()

    private fun shot(name: String, items: List<NoorAiChatItem>, typing: Boolean, tip: Boolean) {
        rule.setContent {
            MyApplicationTheme {
                NoorAiChatContent(
                    mode = NoorAiMode.AskNoor, messages = items, input = "", isTyping = typing, showTip = tip, revealId = null,
                    onBack = {}, onInput = {}, onSend = {}, onSuggestion = {}, onStop = {}, onModeChange = {}, onClearChat = {},
                    onDismissTip = {}, onCopy = {}, onShare = {}, onRegenerate = {}, onFeedback = { _, _ -> }, onRevealDone = {},
                )
            }
        }
        rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
    }

    @Test fun chat() = shot("noorai_chat", listOf(
        NoorAiChatItem("How do I keep my intention sincere?", true, 1_760_000_000_000L, id = 1),
        NoorAiChatItem(answer, false, 1_760_000_060_000L, feedback = 1, id = 2),
    ), typing = false, tip = true)

    @Test fun empty() = shot("noorai_empty", emptyList(), typing = false, tip = true)
}
