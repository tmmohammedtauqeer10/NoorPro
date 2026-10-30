package com.noorpro.app.messaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.R
import com.noorpro.app.data.messaging.*
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 1.1.3 sample: NoorPro messaging screens rendered light + dark from real Compose code. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class V105MessagingTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    /** Abstract, generated avatar art (no real people). */
    private fun avatarArt(c1: Int, c2: Int, robe: Int): Bitmap {
        val s = 256; val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888); val cv = Canvas(b)
        cv.drawRect(0f, 0f, s.toFloat(), s.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, s.toFloat(), s.toFloat(), c1, c2, Shader.TileMode.CLAMP) })
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = robe }
        cv.drawOval(RectF(40f, 170f, 216f, 330f), p)
        cv.drawCircle(128f, 112f, 58f, p)
        p.color = 0xFFF1D7B8.toInt(); cv.drawCircle(128f, 118f, 38f, p)
        return b
    }

    @Before fun images() {
        UmmahFeedPreview.offline = true
        UmmahFeedPreview.images = mapOf(
            "img://mosque" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_hero_mosque),
            "img://rehal" to BitmapFactory.decodeResource(ctx.resources, R.drawable.noor_home_quran_rehal),
            "img://p1" to avatarArt(0xFFE6F2EC.toInt(), 0xFFB9DCCB.toInt(), 0xFF0B6243.toInt()),
            "img://p2" to avatarArt(0xFFFDF1D6.toInt(), 0xFFE8CF8E.toInt(), 0xFF7A4E9E.toInt()),
            "img://p3" to avatarArt(0xFFF3E6C4.toInt(), 0xFFD9C38A.toInt(), 0xFF1F6FA3.toInt()),
        )
    }

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

    private fun home(tab: MsgHomeTab, fab: Boolean = false) = MessagesHomeState(
        tab = tab, chats = MessagingSamples.chats(), groups = MessagingSamples.groups(), discover = MessagingSamples.discover(),
        moment = IslamicTouches.Moment.JUMUAH, salahHint = "Asr in 24 min", fabOpen = fab)

    private val direct = DirectChatState(
        "Amina Yusuf", "amina.reflects", "img://p1", online = true, salahHint = "Maghrib in 18 min — replies can wait",
        lines = listOf(
            MsgLine("d0", MsgLineType.DAY, text = "TODAY · 10 JUMADA AL-AWWAL"),
            MsgLine("m1", mine = true, text = "Assalamu alaikum Amina! How was the tajweed class?", time = "4:02 PM"),
            MsgLine("m2", text = "Wa alaikum assalam wa rahmatullah 🌿 Alhamdulillah, we finished the rules of noon saakin.", time = "4:05 PM"),
            MsgLine("m3", MsgLineType.VOICE, text = "", time = "4:06 PM", durationMs = 42_000, firstInRun = false),
            MsgLine("m4", mine = true, text = "MashaAllah! Can you share the notes?", time = "4:08 PM", replyTo = "Amina" to "we finished the rules of noon saakin", reactions = "🤲 1"),
            MsgLine("m5", MsgLineType.DUA, text = "Rabbi zidni ilma — My Lord, increase me in knowledge.", time = "4:10 PM"),
            MsgLine("m6", text = "Assalamu alaikum, are you joining the halaqah tonight?", time = "4:12 PM"),
        ))

    private val groupChat = GroupChatState(
        MessagingSamples.groups()[0], typingNames = emptyList(),
        lines = listOf(
            MsgLine("g0", MsgLineType.DAY, text = "TODAY"),
            MsgLine("g1", MsgLineType.SYSTEM, text = "Hafsa joined with an invite link"),
            MsgLine("g2", text = "Assalamu alaikum everyone! Today's ayah: Al-Mulk 67:2 🌙", time = "6:10 AM", senderName = "Amina Yusuf", senderPhoto = "img://p1", senderRole = GroupRole.ADMIN, reactions = "❤️ 12  🤲 8"),
            MsgLine("g3", MsgLineType.IMAGE, text = "Our circle after Fajr, alhamdulillah", time = "6:42 AM", senderName = "Bilal Hassan", imageUrl = "img://mosque"),
            MsgLine("g4", MsgLineType.VOICE, time = "6:50 AM", senderName = "Khadija Rahman", durationMs = 75_000),
            MsgLine("g5", mine = true, text = "JazakAllahu khayran! I'll bring the tajweed charts tonight.", time = "7:02 AM", replyTo = "Amina Yusuf" to "Today's ayah: Al-Mulk 67:2"),
            MsgLine("g6", text = "Ameen. See you after Isha in sha Allah", time = "7:05 AM", senderName = "Sumayyah Khan", senderPhoto = "img://p2"),
        ))

    private val info get() = GroupInfoState(MessagingSamples.groups()[0], MessagingSamples.groupMembers, joinRequests = 3, inviteCode = "QURAN-7KD2")
    private val create get() = CreateGroupState(
        name = "Juz 30 Hifz Circle", description = "Memorise one surah a week, revise together.", topicId = "quran",
        visibility = GroupVisibility.PUBLIC_REQUEST, candidates = MessagingSamples.people, selected = setOf("u1", "u3", "u8"))
    private val discover get() = DiscoverState(MessagingSamples.discover(), topicId = "quran")
    private fun find(q: String) = FindState(q, chats = MessagingSamples.chats(), people = MessagingSamples.people, groups = MessagingSamples.discover())

    @Test fun homeChatsLight() { show(false) { NoorMessagesHome(home(MsgHomeTab.CHATS)) }; tags("msg_home", "msg_tabs", "msg_moment", "msg_chat_c1", "msg_fab"); save("01_home_chats_light") }
    @Test fun homeChatsDark() { show(true) { NoorMessagesHome(home(MsgHomeTab.CHATS)) }; tags("msg_home", "msg_chat_c1"); save("01_home_chats_dark") }
    @Test fun homeGroupsLight() { show(false) { NoorMessagesHome(home(MsgHomeTab.GROUPS, fab = true)) }; tags("msg_group_g1", "msg_fab_menu"); save("02_home_groups_light") }
    @Test fun homeGroupsDark() { show(true) { NoorMessagesHome(home(MsgHomeTab.GROUPS, fab = true)) }; tags("msg_group_g1", "msg_fab_menu"); save("02_home_groups_dark") }
    @Test fun directLight() { show(false) { NoorDirectChat(direct) }; tags("msg_direct_chat", "msg_quick_replies", "msg_composer"); save("03_direct_light") }
    @Test fun directDark() { show(true) { NoorDirectChat(direct) }; tags("msg_direct_chat", "msg_quick_replies"); save("03_direct_dark") }
    @Test fun groupChatLight() { show(false) { NoorGroupChat(groupChat) }; tags("msg_group_chat", "msg_pinned_note", "msg_system"); save("04_group_chat_light") }
    @Test fun groupChatDark() { show(true) { NoorGroupChat(groupChat) }; tags("msg_group_chat", "msg_pinned_note"); save("04_group_chat_dark") }
    @Test fun groupInfoLight() { show(false) { NoorGroupInfo(info) }; tags("msg_group_info", "msg_invite_card"); save("05_group_info_light") }
    @Test fun groupInfoDark() { show(true) { NoorGroupInfo(info) }; tags("msg_group_info", "msg_invite_card"); save("05_group_info_dark") }
    @Test fun createLight() { show(false) { NoorCreateGroup(create) }; tags("msg_create_group", "msg_create_button", "msg_topic_quran"); save("06_create_light") }
    @Test fun createDark() { show(true) { NoorCreateGroup(create) }; tags("msg_create_group", "msg_create_button"); save("06_create_dark") }
    @Test fun discoverLight() { show(false) { NoorDiscoverGroups(discover) }; tags("msg_discover", "msg_featured"); assertNoPrivate(); save("07_discover_light") }
    @Test fun discoverDark() { show(true) { NoorDiscoverGroups(discover) }; tags("msg_discover", "msg_featured"); save("07_discover_dark") }
    @Test fun findLight() { show(false) { NoorFindPeople(find("Amina")) }; tags("msg_find", "msg_best_u9"); save("08_find_light") }
    @Test fun findDark() { show(true) { NoorFindPeople(find("Amina")) }; tags("msg_find", "msg_best_u9"); save("08_find_dark") }
    @Test fun findSuggestLight() { show(false) { NoorFindPeople(find("")) }; tags("msg_add_friend"); save("09_find_suggest_light") }
    @Test fun findSuggestDark() { show(true) { NoorFindPeople(find("")) }; tags("msg_add_friend"); save("09_find_suggest_dark") }
    @Test fun homeDiscoverLight() { show(false) { NoorMessagesHome(home(MsgHomeTab.DISCOVER)) }; tags("msg_topic_quran"); save("10_home_discover_light") }

    private fun tags(vararg t: String) = t.forEach { rule.onNode(hasTestTag(it), useUnmergedTree = true).assertExists() }
    private fun assertNoPrivate() = rule.onNodeWithTag("msg_discover_g3").assertDoesNotExist()
}
