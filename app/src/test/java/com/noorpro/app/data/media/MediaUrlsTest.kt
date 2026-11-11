package com.noorpro.app.data.media

import com.noorpro.app.ui.screens.isTransientReelError
import com.noorpro.app.ui.screens.reelPlaybackUrl
import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlsTest {
    @Test fun legacyFieldsPickFirstNonBlank() {
        val doc = mapOf("mediaUrl" to " ", "videoUrl" to "https://x/v.mp4", "url" to "https://x/other")
        assertEquals("https://x/v.mp4", MediaUrls.firstNonBlank(MediaUrls.LEGACY_MEDIA_FIELDS) { doc[it] })
        assertEquals("", MediaUrls.firstNonBlank(MediaUrls.LEGACY_THUMB_FIELDS) { doc[it] })
    }

    @Test fun gsAndBarePathsNeedResolving() {
        assertEquals("gs://b/o/v.mp4", MediaUrls.storagePathToResolve("gs://b/o/v.mp4", ""))
        assertEquals("ummah_submissions/u/videos/1", MediaUrls.storagePathToResolve("", "/ummah_submissions/u/videos/1"))
        assertNull(MediaUrls.storagePathToResolve("https://firebasestorage.googleapis.com/v0/b/x/o/y?alt=media", "/y"))
        assertNull(MediaUrls.storagePathToResolve("", ""))
    }

    @Test fun cleartextUpgradedOnlyForKnownHosts() {
        assertEquals("https://d16l9nz0yd3az2.cloudfront.net/reels/a.mp4", MediaUrls.normalize("http://d16l9nz0yd3az2.cloudfront.net/reels/a.mp4"))
        assertEquals("https://firebasestorage.googleapis.com/v0/b", MediaUrls.normalize("http://firebasestorage.googleapis.com/v0/b"))
        assertEquals("http://example.org/a.mp4", MediaUrls.normalize("http://example.org/a.mp4"))
        assertEquals("https://a/b", MediaUrls.normalize("  https://a/b "))
    }

    @Test fun redactDropsTokens() {
        assertEquals("https://h/o/x?<redacted>", MediaUrls.redact("https://h/o/x?alt=media&token=secret"))
        assertEquals("https://h/a.mp4", MediaUrls.redact("https://h/a.mp4"))
    }

    @Test fun httpClassification() {
        assertEquals(MediaUrls.Reachability.OK, MediaUrls.classifyHttp(206))
        assertEquals(MediaUrls.Reachability.DEAD, MediaUrls.classifyHttp(402)) // Storage billing disabled
        assertEquals(MediaUrls.Reachability.DEAD, MediaUrls.classifyHttp(404))
        assertEquals(MediaUrls.Reachability.DEAD, MediaUrls.classifyHttp(403))
        assertEquals(MediaUrls.Reachability.UNKNOWN, MediaUrls.classifyHttp(503))
    }

    @Test fun videoDetection() {
        assertTrue(MediaUrls.looksLikeVideo("video", ""))
        assertTrue(MediaUrls.looksLikeVideo("image", "https://f/o/ummah_submissions%2Fu%2Fvideos%2F1?alt=media"))
        assertTrue(MediaUrls.looksLikeVideo("post", "https://c/hls/x/index.m3u8"))
        assertFalse(MediaUrls.looksLikeVideo("image", "https://f/o/images%2Fa.jpg?alt=media"))
        assertTrue(MediaUrls.isLikelyVideoUrl("https://cdn.example/reels/clip.mp4?token=secret"))
        assertFalse(MediaUrls.isLikelyVideoUrl("https://cdn.example/reels/cover.jpg"))
    }

    @Test fun videoPreviewPrefersThumbnailThenFrame() {
        assertEquals("https://t.jpg", videoPreviewModel("https://t.jpg", "https://v.mp4"))
        assertEquals(RemoteVideoFrame("https://v.mp4"), videoPreviewModel("", "https://v.mp4"))
        assertNull(videoPreviewModel("", ""))
    }

    @Test fun reelUrlSelection() {
        val mp4 = "https://c/reels/a.mp4"; val hls = "https://c/hls/reels/a/index.m3u8"
        // Untrusted geometry -> MP4 first; after the MP4 fails (4K source) -> HLS.
        assertEquals(mp4, reelPlaybackUrl(mp4, hls, trustedGeometry = false, hlsFailed = false, mp4Failed = false))
        assertEquals(hls, reelPlaybackUrl(mp4, hls, trustedGeometry = false, hlsFailed = false, mp4Failed = true))
        assertEquals(hls, reelPlaybackUrl(mp4, hls, trustedGeometry = true, hlsFailed = false, mp4Failed = false))
        assertEquals(mp4, reelPlaybackUrl(mp4, hls, trustedGeometry = true, hlsFailed = true, mp4Failed = false))
        assertEquals(mp4, reelPlaybackUrl(mp4, "", trustedGeometry = true, hlsFailed = false, mp4Failed = true))
        assertEquals(hls, reelPlaybackUrl("", hls, trustedGeometry = false, hlsFailed = false, mp4Failed = false))
    }

    @Test fun onlyNetworkFailuresAreAutoRetried() {
        assertTrue(isTransientReelError(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, null))
        assertTrue(isTransientReelError(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 503))
        assertFalse(isTransientReelError(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 402))
        assertFalse(isTransientReelError(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, null))
    }
}
