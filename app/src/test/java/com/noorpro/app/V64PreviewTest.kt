package com.noorpro.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.noorpro.app.data.UmmahPost
import com.noorpro.app.ui.components.StitchScreen
import com.noorpro.app.ui.screens.*
import com.noorpro.app.ui.theme.MyApplicationTheme
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.ui.viewmodel.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 1.0.64 previews -> build/v64/v64_<screen>_<light|dark>.png (frames from /workspace/store_assets/raw when present). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class V64PreviewTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dark = mutableStateOf(false)

    private fun both(name: String, content: @Composable () -> Unit) {
        rule.setContent { MyApplicationTheme(darkTheme = dark.value) { content() } }
        for (d in listOf(false, true)) {
            dark.value = d; rule.waitForIdle(); Thread.sleep(600); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
            rule.onRoot().captureRoboImage(filePath = "build/v64/v64_${name}_${if (d) "dark" else "light"}.png")
        }
    }

    private fun frame(path: String) = runCatching { android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()

    private fun post(id: String, type: String, caption: String, arabic: String = "", likes: Long = 0, comments: Long = 0, shares: Long = 0, views: Long = 0, w: Int = 1080, h: Int = 1920, media: String = "") = UmmahPost(
        id = id, type = type, mediaUrl = media, thumbnailUrl = "", creatorUid = "u_$id", creatorName = "Noor Reminders", creatorHandle = "@noor.reminders",
        caption = caption, arabicText = arabic, sourceReference = "", publishedAt = System.currentTimeMillis() - 3_600_000L * 3,
        likeCount = likes, commentCount = comments, shareCount = shares, viewCount = views, saveCount = 210, mediaWidth = w, mediaHeight = h,
    )

    @Composable
    private fun reel(p: UmmahPost, aspect: Float) = Box(Modifier.fillMaxSize().background(Color.Black)) {
        StitchLiveReelPage(post = p, displayName = "Guest", isFollowing = false, isOwnPost = false, isLiked = true, isSaved = false,
            displayLikeCount = p.likeCount, displayCommentCount = p.commentCount, displaySaveCount = 210, displayShareCount = p.shareCount, displayViewCount = p.viewCount,
            isActive = true, isPlaying = true, playbackFailed = false, videoAspectRatio = aspect, landscapeMode = false, player = null,
            onFollow = {}, onLike = {}, onComment = {}, onSave = {}, onShare = {}, onOpenProfile = {}, onDoubleTapLike = {},
            onLandscapeModeChange = {}, onReport = {}, onRetry = {}, onTogglePlay = {})
    }

    @Test fun reelPortrait() {
        ReelPreviewFrame.frame = frame("/workspace/store_assets/raw/reel_masjid.jpg")
        val p = post("r1", "reel", "Find peace in the remembrance of Allah 🤍 #dhikr #peace", "أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ", 1240, 86, 120, 9800)
        both("reels_portrait") { reel(p, 9f / 16f) }
    }

    @Test fun reelLandscape() {
        ReelPreviewFrame.frame = frame("/workspace/store_assets/raw/reel_wide.jpg")
        val p = post("r2", "reel", "Sunset over the masjid — a moment to reflect. #reflection #masjid", "سُبْحَانَ ٱللَّٰهِ", 860, 42, 75, 6400, 1920, 1080)
        both("reels_landscape") { reel(p, 16f / 9f) }
    }

    @Test fun optionsSheet() = both("reels_options") {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.BottomCenter) {
            ReelOptionsSheetContent(stitchReelQualityOptions.first(), 1f, true, false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, Modifier.fillMaxWidth())
        }
    }

    @Test fun ummahFeed() = both("ummah") {
        StitchScreen {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StitchComposerCard("G") {}
                StitchRealPostCard(post("p1", "text", "Alhamdulillah for another Jummah. May Allah accept our prayers and grant us sincerity. 🤲", likes = 124, comments = 18, shares = 6).copy(category = "Reflection", creatorHandle = "@aisha.reflects"), onOpenReel = {})
                StitchRealPostCard(post("p2", "text", "Reminder: the Prophet ﷺ said, “The most beloved deeds to Allah are those done consistently, even if small.” (Bukhari 6464)", likes = 342, comments = 27, shares = 41).copy(category = "Hadith", creatorHandle = "@daily.hadith"), isLiked = true, onOpenReel = {})
            }
        }
    }

    @Test fun profile() {
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, Configuration.Builder().build())
        ctx.getSharedPreferences("onboarding", android.content.Context.MODE_PRIVATE).edit().putBoolean("permissions_intro_complete", true).putBoolean("media_notif_asked", true).putBoolean("exact_alarm_asked", true).commit()
        com.noorpro.app.prayer.PrayerPrefs(ctx).permissionPromptSnoozeUntil = System.currentTimeMillis() + 86_400_000L
        rule.setContent {
            val vm: DeenViewModel = viewModel()
            LaunchedEffect(dark.value) { vm.updateThemeMode(if (dark.value) ThemeMode.DARK else ThemeMode.LIGHT) }
            LaunchedEffect(Unit) { vm.openCreatorProfile("u_demo_creator", "Aisha Rahman", "@aisha.reflects") }
            MyApplicationTheme(darkTheme = dark.value) { MainLayout() }
        }
        for (d in listOf(false, true)) {
            dark.value = d; rule.waitForIdle(); Thread.sleep(800); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
            rule.onRoot().captureRoboImage(filePath = "build/v64/v64_profile_${if (d) "dark" else "light"}.png")
        }
    }
}
