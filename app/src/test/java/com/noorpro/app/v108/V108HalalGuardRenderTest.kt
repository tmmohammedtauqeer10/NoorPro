package com.noorpro.app.v108

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.halal.*
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.components.*
import androidx.compose.ui.graphics.asImageBitmap
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V108HalalGuardRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val now = 1_790_000_000_000L

    private fun img(a: Int, b: Int): Bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawRect(0f, 0f, 200f, 200f, Paint().apply { shader = LinearGradient(0f, 0f, 200f, 200f, a, b, Shader.TileMode.CLAMP) })
    }

    @Before fun setUp() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        HalalTextFilter.load(listOf("src/main/assets/halal/words.json", "app/src/main/assets/halal/words.json").map(::File).first { it.exists() }.readText())
        HalalFlags.override = HalalFlags.ALL_KEYS.associateWith { true }
        UmmahFeedPreview.images = mapOf(
            "https://x/q1.jpg" to img(0xFF7A3B2E.toInt(), 0xFFF0C9A8.toInt()),
            "https://x/q2.jpg" to img(0xFF1B3A5C.toInt(), 0xFF7FB8A4.toInt()),
        )
    }
    @After fun tearDown() { HalalFlags.override = null }

    private fun show(dark: Boolean, content: @Composable () -> Unit) =
        rule.setContent { MyApplicationTheme(darkTheme = dark) { content() } }

    // ---------------------------------------------------------------- guidelines
    @Test fun guidelinesLight() {
        show(false) { HalalGuidelinesScreen(onBack = {}) }
        rule.onNodeWithTag("halal_guidelines").assertExists()
        rule.onNodeWithTag("guideline_0").assertExists()
        rule.saveShot("halal_guidelines_light")
    }

    @Test fun guidelinesDark() {
        show(true) { HalalGuidelinesScreen(onBack = {}) }
        rule.saveShot("halal_guidelines_dark")
    }

    // ---------------------------------------------------------------- report sheet + text pre-check
    @Composable private fun ReportSheet() {
        var reason by remember { mutableStateOf("Wrong Quran / hadith") }
        Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Composer pre-check", color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HalalTextWarning("what the f u c k", Modifier.fillMaxWidth())
                HalalTextWarning("Earn 5000 daily from home, send OTP", Modifier.fillMaxWidth())
                HalalTextWarning("m4d4rch0d", Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(stitchSurface()).padding(18.dp)) {
                Text("Report post", color = stitchText(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(6.dp))
                Text("Why are you reporting this? We review every report within 24 hours.", color = stitchMutedText(), fontSize = 13.sp)
                // AlertDialog supplies the content colour in the app; mirror it here.
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides stitchText()) {
                    UgcReportReasonList(reason) { reason = it }
                }
            }
        }
    }

    @Test fun reportSheetLight() {
        show(false) { ReportSheet() }
        rule.onNodeWithTag("reason_0").assertExists()
        rule.onNodeWithText("Sectarian hate / takfir").assertExists()
        rule.onNodeWithTag("halal_block").assertExists()
        assertEquals(2, rule.onAllNodesWithTag("halal_warning").fetchSemanticsNodes().size)
        rule.saveShot("halal_report_light")
    }

    @Test fun reportSheetDark() {
        show(true) { ReportSheet() }
        rule.saveShot("halal_report_dark")
    }

    // ---------------------------------------------------------------- moderator panel
    private fun panel(tab: ModTab) = ModeratorPanelState().apply {
        this.tab = tab
        queue = listOf(
            ModerationItem("q1", "post", "s1", "u1", "Bilal Ahmed", "image", "Summer beach day", "https://x/q1.jpg", "https://x/q1.jpg",
                listOf("possible_racy"), mapOf("racy" to "LIKELY", "adult" to "UNLIKELY"), now - 40 * 60_000),
            ModerationItem("q2", "post", "s2", "u2", "promo_deals", "image", "Earn 5000 daily from home, DM me", "https://x/q2.jpg", "https://x/q2.jpg",
                listOf("text_scam", "new_account"), emptyMap(), now - 2 * 3_600_000),
            ModerationItem("q3", "comment", "c9", "u3", "anon_77", "comment", "these rafidis again", "", "",
                listOf("text_hate"), emptyMap(), now - 5 * 3_600_000),
        )
        reports = listOf(
            ModerationReport("r1", "post", "p7", "u5", "Wrong Quran / hadith", "Fabricated hadith attributed to Bukhari", "u8", now - 3_600_000),
            ModerationReport("r2", "reel", "p9", "u6", "Immodest / indecent", "", "u9", now - 7_200_000),
        )
        audit = listOf(
            AuditEntry("a1", "system", "auto_remove", "post", "u4", "text_severe", now - 10 * 60_000),
            AuditEntry("a2", "Xk2mAdmin01", "approve", "post", "u1", "", now - 3_600_000),
            AuditEntry("a3", "Xk2mAdmin01", "warn", "comment", "u3", "Sectarian insult", now - 26 * 3_600_000),
            AuditEntry("a4", "Xk2mAdmin01", "ban", "user", "u10", "Repeated scam posts", now - 50 * 3_600_000),
        )
        strikes = mapOf("u1" to StrikeRecord(0), "u2" to StrikeRecord(2), "u3" to StrikeRecord(3, suspendedUntilMs = now + 5 * 86_400_000L))
    }

    @Test fun moderatorQueueLight() {
        show(false) { ModeratorPanel(panel(ModTab.QUEUE), ModeratorActions()) }
        rule.onNodeWithTag("moderator_panel").assertExists()
        rule.onNodeWithTag("modtab_QUEUE").assertTextContains("Queue", substring = true)
        rule.saveShot("halal_modqueue_light")
    }

    @Test fun moderatorQueueDark() {
        show(true) { ModeratorPanel(panel(ModTab.QUEUE), ModeratorActions()) }
        rule.saveShot("halal_modqueue_dark")
    }

    @Test fun moderatorDecisionCallsCallable() {
        val calls = mutableListOf<String>()
        show(false) { ModeratorPanel(panel(ModTab.QUEUE), ModeratorActions(onDecide = { a, t, id, _, s -> calls += "${a.wire}:$t:$id:$s" })) }
        rule.onNodeWithTag("approve_q1").performClick()
        rule.waitForIdle()
        assertEquals("approve:queue:q1:false", calls.firstOrNull())
    }

    @Test fun moderatorReportsAndAudit() {
        show(false) { ModeratorPanel(panel(ModTab.REPORTS), ModeratorActions()) }
        rule.saveShot("halal_modreports_light")
        rule.onNodeWithTag("modtab_AUDIT").performClick()
        rule.saveShot("halal_modaudit_light")
        rule.onNodeWithTag("audit_a4").assertExists()
    }

    @Test fun moderatorAuditDark() {
        show(true) { ModeratorPanel(panel(ModTab.AUDIT), ModeratorActions()) }
        rule.saveShot("halal_modaudit_dark")
    }

    // ---------------------------------------------------------------- safety settings
    @Composable private fun Safety() {
        var s by remember { mutableStateOf(SafetySettings(MessageRequestMode.FOLLOWING, hideNonFollowerComments = true, gender = CircleGender.FEMALE)) }
        Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Safety", color = stitchText(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(10.dp))
            SafetySettingsContent(s, { s = it }, circlesEnabled = true, onGuidelines = {})
            Spacer(Modifier.height(12.dp))
            StitchMessageRequestsRow(false, 3) {}
        }
    }

    @Test fun safetySettingsLight() {
        show(false) { Safety() }
        rule.onNodeWithTag("safety_settings").assertExists()
        rule.onNodeWithTag("msgreq_FOLLOWING").assertExists()
        rule.saveShot("halal_safety_light")
    }

    @Test fun safetySettingsDark() {
        show(true) { Safety() }
        rule.saveShot("halal_safety_dark")
    }

    // ---------------------------------------------------------------- blocked / queued post + blurred media
    @Composable private fun Notices() {
        Column(Modifier.fillMaxSize().background(stitchBackground()).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HalalBlockedNoticeCard("This post contains language that breaks the Halal community guidelines.", {}, {}, Modifier.fillMaxWidth())
            HalalBlockedNoticeCard("A moderator will check it shortly, usually within a few hours.", {}, {}, Modifier.fillMaxWidth(), queued = true)
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp))) {
                SensitiveMedia(true, "demo", Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(UmmahFeedPreview.images["https://x/q1.jpg"]!!.asImageBitmap(), null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    @Test fun blockedPostLight() {
        show(false) { Notices() }
        rule.onAllNodesWithTag("halal_blocked_notice").assertCountEquals(2)
        rule.onNodeWithText("Your post wasn't published").assertExists()
        rule.onNodeWithText("Your post is being reviewed").assertExists()
        rule.saveShot("halal_blocked_light")
    }

    @Test fun blockedPostDark() {
        show(true) { Notices() }
        rule.saveShot("halal_blocked_dark")
    }
}
