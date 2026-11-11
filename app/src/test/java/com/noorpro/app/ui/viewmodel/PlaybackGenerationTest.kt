package com.noorpro.app.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackGenerationTest {

    /**
     * Regression for "Play all starts at track 2": the previous item's completion callback (token 1)
     * fires while the new playlist (token 2) is still loading. It must be ignored, so the queue
     * stays on index 0 instead of advancing.
     */
    @Test
    fun staleCompletionFromPreviousItemDoesNotAdvanceNewQueue() {
        val generation = PlaybackGeneration()
        var queueIndex = -1
        val tracks = listOf("Surah Al-Fatihah", "Ayatul Kursi", "Surah Al-Ikhlas")

        val previousItemToken = generation.bump() // e.g. Ar-Rahman still playing
        // "Play all": new item started at index 0.
        val playAllToken = generation.bump()
        queueIndex = 0

        fun onCompletion(token: Long) {
            if (!generation.isCurrent(token)) return
            queueIndex++
        }

        onCompletion(previousItemToken) // late callback of the old player
        assertEquals("Surah Al-Fatihah", tracks[queueIndex])

        onCompletion(playAllToken) // real end of track 1 -> track 2
        assertEquals("Ayatul Kursi", tracks[queueIndex])
    }

    @Test
    fun tokensAreMonotonic() {
        val generation = PlaybackGeneration()
        val a = generation.bump()
        val b = generation.bump()
        assertTrue(b > a)
        assertFalse(generation.isCurrent(a))
        assertTrue(generation.isCurrent(b))
    }
}
