package com.noorpro.app.audio.session

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun attach(player: AlNoorPlayer) {
        this.player = player
    }

    override fun startSession() {
        // Idempotent on purpose: the service may have stopped itself (task removed while paused),
        // so never short-circuit on a cached flag - just (re)start it. Called from the foreground UI
        // when the user starts playback; Media3 then promotes it to a mediaPlayback foreground service.
        AlNoorPlaybackChannels.ensure(appContext)
        // startService can throw when the app is in the background (API 26+); the controller bind
        // below still creates the service in that case, so never let it escape.
        runCatching { appContext.startService(Intent(appContext, AlNoorMediaSessionService::class.java)) }
            .onFailure { Log.w(TAG, "startService failed; relying on controller bind", it) }
        connectController()
        started = true
    }

    /**
     * Connects a [MediaController] to the service. This binds the service, makes it call
     * `onGetSession` -> `addSession`, and keeps the notification manager monitoring the session
     * even if the service's own `addSession` was somehow skipped.
     */
    private fun connectController() {
        val existing = controllerFuture
        if (existing != null && !existing.isCancelled) {
            // Still connecting, or connected: keep it. A dead one (service stopped) is replaced.
            val alive = !existing.isDone || runCatching { existing.get().isConnected }.getOrDefault(false)
            if (alive) return
            disconnectController()
        }
        runCatching {
            val token = SessionToken(appContext, ComponentName(appContext, AlNoorMediaSessionService::class.java))
            val future = MediaController.Builder(appContext, token).buildAsync()
            future.addListener({
                // Surface connection failures in logs; the service keeps working without us.
                runCatching { future.get() }.onFailure { Log.w(TAG, "MediaController connect failed", it) }
            }, MoreExecutors.directExecutor())
            controllerFuture = future
        }.onFailure { Log.w(TAG, "Could not create MediaController", it) }
    }

    private fun disconnectController() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    override fun stopSession() {
        if (!started) return
        disconnectController()
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

private const val TAG = "AlNoorMediaCtl"

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
