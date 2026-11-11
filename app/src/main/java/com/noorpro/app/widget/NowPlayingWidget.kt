package com.noorpro.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import com.noorpro.app.MainActivity
import com.noorpro.app.R
import com.noorpro.app.audio.session.AlNoorSessionRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 1.0.98 Now playing 4x2 widget, driven by the app's Media3 session player (Al Noor audio or Quran
 * recitation - whichever [AlNoorSessionRouter] has selected). Shows artwork (real cover when the track
 * has one, else the N tile), title / subtitle, previous / play-pause / next and progress with a
 * launcher-ticked elapsed clock. With no active session it shows the last played item, or "Play Quran".
 */
object NowPlayingWidget {
    data class State(
        val title: String,
        val subtitle: String,
        val artUri: String?,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val isQuran: Boolean,
    )

    /** What the idle widget shows (persisted, survives process death). */
    data class LastPlayed(val title: String, val subtitle: String, val artUri: String?, val isQuran: Boolean)

    const val ACTION_PREV = "com.noorpro.app.widget.NP_PREV"
    const val ACTION_TOGGLE = "com.noorpro.app.widget.NP_TOGGLE"
    const val ACTION_NEXT = "com.noorpro.app.widget.NP_NEXT"
    private const val PREFS = "now_playing_widget"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pushJob: Job? = null
    private var artCache: Pair<String, Bitmap>? = null

    fun widgetIds(context: Context): IntArray = runCatching {
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, NowPlayingWidgetReceiver::class.java))
    }.getOrDefault(IntArray(0))

    /** Snapshot of a Media3 player. Must run on the player's (main) thread. */
    fun capture(player: Player): State? {
        if (player.mediaItemCount == 0 || player.currentMediaItem == null) return null
        val md = player.mediaMetadata
        val title = (md.displayTitle ?: md.title)?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val subtitle = (md.subtitle ?: md.artist ?: md.albumTitle)?.toString().orEmpty()
        val id = player.currentMediaItem?.mediaId.orEmpty()
        val dur = player.duration.takeIf { it > 0L } ?: 0L
        return State(
            title = title,
            subtitle = subtitle,
            artUri = md.artworkUri?.toString(),
            isPlaying = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = dur,
            isQuran = id.startsWith("surah:") || md.albumTitle?.toString() == "Quran",
        )
    }

    /** Called from the media service on player events (main thread). Cheap no-op without widgets. */
    fun onPlayerChanged(context: Context, player: Player?) {
        val app = context.applicationContext
        if (widgetIds(app).isEmpty()) return
        val state = player?.let { runCatching { capture(it) }.getOrNull() }
        state?.let { saveLast(app, it) }
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(250) // collapse bursts of events (item transition + metadata + is-playing)
            push(app, state)
        }
    }

    /** Session is gone (service destroyed): show the idle / last played state. */
    fun showIdle(context: Context) {
        val app = context.applicationContext
        if (widgetIds(app).isEmpty()) return
        pushJob?.cancel()
        pushJob = scope.launch { push(app, null) }
    }

    /** Full refresh (widget added / resized / system update): read the live player on main, then push. */
    fun refreshAll(context: Context, done: (() -> Unit)? = null) {
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            val state = AlNoorSessionRouter.currentPlayer()?.let { runCatching { capture(it) }.getOrNull() }
            scope.launch {
                try { push(app, state) } finally { done?.invoke() }
            }
        }
    }

    private suspend fun push(context: Context, state: State?) {
        val ids = widgetIds(context)
        if (ids.isEmpty()) return
        val last = if (state == null) loadLast(context) else null
        val art = loadArt(context, state?.artUri ?: last?.artUri)
        val views = buildViews(context, state, last, art)
        val mgr = AppWidgetManager.getInstance(context)
        ids.forEach { runCatching { mgr.updateAppWidget(it, views) } }
    }

    fun buildViews(context: Context, state: State?, last: LastPlayed?, art: Bitmap?): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_now_playing_4x2)
        if (art != null) v.setImageViewBitmap(R.id.widget_np_art, art) else v.setImageViewResource(R.id.widget_np_art, R.drawable.widget_art_fallback)
        if (state != null) {
            v.setTextViewText(R.id.widget_np_title, state.title)
            v.setTextViewText(R.id.widget_np_subtitle, state.subtitle)
            v.setImageViewResource(R.id.widget_np_play, if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
            v.setContentDescription(R.id.widget_np_play_btn, if (state.isPlaying) "Pause" else "Play")
            v.setViewVisibility(R.id.widget_np_prev, View.VISIBLE)
            v.setViewVisibility(R.id.widget_np_next, View.VISIBLE)
            val hasDuration = state.durationMs > 0L
            v.setViewVisibility(R.id.widget_np_progress, if (hasDuration) View.VISIBLE else View.INVISIBLE)
            v.setViewVisibility(R.id.widget_np_times, View.VISIBLE)
            if (hasDuration) {
                v.setProgressBar(R.id.widget_np_progress, 1000, ((state.positionMs * 1000L) / state.durationMs).toInt().coerceIn(0, 1000), false)
            }
            // Elapsed time is ticked by the launcher itself (no wake-ups); the service re-syncs it.
            v.setChronometer(R.id.widget_np_elapsed, SystemClock.elapsedRealtime() - state.positionMs, null, state.isPlaying)
            v.setTextViewText(R.id.widget_np_duration, if (hasDuration) clock(state.durationMs) else "")
            v.setOnClickPendingIntent(R.id.widget_np_prev, controlPending(context, ACTION_PREV, 1))
            v.setOnClickPendingIntent(R.id.widget_np_play_btn, controlPending(context, ACTION_TOGGLE, 2))
            v.setOnClickPendingIntent(R.id.widget_np_next, controlPending(context, ACTION_NEXT, 3))
            v.setOnClickPendingIntent(android.R.id.background, openAppPending(context, nowPlaying = true, quran = state.isQuran))
        } else {
            val quran = last?.isQuran ?: true
            v.setTextViewText(R.id.widget_np_title, last?.title ?: "Play Quran")
            v.setTextViewText(R.id.widget_np_subtitle, if (last != null) "Last played \u00B7 ${last.subtitle}".trimEnd(' ', '\u00B7') else "Tap to listen")
            v.setImageViewResource(R.id.widget_np_play, R.drawable.ic_widget_play)
            v.setContentDescription(R.id.widget_np_play_btn, "Play")
            v.setViewVisibility(R.id.widget_np_prev, View.GONE)
            v.setViewVisibility(R.id.widget_np_next, View.GONE)
            v.setViewVisibility(R.id.widget_np_progress, View.GONE)
            v.setViewVisibility(R.id.widget_np_times, View.GONE)
            v.setChronometer(R.id.widget_np_elapsed, SystemClock.elapsedRealtime(), null, false)
            val open = openAppPending(context, nowPlaying = false, quran = quran)
            v.setOnClickPendingIntent(R.id.widget_np_play_btn, open)
            v.setOnClickPendingIntent(android.R.id.background, open)
        }
        return v
    }

    fun clock(ms: Long): String {
        val s = ms / 1000L
        return if (s >= 3600) String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
        else String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }

    private fun saveLast(context: Context, s: State) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getString("title", null) == s.title && p.getString("subtitle", null) == s.subtitle) return
        p.edit().putString("title", s.title).putString("subtitle", s.subtitle)
            .putString("art", s.artUri).putBoolean("quran", s.isQuran).apply()
    }

    fun loadLast(context: Context): LastPlayed? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val title = p.getString("title", null) ?: return null
        return LastPlayed(title, p.getString("subtitle", null).orEmpty(), p.getString("art", null), p.getBoolean("quran", false))
    }

    /** Real cover art only (http/https/content/file). Bundled placeholders -> null -> N tile. */
    private suspend fun loadArt(context: Context, uri: String?): Bitmap? {
        if (uri.isNullOrBlank() || uri.startsWith("android.resource://")) return null
        artCache?.let { (u, b) -> if (u == uri) return b }
        val bmp = runCatching {
            val loader = coil.Coil.imageLoader(context)
            val req = coil.request.ImageRequest.Builder(context).data(uri).size(256).allowHardware(false).build()
            val res = loader.execute(req) as? coil.request.SuccessResult ?: return@runCatching null
            rounded(res.drawable.toBitmap(256, 256), 256f * 0.2f)
        }.getOrNull()
        if (bmp != null) artCache = uri to bmp
        return bmp
    }

    private fun rounded(src: Bitmap, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        Canvas(out).drawRoundRect(RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), radius, radius, paint)
        return out
    }

    private fun immutable() = PendingIntent.FLAG_UPDATE_CURRENT or
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

    private fun controlPending(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, 72_400 + code,
            Intent(context, NowPlayingWidgetReceiver::class.java).setAction(action), immutable(),
        )

    fun openAppPending(context: Context, nowPlaying: Boolean, quran: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "com.noorpro.app.widget.NOW_PLAYING"
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, nowPlaying)
            putExtra(if (quran) "OPEN_QURAN" else MainActivity.EXTRA_OPEN_AUDIO, !nowPlaying)
        }
        return PendingIntent.getActivity(context, if (nowPlaying) 72_410 else if (quran) 72_411 else 72_412, intent, immutable())
    }
}

/** Now playing widget provider; also executes the widget's transport buttons on the session player. */
class NowPlayingWidgetReceiver : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NowPlayingWidget.ACTION_PREV, NowPlayingWidget.ACTION_TOGGLE, NowPlayingWidget.ACTION_NEXT -> control(context, intent.action!!)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)
    override fun onEnabled(context: Context) = refresh(context)
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refresh(context)
    }

    private fun refresh(context: Context) {
        val pending = goAsync()
        NowPlayingWidget.refreshAll(context) { pending.finish() }
    }

    private fun control(context: Context, action: String) {
        val app = context.applicationContext
        val pending = goAsync()
        Handler(Looper.getMainLooper()).post {
            try {
                val p = AlNoorSessionRouter.currentPlayer()
                if (p == null || p.mediaItemCount == 0) {
                    // Session gone (process restarted): fall back to the idle state, whose buttons open the app.
                    NowPlayingWidget.showIdle(app)
                } else {
                    runCatching {
                        when (action) {
                            NowPlayingWidget.ACTION_PREV -> p.seekToPrevious()
                            NowPlayingWidget.ACTION_NEXT -> p.seekToNext()
                            else -> if (p.isPlaying || (p.playWhenReady && p.playbackState == Player.STATE_BUFFERING)) {
                                p.pause()
                            } else {
                                if (p.playbackState == Player.STATE_IDLE && p.isCommandAvailable(Player.COMMAND_PREPARE)) p.prepare()
                                p.play()
                            }
                        }
                    }
                    NowPlayingWidget.onPlayerChanged(app, p)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
