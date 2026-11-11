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
        provider.setSmallIcon(com.noorpro.app.notify.NoorNotify.SMALL_ICON)
        setMediaNotificationProvider(provider)

        // Shared session: plays whichever source (Al Noor nasheeds or Quran recitation) is selected.
        mediaSession = MediaSession.Builder(this, AlNoorSessionRouter.playerFor(this))
            .setSessionActivity(sessionActivityPendingIntent())
            .setId("al_noor_audio_session")
            .setCallback(NoorMediaLayout.Callback(applicationContext))
            .build()
        // ROOT CAUSE FIX: MediaSessionService only wires a session into its notification manager
        // (and therefore only posts the media notification / goes foreground) after addSession().
        // It is called implicitly from onGetSession() when a MediaController binds, but the app
        // used plain startService() with no controller, so the session was never registered and no
        // notification was ever posted ("No media playing" in the QS media player).
        mediaSession?.let { addSession(it); AlNoorSessionRouter.attach(it) }
        // 1.0.96: keep the custom buttons (−10/+10 or like) in step with the source / track.
        runCatching { AlNoorSessionRouter.playerFor(this, AlNoorSessionRouter.Source.AL_NOOR).addListener(layoutListener) }
        runCatching { QuranSessionBridge.sessionPlayer().addListener(layoutListener) }
        NoorMediaLayout.refresh(this, mediaSession)
        // 1.0.98: home-screen Now playing widget follows the session player.
        widgetHandler.post(widgetProgressTick)
        com.noorpro.app.widget.NowPlayingWidget.onPlayerChanged(this, mediaSession?.player)
        Log.i(TAG, "Al Noor media session service created")
    }

    private val layoutListener = object : androidx.media3.common.Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            NoorMediaLayout.refresh(this@AlNoorMediaSessionService, mediaSession)
        }
        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
            NoorMediaLayout.refresh(this@AlNoorMediaSessionService, mediaSession)
        }
        override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
            if (events.containsAny(
                    androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED,
                    androidx.media3.common.Player.EVENT_PLAYBACK_STATE_CHANGED,
                    androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION,
                    androidx.media3.common.Player.EVENT_MEDIA_METADATA_CHANGED,
                    androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY,
                    androidx.media3.common.Player.EVENT_TIMELINE_CHANGED,
                )
            ) {
                com.noorpro.app.widget.NowPlayingWidget.onPlayerChanged(this@AlNoorMediaSessionService, mediaSession?.player)
            }
        }
    }

    /** 1.0.98: re-sync the widget progress bar every 15 s, only while audio is actually playing. */
    private val widgetHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val widgetProgressTick: Runnable = object : Runnable {
        override fun run() {
            val p = mediaSession?.player
            if (p != null && p.isPlaying) com.noorpro.app.widget.NowPlayingWidget.onPlayerChanged(this@AlNoorMediaSessionService, p)
            widgetHandler.postDelayed(this, 15_000L)
        }
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
        runCatching { QuranSessionBridge.pauseForOtherSource() }
        runCatching { if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.sessionPlayer.removeListener(layoutListener) }
        runCatching { QuranSessionBridge.sessionPlayer().removeListener(layoutListener) }
        widgetHandler.removeCallbacks(widgetProgressTick)
        mediaSession?.run {
            AlNoorSessionRouter.detach(this)
            release()
            mediaSession = null
        }
        runCatching { com.noorpro.app.widget.NowPlayingWidget.showIdle(this) }
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AlNoorMediaService"
    }
}
