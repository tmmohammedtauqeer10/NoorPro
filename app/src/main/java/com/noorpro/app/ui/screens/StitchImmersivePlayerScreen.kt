package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.components.StitchDarkMuted
import com.noorpro.app.ui.components.StitchDarkSurface
import com.noorpro.app.ui.components.StitchEmeraldDeep
import com.noorpro.app.ui.components.StitchGold
import com.noorpro.app.ui.components.StitchLine
import com.noorpro.app.ui.components.StitchMuted
import com.noorpro.app.ui.components.isStitchLight
import com.noorpro.app.ui.components.stitchBackground
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.theme.LibreCaslon
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

/**
 * Immersive "Quiet Devotion" audio player matching the Islamic Faith Hub Stitch design.
 * The artwork is drawn in Compose per surah (crisp at any size — never blurry) and shows that
 * surah's own Arabic name, so every recitation gets its own artwork. A frosted-glass panel holds
 * the timeline and controls, all wired to the shared ViewModel — this is the NOW_PLAYING route.
 */
@Composable
fun StitchImmersivePlayerScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val light = isStitchLight()

    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val isShuffle by viewModel.isShuffleEnabled.collectAsState()
    val isRepeat by viewModel.isRepeatEnabled.collectAsState()
    val progress by viewModel.audioProgress.collectAsState()
    val position by viewModel.audioPosition.collectAsState()
    val duration by viewModel.audioDuration.collectAsState()
    val speed by viewModel.audioPlaybackSpeed.collectAsState()
    val surah by viewModel.playingSurah.collectAsState()
    val reciter by viewModel.selectedReciter.collectAsState()

    val title = surah?.nameEnglish ?: "Ar-Rahman"
    val arabicName = surah?.nameArabic ?: "الرَّحْمَٰن"
    val ayahCount = surah?.versesCount?.takeIf { it > 0 } ?: 78
    val badge = if ((surah?.type ?: "Meccan").startsWith("Med", ignoreCase = true)) "Madani" else "Makki"

    val scrimEnd = stitchBackground()
    val glass = if (light) Color.White.copy(alpha = 0.72f) else StitchDarkSurface.copy(alpha = 0.80f)
    val glassBorder = if (light) Color.White.copy(alpha = 0.5f) else StitchLine.copy(alpha = 0.3f)

    // Clean, sharp gradient backdrop (no blurred photo).
    val backdrop = Brush.verticalGradient(
        listOf(
            if (light) Color(0xFFF4EFE4) else Color(0xFF0C1A15),
            scrimEnd,
            scrimEnd
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(backdrop)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircleButton(Icons.Default.KeyboardArrowDown, "Close", glass, glassBorder) {
                    if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DASHBOARD)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(glass)
                        .border(1.dp, glassBorder, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StitchGold))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "NOW PLAYING",
                        color = stitchMutedText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }
                GlassCircleButton(Icons.Default.MoreHoriz, "More", glass, glassBorder) {
                    viewModel.navigateTo(DeenScreen.AUDIO_LIBRARY)
                }
            }

            // Artwork + meta
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SurahArtwork(
                    arabicName = arabicName,
                    englishName = title,
                    ayahCount = ayahCount,
                    light = light
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    title,
                    color = stitchText(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LibreCaslon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(reciter.name, color = stitchMutedText(), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(stitchPrimary().copy(alpha = 0.07f))
                        .border(1.dp, stitchPrimary().copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Mosque, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(badge, color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Glass control panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(glass)
                    .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                // Timeline
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = { viewModel.seekAudio(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = stitchPrimary(),
                        activeTrackColor = StitchGold,
                        inactiveTrackColor = stitchMutedText().copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(position.ifBlank { "0:00" }, color = stitchMutedText(), fontSize = 13.sp)
                    Text(duration.ifBlank { "0:00" }, color = stitchMutedText(), fontSize = 13.sp)
                }

                Spacer(Modifier.height(14.dp))

                // Main controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlIcon(
                        Icons.Default.Shuffle, "Shuffle", 26.dp,
                        tint = if (isShuffle) stitchPrimary() else stitchMutedText()
                    ) { viewModel.toggleShuffleMode() }
                    ControlIcon(Icons.Default.SkipPrevious, "Previous", 34.dp, tint = stitchText()) {
                        viewModel.playPreviousSurah()
                    }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(stitchPrimary())
                            .clickable { viewModel.toggleAudioPlayback() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    ControlIcon(Icons.Default.SkipNext, "Next", 34.dp, tint = stitchText()) {
                        viewModel.playNextSurah()
                    }
                    ControlIcon(
                        Icons.Default.Repeat, "Repeat", 26.dp,
                        tint = if (isRepeat) stitchPrimary() else stitchMutedText()
                    ) { viewModel.toggleRepeatMode() }
                }

                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(glassBorder))
                Spacer(Modifier.height(14.dp))

                // Secondary controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SecondaryAction(Icons.Default.Snooze, "Sleep") {
                        Toast.makeText(context, "Sleep timer coming soon", Toast.LENGTH_SHORT).show()
                    }
                    SecondaryAction(Icons.Default.Speed, formatSpeed(speed)) {
                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val next = speeds[(speeds.indexOf(speed).coerceAtLeast(0) + 1) % speeds.size]
                        viewModel.setAudioPlaybackSpeed(next)
                    }
                    SecondaryAction(Icons.Default.Subtitles, "Lyrics") {
                        surah?.let { viewModel.selectSurah(it) }
                    }
                }
            }
        }
    }
}

/**
 * Per-surah artwork drawn entirely in Compose — always crisp (no raster scaling / blur). An
 * ornamental cream/emerald frame with the Bismillah, the surah's own Arabic name, a gold flourish,
 * and the English name + ayah range. Every surah therefore shows its own distinct artwork.
 */
@Composable
private fun SurahArtwork(arabicName: String, englishName: String, ayahCount: Int, light: Boolean) {
    val panel = if (light) {
        Brush.linearGradient(listOf(Color(0xFFFBF7EF), Color(0xFFF0E8D8)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF12241D), Color(0xFF0A1712)))
    }
    val nameColor = if (light) StitchEmeraldDeep else Color(0xFFF3E9CC)
    val muted = if (light) StitchMuted else StitchDarkMuted
    val frame = StitchGold

    Box(
        modifier = Modifier
            .size(288.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(panel)
            .border(1.dp, frame.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
    ) {
        // Inner ornamental frame line.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .border(1.dp, frame.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                color = frame,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(26.dp))
            Text(
                "سُورَةُ $arabicName",
                color = nameColor,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(20.dp))
            // Gold flourish: line — diamond — line.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(44.dp).height(1.5.dp).background(frame.copy(alpha = 0.7f)))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.size(7.dp).rotate(45f).background(frame))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.width(44.dp).height(1.5.dp).background(frame.copy(alpha = 0.7f)))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                englishName,
                color = nameColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LibreCaslon,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text("Ayah 1-$ayahCount", color = muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    glass: Color,
    glassBorder: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(glass)
            .border(1.dp, glassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = stitchText(), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun SecondaryAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = stitchMutedText(), modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = stitchMutedText(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Formats a playback speed like 1.0 → "1.0x", 1.25 → "1.25x". */
private fun formatSpeed(speed: Float): String {
    val s = if (speed % 1f == 0f) "${speed.toInt()}.0" else speed.toString()
    return "${s}x"
}
