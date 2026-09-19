package com.example.audio

import android.content.Context
import com.example.audio.player.AlNoorPlayer
import com.example.audio.repository.AlNoorAudioRepository
import com.example.audio.repository.BundledAlNoorAudioRepository
import com.example.audio.session.AlNoorMediaSession
import com.example.audio.session.AlNoorMediaSessionController
import com.example.audio.session.AlNoorPlaybackChannels

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
