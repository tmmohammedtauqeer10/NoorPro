package com.noorpro.app.audio

import android.content.Context
import com.noorpro.app.audio.player.AlNoorPlayer
import com.noorpro.app.audio.repository.AlNoorAudioRepository
import com.noorpro.app.audio.repository.BundledAlNoorAudioRepository

/**
 * App-wide Al Noor Audio session (separate from Quran MediaPlayer).
 * Initialized once from [com.example.NoorProApplication].
 */
object AlNoorAudioSession {
    @Volatile
    private var initialized = false

    lateinit var repository: AlNoorAudioRepository
        private set

    lateinit var player: AlNoorPlayer
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            repository = BundledAlNoorAudioRepository(app)
            player = AlNoorPlayer(app)
            initialized = true
        }
    }

    fun isInitialized(): Boolean = initialized
}
