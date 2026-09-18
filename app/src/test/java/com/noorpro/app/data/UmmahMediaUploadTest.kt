package com.noorpro.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UmmahMediaUploadTest {
    @Test
    fun genericGalleryMimeIsNormalizedForReels() {
        assertEquals("video/mp4", normalizedUmmahContentType("reel", "application/octet-stream"))
        assertEquals("video/mp4", normalizedUmmahContentType("video", null))
    }

    @Test
    fun validMediaMimeIsPreserved() {
        assertEquals("video/quicktime", normalizedUmmahContentType("reel", "video/quicktime"))
        assertEquals("image/png", normalizedUmmahContentType("image", "image/png"))
    }
}
