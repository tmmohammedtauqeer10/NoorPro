package com.noorpro.app.media

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.noorpro.app.data.media.ReelMediaAvailability
import com.noorpro.app.data.media.RemoteVideoFrame
import com.noorpro.app.data.media.RemoteVideoFrameFetcher
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device checks against real production media. URLs are passed as instrumentation args
 * (deadUrl, mp4Url, hlsUrl) so no download tokens are committed. Network required.
 */
@RunWith(AndroidJUnit4::class)
class ReelMediaDeviceTest {
    private val args = InstrumentationRegistry.getArguments()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun deadStorageObjectIsDetected() {
        val dead = args.getString("deadUrl").orEmpty()
        assumeTrue(dead.isNotBlank())
        ReelMediaAvailability.init(context)
        val latch = CountDownLatch(1)
        ReelMediaAvailability.ensureChecked(listOf(dead)) { latch.countDown() }
        latch.await(20, TimeUnit.SECONDS)
        assertTrue("expected dead", ReelMediaAvailability.isDead(listOf(dead)))
    }

    @Test fun thumbnailFrameStreamsFromRemoteMp4(): Unit = runBlocking {
        val mp4 = args.getString("mp4Url").orEmpty()
        assumeTrue(mp4.isNotBlank())
        val loader = ImageLoader.Builder(context).components { add(RemoteVideoFrameFetcher.Factory()) }.build()
        val start = System.currentTimeMillis()
        val result = loader.execute(ImageRequest.Builder(context).data(RemoteVideoFrame(mp4)).build())
        assertTrue("frame result $result", result is SuccessResult)
        val bmp = ((result as SuccessResult).drawable as BitmapDrawable).bitmap
        val out = File(context.getExternalFilesDir(null), "thumb-evidence.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.i("NoorReelsTest", "frame ${bmp.width}x${bmp.height} in ${System.currentTimeMillis() - start}ms -> $out")
    }

    @Test fun hlsReelReachesReady() {
        val hls = args.getString("hlsUrl").orEmpty()
        assumeTrue(hls.isNotBlank())
        val latch = CountDownLatch(1)
        var error: PlaybackException? = null
        var player: ExoPlayer? = null
        Handler(Looper.getMainLooper()).post {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) latch.countDown() }
                    override fun onPlayerError(e: PlaybackException) { error = e; latch.countDown() }
                })
                setMediaItem(MediaItem.fromUri(hls)); prepare(); volume = 0f; play()
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        Handler(Looper.getMainLooper()).post { player?.release() }
        Log.i("NoorReelsTest", "hls ready error=${error?.errorCodeName}")
        assertTrue("hls error ${error?.errorCodeName}", error == null && latch.count == 0L)
    }
}
