package com.noorpro.app.audio

import android.content.Context
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.audio.models.LicenseType
import com.noorpro.app.audio.models.RepeatMode
import com.noorpro.app.audio.models.Track
import com.noorpro.app.audio.player.AlNoorMediaItems
import com.noorpro.app.audio.player.AlNoorPlayer
import com.noorpro.app.audio.session.AlNoorMediaSessionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlNoorMediaSessionTest {

    private fun track(n: Int, cover: String? = "https://example.com/c$n.jpg") = Track(
        id = "t$n",
        title = "Title $n",
        artistId = "a",
        artistName = "Artist",
        audioUrl = "https://example.com/audio$n.mp3",
        durationMs = 60_000L,
        coverUrl = cover,
        license = LicenseType.values().first(),
        attributionText = "attr",
        sourceUrl = "https://example.com",
    )

    @Test
    fun mediaItem_carriesTitleArtistArtworkAndId() {
        val item = AlNoorMediaItems.fromTrack(track(1))
        assertEquals("t1", item.mediaId)
        assertEquals("Title 1", item.mediaMetadata.title.toString())
        assertEquals("Artist", item.mediaMetadata.artist.toString())
        assertEquals("https://example.com/c1.jpg", item.mediaMetadata.artworkUri.toString())
    }

    @Test
    fun mediaItem_blankCoverUsesBundledArtwork() {
        assertEquals(
            AlNoorMediaItems.FALLBACK_ART,
            AlNoorMediaItems.fromTrack(track(1, cover = " ")).mediaMetadata.artworkUri.toString(),
        )
    }

    @Test
    fun setQueue_loadsFullPlaylistSoPreviousNextWork() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val player = AlNoorPlayer(ctx)
        try {
            player.setQueue(listOf(track(1), track(2), track(3)), startIndex = 1, autoPlay = false)
            val exo = player.exoPlayerOrNull()!!
            assertEquals(3, exo.mediaItemCount)
            assertEquals(1, exo.currentMediaItemIndex)
            assertTrue(exo.hasPreviousMediaItem())
            assertTrue(exo.hasNextMediaItem())
            assertTrue(player.sessionPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))

            player.skipNext()
            assertEquals(2, exo.currentMediaItemIndex)
            assertEquals(2, player.queue.value.currentIndex)
            assertEquals("t3", player.queue.value.currentTrack?.id)
            assertFalse(exo.hasNextMediaItem())

            player.skipPrevious()
            assertEquals(1, exo.currentMediaItemIndex)
            assertEquals(1, player.queue.value.currentIndex)
        } finally {
            player.release()
        }
    }

    @Test
    fun repeatAndShuffle_areAppliedToExoPlayer() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val player = AlNoorPlayer(ctx)
        try {
            player.setQueue(listOf(track(1), track(2)), autoPlay = false)
            val exo = player.exoPlayerOrNull()!!
            player.cycleRepeatMode()
            assertEquals(RepeatMode.ALL, player.queue.value.repeatMode)
            assertEquals(Player.REPEAT_MODE_ALL, exo.repeatMode)
            player.toggleShuffle()
            assertTrue(exo.shuffleModeEnabled)
            player.setPlaybackSpeed(1.5f)
            assertEquals(1.5f, exo.playbackParameters.speed)
            assertEquals(1.5f, player.playback.value.speed)
        } finally {
            player.release()
        }
    }

    @Test
    fun service_registersSessionWithoutAnyControllerBinding() {
        val controller = Robolectric.buildService(AlNoorMediaSessionService::class.java).create()
        val service = controller.get()
        try {
            val sessions = service.sessions
            assertEquals(1, sessions.size)
            assertNotNull(sessions.first())
            assertTrue(service.isSessionAdded(sessions.first()))
        } finally {
            controller.destroy()
        }
    }
}
