package com.noorpro.app.audio

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import com.noorpro.app.audio.session.QuranSessionBridge
import com.noorpro.app.audio.session.QuranSessionBridge.SeekAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuranSessionBridgeTest {

    private val calls = mutableListOf<String>()
    private val commands = object : QuranSessionBridge.Commands {
        override fun play() { calls += "play" }
        override fun pause() { calls += "pause" }
        override fun next() { calls += "next" }
        override fun previous() { calls += "previous" }
        override fun seekTo(positionMs: Long) { calls += "seek:$positionMs" }
        override fun stop() { calls += "stop" }
        override fun positionMs(): Long = 1234L
    }

    @After
    fun tearDown() {
        QuranSessionBridge.commands = null
    }

    @Test
    fun resolveSeek_mapsMedia3CommandsToSurahNavigation() {
        assertEquals(SeekAction.NEXT, QuranSessionBridge.resolveSeek(Player.COMMAND_SEEK_TO_NEXT, 1, 1))
        assertEquals(SeekAction.PREVIOUS, QuranSessionBridge.resolveSeek(Player.COMMAND_SEEK_TO_PREVIOUS, 1, 1))
        assertEquals(SeekAction.NEXT, QuranSessionBridge.resolveSeek(Player.COMMAND_SEEK_TO_MEDIA_ITEM, 2, 1))
        assertEquals(SeekAction.PREVIOUS, QuranSessionBridge.resolveSeek(Player.COMMAND_SEEK_TO_MEDIA_ITEM, 0, 1))
        assertEquals(SeekAction.SEEK_IN_ITEM, QuranSessionBridge.resolveSeek(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, 1, 1))
    }

    @Test
    fun sessionPlayer_reflectsSnapshotAndRoutesCommands() {
        QuranSessionBridge.commands = commands
        val player = QuranSessionBridge.sessionPlayer()
        val np = QuranSessionBridge.NowPlaying(
            id = "surah:55", title = "Ar-Rahman", artist = "Mahmoud Khaleel Al-Husary", isPlaying = false,
        )
        // isPlaying=false: no service start / source switch, just state.
        QuranSessionBridge.update(ApplicationProvider.getApplicationContext(), np)

        assertEquals(Player.STATE_READY, player.playbackState)
        assertFalse(player.playWhenReady)
        assertEquals("Ar-Rahman", player.mediaMetadata.title.toString())
        assertEquals(3, player.mediaItemCount)
        assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(player.hasNextMediaItem())
        assertTrue(player.hasPreviousMediaItem())
        assertEquals(C.TIME_UNSET, player.duration)

        player.play()
        player.seekToNext()
        player.seekToPrevious()
        assertEquals(listOf("play", "next", "previous"), calls)

        QuranSessionBridge.update(ApplicationProvider.getApplicationContext(), null)
        assertEquals(Player.STATE_IDLE, player.playbackState)
        assertFalse(QuranSessionBridge.audible.value)
    }
}
