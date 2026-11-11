package com.noorpro.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReelCountsAndTagsTest {
    @Test fun abbreviates() {
        assertEquals("0", abbreviateCount(0))
        assertEquals("999", abbreviateCount(999))
        assertEquals("1.2K", abbreviateCount(1_250))
        assertEquals("1K", abbreviateCount(1_000))
        assertEquals("12K", abbreviateCount(12_900))
        assertEquals("1.5M", abbreviateCount(1_540_000))
    }

    @Test fun extractsTags() {
        assertEquals(listOf("#Makkah", "@noor_pro", "#dua"), captionTags("Visit #Makkah with @noor_pro #dua #Makkah email a@b.c"))
    }

    @Test fun portraitFillsScreen() {
        // 9:16 decoded frame must not be treated as a fit/letterbox frame.
        assertEquals(false, shouldFitReelFrame(displayedReelAspectRatio(1080, 1920)))
        assertEquals(true, shouldFitReelFrame(displayedReelAspectRatio(1920, 1080)))
    }
}
