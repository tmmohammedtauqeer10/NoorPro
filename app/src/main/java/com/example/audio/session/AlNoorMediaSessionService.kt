package com.example.audio.session
import com.example.R

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.audio.AlNoorAudioSession

/**
 * Media3 [MediaSessionService] for Al Noor Audio shade / lock-screen controls.
 *
 * Posts on channel [AlNoorPlaybackChannels.PLAYBACK] with play / pause / next
 * (Media3 default compact actions). Never shares prayer adhan channels.
 * Quran recitation continues to use the separate MediaPlayer path in DeenViewModel.
 */
class AlNoorMediaSessionService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var localPlayer: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        AlNoorPlaybackChannels.ensure(this)

        // Real Media3 session notification on al_noor_playback (not default_channel_id).
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(AlNoorPlaybackChannels.PLAYBACK)
            .setChannelName(R.string.al_noor_playback_channel_name)
            .setNotificationId(AlNoorPlaybackChannels.PLAYBACK_NOTIFICATION_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)

        val player: Player = resolvePlayer()
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent())
            .setId("al_noor_audio_session")
            .build()
    }

    private fun sessionActivityPendingIntent(): PendingIntent {
        val launch = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_AL_NOOR_AUDIO", true)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(this, 72002, launch, flags)
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
