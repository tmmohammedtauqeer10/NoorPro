package com.noorpro.app.audio.session

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.noorpro.app.MainActivity
import com.noorpro.app.audio.AlNoorAudioSession
import com.noorpro.app.audio.QuranAudioCache
import com.noorpro.app.audio.player.AlNoorMediaItems
import com.noorpro.app.audio.podcast.PodcastRepository
import com.noorpro.app.data.QuranMetaData
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.audio.*

/**
 * 1.1.6 Android Auto / car media browser (car_mode_enabled). Declared disabled in the manifest and only
 * enabled by [CarComponents.sync] while the flag is on, so cars never list NoorPro until then.
 *
 * It has its own ExoPlayer (spoken-word audio attributes, audio focus) rather than sharing the phone
 * session's player, so the existing Al-Noor / Quran notification and widget keep working untouched;
 * starting playback here pauses both phone sources. Browse tree: [CarModeLogic].
 */
@OptIn(UnstableApi::class)
class NoorCarLibraryService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())
    private val saveTick = object : Runnable {
        override fun run() { saveResume(); if (player.isPlaying) handler.postDelayed(this, 10_000) }
    }

    override fun onCreate() {
        super.onCreate()
        AlNoorPlaybackChannels.ensure(this)
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) { pausePhoneSources(); handler.removeCallbacks(saveTick); handler.postDelayed(saveTick, 10_000) } else saveResume()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { saveResume() }
        })
        session = MediaLibrarySession.Builder(this, player, Callback())
            .setId("noor_car_library")
            .setSessionActivity(PendingIntent.getActivity(this, 7461, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        saveResume()
        session?.release(); session = null
        player.release()
        super.onDestroy()
    }

    private fun pausePhoneSources() {
        runCatching { if (AlNoorAudioSession.isInitialized()) AlNoorAudioSession.player.pause() }
        runCatching { QuranSessionBridge.pauseForOtherSource() }
    }

    private fun saveResume() {
        val item = player.currentMediaItem ?: return
        val t = CarModeLogic.parse(item.mediaId) as? CarTarget.Episode ?: return
        val pos = player.currentPosition.takeIf { it > 0 } ?: return
        BayanResumeStore(this).save(ResumePoint(t.trackId, t.podcastId, item.mediaMetadata.title?.toString().orEmpty(),
            item.localConfiguration?.uri?.toString().orEmpty(), pos, player.duration.takeIf { it > 0 } ?: 0L, System.currentTimeMillis(),
            item.mediaMetadata.artworkUri?.toString()))
    }

    // ---------------------------------------------------------------- tree
    private fun bayansOn() = IslamicFlags.isOn(IslamicFlags.BAYAN_PODCASTS)

    private fun children(parentId: String): List<MediaItem> {
        if (!IslamicFlags.isOn(IslamicFlags.CAR_MODE)) return emptyList()
        val nodes: List<CarNode> = when (parentId) {
            CarModeLogic.ROOT -> CarModeLogic.root(bayansOn(), RoutineStore(this).all().size, BayanLogic.inProgress(BayanResumeStore(this).all()).size)
            CarModeLogic.QURAN -> CarModeLogic.quran(reciterStorage().completeSurahs)
            CarModeLogic.ROUTINES -> CarModeLogic.routines(RoutineStore(this).all())
            CarModeLogic.CONTINUE -> if (bayansOn()) CarModeLogic.continueNodes(BayanResumeStore(this).all()) else emptyList()
            CarModeLogic.BAYANS -> if (bayansOn()) BayanLogic.shows().let { s -> CarModeLogic.shows(s, s.associate { it.id to PodcastRepository.cachedEpisodes(this, it).size }) } else emptyList()
            else -> CarModeLogic.showIdOf(parentId)?.takeIf { bayansOn() }?.let { id ->
                val p = BayanLogic.shows().firstOrNull { it.id == id } ?: return emptyList()
                CarModeLogic.episodes(p, PodcastRepository.cachedEpisodes(this, p), BayanResumeStore(this).all().associateBy { it.trackId })
            }.orEmpty()
        }
        return nodes.map(::node)
    }

    private fun reciterStorage() = OfflineQuranStore.storage(this, CarModeLogic.reciter(OfflineQuranStore.all(this)))

    private fun node(n: CarNode): MediaItem = MediaItem.Builder().setMediaId(n.id).setMediaMetadata(
        MediaMetadata.Builder().setTitle(n.title).setSubtitle(n.subtitle).setArtist(n.subtitle)
            .setIsBrowsable(!n.playable).setIsPlayable(n.playable)
            .setMediaType(if (n.playable) MediaMetadata.MEDIA_TYPE_MUSIC else MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .setArtworkUri(Uri.parse(AlNoorMediaItems.QURAN_ART)).build(),
    ).build()

    /** Turns a playable node into real items: a surah becomes its ayah files in order; an episode its enclosure. */
    private fun resolve(mediaId: String): Pair<List<MediaItem>, Long> {
        val reciter = CarModeLogic.reciter(OfflineQuranStore.all(this))
        fun surah(n: Int) = CarModeLogic.ayahSources(reciter.prefixUrl, n) { a -> QuranAudioCache.cachedFile(this, reciter.prefixUrl, n, a)?.let { Uri.fromFile(it).toString() } }
            .mapIndexed { i, uri ->
                MediaItem.Builder().setMediaId(CarModeLogic.surahId(n) + "#${i + 1}").setUri(uri).setMediaMetadata(MediaMetadata.Builder()
                    .setTitle("${OfflineQuranLogic.surahName(n)} · ${i + 1}").setArtist(reciter.name)
                    .setAlbumTitle(QuranMetaData.surahNamesAr.getOrElse(n - 1) { "" }).setIsPlayable(true).setIsBrowsable(false)
                    .setArtworkUri(Uri.parse(AlNoorMediaItems.QURAN_ART)).build()).build()
            }
        return when (val t = CarModeLogic.parse(mediaId.substringBefore('#'))) {
            is CarTarget.Surah -> surah(t.surah) to 0L
            is CarTarget.Routine -> RoutineStore(this).all().firstOrNull { it.id == t.id }?.surahs.orEmpty().flatMap { surah(it) } to 0L
            is CarTarget.Episode -> {
                val p = BayanLogic.shows().firstOrNull { it.id == t.podcastId }
                val e = p?.let { PodcastRepository.cachedEpisodes(this, it).firstOrNull { e -> BayanLogic.trackId(it, e) == t.trackId } }
                val saved = BayanResumeStore(this).get(t.trackId)
                val url = e?.audioUrl ?: saved?.audioUrl
                if (url.isNullOrBlank() || !bayansOn()) emptyList<MediaItem>() to 0L
                else listOf(MediaItem.Builder().setMediaId(mediaId).setUri(url).setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(e?.title ?: saved?.title).setArtist(p?.name).setIsPlayable(true).setIsBrowsable(false)
                    .setArtworkUri(Uri.parse(e?.imageUrl ?: saved?.artwork ?: AlNoorMediaItems.FALLBACK_ART)).build()).build()) to
                    (saved?.let { BayanLogic.resumeFrom(it.positionMs, it.durationMs) } ?: 0L)
            }
            null -> emptyList<MediaItem>() to 0L
        }
    }

    private inner class Callback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(node(CarNode(CarModeLogic.ROOT, "NoorPro", "Quran & bayans", false)), params))

        override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val all = children(parentId)
            val from = (page * pageSize).coerceAtMost(all.size)
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(all.subList(from, (from + pageSize).coerceAtMost(all.size))), params))
        }

        override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String): ListenableFuture<LibraryResult<MediaItem>> {
            val first = resolve(mediaId).first.firstOrNull()
            return Futures.immediateFuture(if (first != null) LibraryResult.ofItem(first, null) else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
        }

        override fun onSetMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val (items, pos) = if (mediaItems.size == 1) resolve(mediaItems[0].mediaId) else mediaItems.flatMap { resolve(it.mediaId).first } to 0L
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(items, 0, if (startPositionMs > 0) startPositionMs else pos))
        }

        override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.flatMap { resolve(it.mediaId).first }.toMutableList())
    }
}
