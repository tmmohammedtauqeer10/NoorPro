package com.noorpro.app.audio

import android.content.Context
import com.noorpro.app.audio.player.AlNoorPlayer
import com.noorpro.app.audio.repository.AlNoorAudioRepository
import com.noorpro.app.audio.repository.BundledAlNoorAudioRepository
import com.noorpro.app.audio.session.AlNoorMediaSession
import com.noorpro.app.audio.session.AlNoorMediaSessionController
import com.noorpro.app.audio.session.AlNoorPlaybackChannels

/**
 * App-wide Al Noor Audio session (separate from Quran MediaPlayer).
 * Initialized once from [com.noorpro.app.NoorProApplication].
 */
object AlNoorAudioSession {
    @Volatile
    private var initialized = false

    lateinit var repository: AlNoorAudioRepository
        private set

    lateinit var player: AlNoorPlayer
        private set

    lateinit var mediaSession: AlNoorMediaSessionController
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            AlNoorPlaybackChannels.ensure(app)
            repository = BundledAlNoorAudioRepository(app)
            player = AlNoorPlayer(app)
            mediaSession = AlNoorMediaSession.obtain(app).also {
                it.attach(player)
                // Starts AlNoorMediaSessionService (media3-session).
                it.startSession()
            }
            initialized = true
        }
    }

    fun isInitialized(): Boolean = initialized
}
