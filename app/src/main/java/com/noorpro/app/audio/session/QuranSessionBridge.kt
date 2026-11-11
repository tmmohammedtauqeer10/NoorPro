package com.noorpro.app.audio.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the Quran recitation player (android.media.MediaPlayer, ayah-by-ayah, owned by
 * DeenViewModel) into the shared Al Noor Media3 session.
 *
 * The ViewModel publishes a [NowPlaying] snapshot whenever its playback state changes and
 * registers [Commands]; [QuranSessionPlayer] exposes that as a Media3 [Player] so the single
 * [AlNoorMediaSessionService] posts ONE media notification (play/pause/next/previous), lock-screen
 * and QS controls, and keeps the process in the foreground while the Quran plays in the background.
 */
object QuranSessionBridge {

    interface Commands {
        fun play()
        fun pause()
        fun next()
        fun previous()
        fun seekTo(positionMs: Long)
        fun stop()
        fun positionMs(): Long
    }

    data class NowPlaying(
        val id: String,
        val title: String,
        val artist: String,
        val subtitle: String? = null,
        val isPlaying: Boolean,
        val buffering: Boolean = false,
        /** [C.TIME_UNSET] when unknown (ayah-by-ayah surah playback). */
        val durationMs: Long = C.TIME_UNSET,
        val hasPrevious: Boolean = true,
        val hasNext: Boolean = true,
    )

    @Volatile
    var commands: Commands? = null

    @Volatile
    var current: NowPlaying? = null
        private set

    private val _audible = MutableStateFlow(false)

    /** True while Quran audio is playing or buffering. Reels observe this (with Al Noor) to pause. */
    val audible: StateFlow<Boolean> = _audible.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var player: QuranSessionPlayer? = null
    private var wasActive = false

    /** The Media3 player for the session. Main thread only. */
    fun sessionPlayer(): Player = player ?: QuranSessionPlayer().also { player = it }

    /** Publish a new snapshot (null = nothing loaded / dismissed). Safe from any thread. */
    fun update(context: Context, nowPlaying: NowPlaying?) {
        val app = context.applicationContext
        val apply = Runnable {
            current = nowPlaying
            val active = nowPlaying != null && (nowPlaying.isPlaying || nowPlaying.buffering)
            _audible.value = active
            player?.refresh()
            // Only on the paused -> playing edge: take over the shared session and (re)start the
            // service, which pauses Al Noor so only one media notification / audio source exists.
            if (active && !wasActive) AlNoorSessionRouter.select(app, AlNoorSessionRouter.Source.QURAN)
            wasActive = active
        }
        if (Looper.myLooper() == Looper.getMainLooper()) apply.run() else main.post(apply)
    }

    /** Called when another source (Al Noor) takes the session. */
    fun pauseForOtherSource() {
        if (current?.isPlaying == true || current?.buffering == true) commands?.pause()
    }

    /** Which command a Media3 seek maps to. Index 0 = previous placeholder, last = next placeholder. */
    enum class SeekAction { NEXT, PREVIOUS, SEEK_IN_ITEM }

    fun resolveSeek(seekCommand: Int, targetIndex: Int, currentIndex: Int): SeekAction = when {
        seekCommand == Player.COMMAND_SEEK_TO_NEXT || seekCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> SeekAction.NEXT
        seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS || seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> SeekAction.PREVIOUS
        targetIndex > currentIndex -> SeekAction.NEXT
        targetIndex < currentIndex -> SeekAction.PREVIOUS
        else -> SeekAction.SEEK_IN_ITEM
    }
}

/**
 * Media3 view of the Quran player. The playlist is [previous placeholder?, current, next placeholder?]
 * so Media3 shows working previous / next buttons; selecting a placeholder is translated into the
 * ViewModel's previous / next surah (or playlist track) command.
 */
@OptIn(UnstableApi::class)
class QuranSessionPlayer : SimpleBasePlayer(Looper.getMainLooper()) {

    fun refresh() = invalidateState()

    override fun getState(): State {
        val np = QuranSessionBridge.current
        val commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            )
            .apply { if (np != null && np.durationMs != C.TIME_UNSET) add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) }
            .build()
        val builder = State.Builder().setAvailableCommands(commands)
        if (np == null) {
            return builder.setPlaybackState(Player.STATE_IDLE).setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST).build()
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(np.title)
            .setDisplayTitle(np.title)
            .setArtist(np.artist)
            .setSubtitle(np.subtitle ?: np.artist)
            .setAlbumTitle(if (np.id.startsWith("surah:")) "Quran" else "Al Noor Audio")
            .setArtworkUri(android.net.Uri.parse(if (np.id.startsWith("surah:")) com.noorpro.app.audio.player.AlNoorMediaItems.QURAN_ART else com.noorpro.app.audio.player.AlNoorMediaItems.FALLBACK_ART))
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()
        val items = mutableListOf<MediaItemData>()
        if (np.hasPrevious) items += placeholder("quran:prev")
        val currentIndex = items.size
        items += MediaItemData.Builder(np.id)
            .setMediaItem(MediaItem.Builder().setMediaId(np.id).setMediaMetadata(metadata).build())
            .setMediaMetadata(metadata)
            .setDurationUs(if (np.durationMs == C.TIME_UNSET) C.TIME_UNSET else np.durationMs * 1000)
            .setIsSeekable(np.durationMs != C.TIME_UNSET)
            .build()
        if (np.hasNext) items += placeholder("quran:next")
        return builder
            .setPlaylist(items)
            .setCurrentMediaItemIndex(currentIndex)
            .setContentPositionMs { QuranSessionBridge.commands?.positionMs() ?: 0L }
            .setPlayWhenReady(np.isPlaying || np.buffering, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(if (np.buffering && !np.isPlaying) Player.STATE_BUFFERING else Player.STATE_READY)
            .build()
    }

    private fun placeholder(uid: String) = MediaItemData.Builder(uid)
        .setMediaItem(MediaItem.Builder().setMediaId(uid).build())
        .build()

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        val c = QuranSessionBridge.commands
        if (playWhenReady) c?.play() else c?.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        QuranSessionBridge.commands?.stop()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val c = QuranSessionBridge.commands
        val currentIndex = currentMediaItemIndex
        when (QuranSessionBridge.resolveSeek(seekCommand, mediaItemIndex, currentIndex)) {
            QuranSessionBridge.SeekAction.NEXT -> c?.next()
            QuranSessionBridge.SeekAction.PREVIOUS -> c?.previous()
            QuranSessionBridge.SeekAction.SEEK_IN_ITEM -> if (positionMs != C.TIME_UNSET) c?.seekTo(positionMs)
        }
        return Futures.immediateVoidFuture()
    }
}
