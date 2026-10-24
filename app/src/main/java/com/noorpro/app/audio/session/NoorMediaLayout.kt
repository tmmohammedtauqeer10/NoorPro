package com.noorpro.app.audio.session

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * 1.0.96: custom buttons on the media notification, lock screen and QS media player.
 * - Al Noor audio (nasheeds, bayans, podcasts): −10 s / +10 s.
 * - Quran surahs: like / unlike (same store as the Audio library hearts).
 */
@OptIn(UnstableApi::class)
object NoorMediaLayout {
    const val SEEK_BACK = "noor.seek_back"
    const val SEEK_FWD = "noor.seek_fwd"
    const val LIKE = "noor.like"
    const val STEP_MS = 10_000L

    val commands: List<SessionCommand> = listOf(SEEK_BACK, SEEK_FWD, LIKE).map { SessionCommand(it, Bundle.EMPTY) }

    fun surahId(mediaId: String?): Int? = mediaId?.takeIf { it.startsWith("surah:") }?.substringAfter("surah:")?.toIntOrNull()

    fun layoutFor(source: AlNoorSessionRouter.Source, liked: Boolean): ImmutableList<CommandButton> = when (source) {
        AlNoorSessionRouter.Source.AL_NOOR -> ImmutableList.of(
            CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
                .setDisplayName("Back 10 seconds")
                .setSessionCommand(SessionCommand(SEEK_BACK, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
                .setDisplayName("Forward 10 seconds")
                .setSessionCommand(SessionCommand(SEEK_FWD, Bundle.EMPTY))
                .build(),
        )
        AlNoorSessionRouter.Source.QURAN -> ImmutableList.of(
            CommandButton.Builder(if (liked) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
                .setDisplayName(if (liked) "Unlike" else "Like")
                .setSessionCommand(SessionCommand(LIKE, Bundle.EMPTY))
                .build(),
        )
    }

    fun currentLayout(context: Context, player: Player?): ImmutableList<CommandButton> {
        val source = AlNoorSessionRouter.source
        val liked = surahId(QuranSessionBridge.current?.id)?.let { id ->
            runCatching { id in com.noorpro.app.ui.screens.AudioLibraryStore.liked(context) }.getOrDefault(false)
        } ?: false
        return layoutFor(source, liked)
    }

    fun refresh(context: Context, session: MediaSession?) {
        val s = session ?: return
        runCatching { s.setCustomLayout(currentLayout(context, s.player)) }
    }

    /** Position after a ±10 s step, clamped to [0, duration]. */
    fun stepped(position: Long, duration: Long, deltaMs: Long): Long {
        val p = (position + deltaMs).coerceAtLeast(0L)
        return if (duration != C.TIME_UNSET && duration > 0) p.coerceAtMost(duration) else p
    }

    class Callback(private val context: Context) : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val cmds = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .apply { commands.forEach { add(it) } }
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(cmds)
                .setCustomLayout(currentLayout(context, session.player))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val p = session.player
            when (customCommand.customAction) {
                SEEK_BACK, SEEK_FWD -> if (p.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                    val delta = if (customCommand.customAction == SEEK_BACK) -STEP_MS else STEP_MS
                    p.seekTo(stepped(p.currentPosition, p.duration, delta))
                }
                LIKE -> surahId(QuranSessionBridge.current?.id)?.let { id ->
                    runCatching { com.noorpro.app.ui.screens.AudioLibraryStore.toggleLike(context, id) }
                    refresh(context, session)
                }
                else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
