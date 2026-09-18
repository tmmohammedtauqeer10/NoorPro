package com.noorpro.app.audio.session

import android.content.Context
import com.noorpro.app.audio.player.AlNoorPlayer

/**
 * Facade for shade / lock-screen playback controls for Al Noor Audio.
 *
 * **Status:** stub only. `androidx.media3:media3-session` is not on the classpath
 * (only media3-exoplayer / ui / hls). When adding MediaSession:
 * 1. Depend on `media3-session` matching ExoPlayer (currently 1.2.0).
 * 2. Implement a `MediaSessionService` posting on [AlNoorPlaybackChannels.PLAYBACK].
 * 3. Replace [StubAlNoorMediaSessionController] and wire from [com.noorpro.app.audio.AlNoorAudioSession].
 *
 * Do **not** reuse prayer notification channels for media controls.
 */
interface AlNoorMediaSessionController {
    fun attach(player: AlNoorPlayer)
    fun startSession()
    fun stopSession()
    fun release()
}

/**
 * No-op until Media3 MediaSession is added. Safe to call from Application / player.
 */
class StubAlNoorMediaSessionController(
    @Suppress("UNUSED_PARAMETER") context: Context,
) : AlNoorMediaSessionController {
    override fun attach(player: AlNoorPlayer) {
        // TODO(media-session): bind ExoPlayer to MediaSession when media3-session lands
    }

    override fun startSession() {
        // TODO(media-session): start foreground MediaSessionService + notify on al_noor_playback
    }

    override fun stopSession() {
        // TODO(media-session): stop foreground + clear playback notification
    }

    override fun release() {
        // TODO(media-session): release MediaSession
    }
}

object AlNoorMediaSession {
    @Volatile
    private var controller: AlNoorMediaSessionController? = null

    fun obtain(context: Context): AlNoorMediaSessionController {
        controller?.let { return it }
        synchronized(this) {
            controller?.let { return it }
            return StubAlNoorMediaSessionController(context.applicationContext).also { controller = it }
        }
    }
}
