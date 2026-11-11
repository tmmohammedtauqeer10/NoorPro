package com.noorpro.app.ui.viewmodel

/**
 * Monotonic token for "which item the user most recently started". Player callbacks capture the
 * token when they are created and ignore themselves once a newer item has been started, so a late
 * completion / error from a previous track can never advance or stop the new one.
 */
class PlaybackGeneration {
    @Volatile
    var current: Long = 0L
        private set

    /** Starts a new generation and returns its token. */
    @Synchronized
    fun bump(): Long = ++current

    fun isCurrent(token: Long): Boolean = token == current
}
