package com.noorpro.app.audio.session

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.noorpro.app.MainActivity
import com.noorpro.app.audio.AlNoorAudioSession

/**
 * Minimal Media3 [MediaSessionService] for Al Noor Audio shade / lock-screen controls.
 *
 * Uses channel [AlNoorPlaybackChannels.PLAYBACK] (never prayer channels).
 * Declared dependency: `androidx.media3:media3-session` (same version as ExoPlayer).
 */
class AlNoorMediaSessionService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var localPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        AlNoorPlaybackChannels.ensure(this)
        val player: Player = resolvePlayer()
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent())
            .build()
    }

    private fun sessionActivityPendingIntent(): PendingIntent {
        val launch = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(this, 0, launch, flags)
    }

    private fun resolvePlayer(): Player {
        if (AlNoorAudioSession.isInitialized()) {
            val shared = AlNoorAudioSession.player.exoPlayerOrNull()
            if (shared != null) return shared
        }
        return ExoPlayer.Builder(this).build().also { localPlayer = it }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            if (localPlayer != null && player === localPlayer) {
                player.release()
            }
            release()
            mediaSession = null
        }
        localPlayer = null
        super.onDestroy()
    }
}
