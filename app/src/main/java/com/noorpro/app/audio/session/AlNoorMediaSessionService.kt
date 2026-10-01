package com.noorpro.app.audio.session

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.noorpro.app.MainActivity
import com.noorpro.app.R
import com.noorpro.app.audio.AlNoorAudioSession

/**
 * Media3 [MediaSessionService] (foregroundServiceType="mediaPlayback") for Al Noor Audio.
 *
 * Media3 posts the media-style notification (title / artist / artwork, previous / play-pause / next)
 * on [AlNoorPlaybackChannels.PLAYBACK] and promotes this service to the foreground while audio plays,
 * so playback survives Home / screen-off / tab changes. The session wraps the process-wide player
 * from [AlNoorAudioSession]; the UI never releases it.
 */
class AlNoorMediaSessionService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        AlNoorPlaybackChannels.ensure(this)
        AlNoorAudioSession.init(this)

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(AlNoorPlaybackChannels.PLAYBACK)
            .setChannelName(R.string.al_noor_playback_channel_name)
            .setNotificationId(AlNoorPlaybackChannels.PLAYBACK_NOTIFICATION_ID)
            .build()
        provider.setSmallIcon(R.drawable.ic_adhan_notification)
        setMediaNotificationProvider(provider)

        mediaSession = MediaSession.Builder(this, AlNoorAudioSession.player.sessionPlayer)
            .setSessionActivity(sessionActivityPendingIntent())
            .setId("al_noor_audio_session")
            .build()
        Log.i(TAG, "Al Noor media session service created")
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** Swiping the app away keeps playing (foreground service); if nothing is playing, shut down. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // The player is process-owned; just detach the session. Pause so nothing keeps playing
        // without a notification once the service is gone.
        runCatching { if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.pause() }
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AlNoorMediaService"
    }
}
