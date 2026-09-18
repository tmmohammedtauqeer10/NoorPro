package com.noorpro.app.audio.session

import android.content.Context
import android.content.Intent
import com.noorpro.app.audio.player.AlNoorPlayer

/**
 * Facade for shade / lock-screen playback controls for Al Noor Audio.
 *
 * Starts [AlNoorMediaSessionService] (Media3 `media3-session`) which posts on
 * [AlNoorPlaybackChannels.PLAYBACK]. Do **not** reuse prayer notification channels.
 */
interface AlNoorMediaSessionController {
    fun attach(player: AlNoorPlayer)
    fun startSession()
    fun stopSession()
    fun release()
}

/**
 * Starts / stops [AlNoorMediaSessionService]. Media3 promotes the service to
 * foreground when playback is active; we use [Context.startService] at attach
 * time so we do not violate the FGS startForeground timeout before play.
 */
class DefaultAlNoorMediaSessionController(
    context: Context,
) : AlNoorMediaSessionController {
    private val appContext = context.applicationContext
    private var player: AlNoorPlayer? = null
    private var started = false

    override fun attach(player: AlNoorPlayer) {
        this.player = player
    }

    override fun startSession() {
        if (started) return
        AlNoorPlaybackChannels.ensure(appContext)
        appContext.startService(Intent(appContext, AlNoorMediaSessionService::class.java))
        started = true
    }

    override fun stopSession() {
        if (!started) return
        appContext.stopService(Intent(appContext, AlNoorMediaSessionService::class.java))
        started = false
    }

    override fun release() {
        stopSession()
        player = null
    }
}

/** @deprecated Name kept for call sites; delegates to [DefaultAlNoorMediaSessionController]. */
typealias StubAlNoorMediaSessionController = DefaultAlNoorMediaSessionController

object AlNoorMediaSession {
    @Volatile
    private var controller: AlNoorMediaSessionController? = null

    fun obtain(context: Context): AlNoorMediaSessionController {
        controller?.let { return it }
        synchronized(this) {
            controller?.let { return it }
            return DefaultAlNoorMediaSessionController(context.applicationContext).also { controller = it }
        }
    }
}
