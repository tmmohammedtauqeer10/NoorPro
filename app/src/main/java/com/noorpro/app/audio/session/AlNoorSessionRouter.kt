package com.noorpro.app.audio.session

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.noorpro.app.audio.AlNoorAudioSession

/**
 * Decides which player the single shared [AlNoorMediaSessionService] session controls:
 * Al Noor nasheeds (ExoPlayer) or Quran recitation ([QuranSessionBridge]). Selecting a source
 * pauses the other one, swaps the session player, and (re)starts the service, so there is only
 * ever one media notification and one audible source. Main thread only.
 */
object AlNoorSessionRouter {
    enum class Source { AL_NOOR, QURAN }

    @Volatile
    var source: Source = Source.AL_NOOR
        private set

    private var session: MediaSession? = null

    /** 1.0.83: observable "who owns playback" so the mini/full player shows the right source. */
    private val _active = kotlinx.coroutines.flow.MutableStateFlow<Source?>(null)
    val active: kotlinx.coroutines.flow.StateFlow<Source?> = _active

    fun playerFor(context: Context, src: Source = source): Player = when (src) {
        Source.QURAN -> QuranSessionBridge.sessionPlayer()
        Source.AL_NOOR -> {
            AlNoorAudioSession.init(context)
            AlNoorAudioSession.player.sessionPlayer
        }
    }

    fun attach(mediaSession: MediaSession) { session = mediaSession }

    fun detach(mediaSession: MediaSession) { if (session === mediaSession) session = null }

    fun select(context: Context, src: Source) {
        check(Looper.myLooper() == Looper.getMainLooper()) { "AlNoorSessionRouter.select must run on main" }
        val app = context.applicationContext
        when (src) {
            Source.QURAN -> if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.pause()
            Source.AL_NOOR -> QuranSessionBridge.pauseForOtherSource()
        }
        source = src
        _active.value = src
        session?.let { s ->
            val target = playerFor(app, src)
            if (s.player !== target) runCatching { s.player = target }
                .onFailure { Log.w(TAG, "Could not swap session player", it) }
        }
        runCatching { AlNoorMediaSession.obtain(app).startSession() }
            .onFailure { Log.w(TAG, "Could not start media service", it) }
    }

    private const val TAG = "AlNoorSessionRouter"
}
