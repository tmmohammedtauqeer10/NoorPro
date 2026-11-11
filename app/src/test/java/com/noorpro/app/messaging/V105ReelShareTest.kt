package com.noorpro.app.messaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.UmmahMessage
import com.noorpro.app.data.messaging.*
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 1.1.3 sample: reel/post sharing into Messages — share sheet + shared cards, light + dark. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V105ReelShareTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun images() {
        UmmahFeedPreview.offline = true
        UmmahFeedPreview.images = mapOf(
            "img://mosque" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_hero_mosque),
            "img://rehal" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_quran_rehal),
            "img://p1" to art(0xFFE6F2EC.toInt(), 0xFF0B6243.toInt()), "img://p2" to art(0xFFFDF1D6.toInt(), 0xFF7A4E9E.toInt()),
            "img://p3" to art(0xFFF3E6C4.toInt(), 0xFF1F6FA3.toInt()),
        )
    }

    /** Abstract generated avatar art (no real people). */
    private fun art(bg: Int, robe: Int): Bitmap {
        val b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888); val c = Canvas(b); c.drawColor(bg)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = robe }
        c.drawOval(android.graphics.RectF(40f, 170f, 216f, 330f), p); c.drawCircle(128f, 112f, 58f, p)
        p.color = 0xFFF1D7B8.toInt(); c.drawCircle(128f, 118f, 38f, p); return b
    }

    // Built from a real 1.0.82 post_share message so the card uses the live data model.
    private val reel = MsgShared.from(UmmahMessage(
        id = "m_reel", senderUid = "u1", senderName = "Amina Yusuf", text = "", mediaUrl = "", type = "post_share", createdAt = 0,
        sharedPostId = "reel_rahman", sharedPostKind = "reel", sharedThumb = "img://mosque", sharedCreatorUid = "c9",
        sharedCreatorName = "Noor Recitations", sharedCreatorPhoto = "", sharedCaption = "Surah Ar-Rahman at Fajr — which of the favours of your Lord will you deny? #Quran"
    )).copy(durationLabel = "0:48")
    private val post = MsgShared("post_adhkar", "post", "img://rehal", "c8", "Daily Adhkar", "", "Morning adhkar checklist — save it for after Fajr 🌅")

    private fun show(dark: Boolean, content: @Composable () -> Unit) = rule.setContent {
        MyApplicationTheme(darkTheme = dark) { Box(Modifier.fillMaxSize().background(stitchBackground())) { content() } }
    }
    private fun save(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        val out = File(File("build/v105").apply { mkdirs() }, "$name.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name not rendered", out.length() > 5000)
    }
    private fun tags(vararg t: String) = t.forEach { rule.onNode(hasTestTag(it), useUnmergedTree = true).assertExists() }

    private fun sheet(query: String = "", selected: Set<String> = setOf("u1", "g1", "u2"), note: String = "SubhanAllah, listen to this 🤍") =
        ShareSheetState(reel, MessagingSamples.shareTargets(), query, selected, note)

    private val direct = DirectChatState("Amina Yusuf", "amina.reflects", "img://p1", online = true, lines = listOf(
        MsgLine("r1", MsgLineType.SHARED, text = "Listen to this recitation 😭 SubhanAllah", time = "6:12 AM", shared = reel, reactions = "🤲 2  ❤️ 1"),
        MsgLine("r2", mine = true, text = "Ma sha Allah, his voice! Saving it for tonight", time = "6:15 AM", replyTo = "Reel · Noor Recitations" to "Surah Ar-Rahman at Fajr"),
    ))
    private val directPost = DirectChatState("Bilal Hassan", "bilal.h", "", online = true, lines = listOf(
        MsgLine("p0", MsgLineType.DAY, text = "TODAY"),
        MsgLine("p1", mine = true, text = "Assalamu alaikum akhi, how are the kids?", time = "8:40 AM"),
        MsgLine("p2", text = "Wa alaikum assalam! Alhamdulillah. Have you seen this?", time = "8:44 AM"),
        MsgLine("p3", MsgLineType.SHARED, text = "Print this for the kids 🌅", time = "8:45 AM", shared = post, firstInRun = false, reactions = "👍 1"),
        MsgLine("p4", mine = true, text = "JazakAllahu khayran, printing now", time = "8:47 AM", replyTo = "Post · Daily Adhkar" to "Morning adhkar checklist"),
    ))
    private val groupChat = GroupChatState(MessagingSamples.groups()[0], lines = listOf(
        MsgLine("g2", MsgLineType.SHARED, senderName = "Bilal Hassan", text = "Tonight's halaqah warm-up 👇", time = "7:20 AM", shared = reel, reactions = "🤲 8  ❤️ 5"),
        MsgLine("g3", text = "Ameen, JazakAllahu khayran", time = "7:22 AM", senderName = "Amina Yusuf", senderRole = GroupRole.ADMIN),
    ))

    @Test fun sheetLight() { show(false) { NoorShareSheet(sheet()) }; tags("msg_share_sheet", "msg_share_search", "msg_share_recent", "msg_share_note", "msg_share_send", "msg_share_actions", "msg_share_t_g1"); save("11_share_sheet_light") }
    @Test fun sheetDark() { show(true) { NoorShareSheet(sheet()) }; tags("msg_share_sheet", "msg_share_send"); save("11_share_sheet_dark") }
    @Test fun sheetEmptyLight() { show(false) { NoorShareSheet(sheet(selected = emptySet(), note = "")) }; rule.onNodeWithTag("msg_share_note").assertDoesNotExist(); save("12_share_empty_light") }
    @Test fun sheetSearchDark() { show(true) { NoorShareSheet(sheet(query = "Fajr", selected = setOf("g1"), note = "")) }; tags("msg_share_t_g1"); save("12_share_search_dark") }
    @Test fun directLight() { show(false) { NoorDirectChat(direct) }; tags("msg_shared_reel_rahman"); save("13_direct_reel_light") }
    @Test fun directDark() { show(true) { NoorDirectChat(direct) }; tags("msg_shared_reel_rahman"); save("13_direct_reel_dark") }
    @Test fun groupLight() { show(false) { NoorGroupChat(groupChat) }; tags("msg_shared_reel_rahman"); save("14_group_reel_light") }
    @Test fun postLight() { show(false) { NoorDirectChat(directPost) }; tags("msg_shared_post_adhkar"); save("15_post_card_light") }
    @Test fun postDark() { show(true) { NoorDirectChat(directPost) }; tags("msg_shared_post_adhkar"); save("15_post_card_dark") }
    @Test fun groupDark() { show(true) { NoorGroupChat(groupChat) }; tags("msg_shared_reel_rahman"); save("14_group_reel_dark") }

    @Test fun tappingCardOpensReel() {
        var opened: MsgShared? = null
        show(false) { NoorDirectChat(direct) { opened = it } }
        rule.onNodeWithTag("msg_shared_reel_rahman", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertEquals("reel_rahman", opened?.postId)
    }

    @Test fun tappingTargetToggles() {
        val toggled = mutableListOf<String>()
        show(false) { NoorShareSheet(sheet(selected = emptySet(), note = ""), onToggle = { toggled += it.id }) }
        rule.onNodeWithTag("msg_share_t_u1", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertEquals(listOf("u1"), toggled)
    }
}
