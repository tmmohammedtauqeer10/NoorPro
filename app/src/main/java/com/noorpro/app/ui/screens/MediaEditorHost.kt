package com.noorpro.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.noorpro.app.audio.catalog.FreeAudioCatalog
import com.noorpro.app.data.ReelEditor
import com.noorpro.app.data.media.edit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * 1.1.3 — glue between the composer and the editors. The editors only produce the final media file;
 * the composer's existing submit / upload calls are unchanged.
 */

/** Labels for the location / time / hashtag stickers, from what the composer already knows. */
internal fun composerStickerLabels(state: ComposerUiState, now: Date = Date()): Map<StickerKind, String> = mapOf(
    StickerKind.LOCATION to state.location.trim().ifBlank { "My masjid" },
    StickerKind.TIME to SimpleDateFormat("EEE · h:mm a", Locale.ENGLISH).format(now),
    StickerKind.HASHTAG to (Regex("#[\\p{L}\\p{N}_]+").find(state.text.text)?.value ?: "#NoorPro"),
)

@Composable
fun PhotoEditorHost(state: ComposerUiState, index: Int, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val uri = state.photos.getOrNull(index)
    if (uri == null) { LaunchedEffect(Unit) { onClose() }; return }
    val (original, previous) = state.photoOrigins[uri] ?: (uri to PhotoEdit())
    val editor = remember(uri) { PhotoEditorState(previous) }
    var bitmap by remember(original) { mutableStateOf<Bitmap?>(ComposerPreview.images[original.toString()]) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(original) { if (bitmap == null) bitmap = withContext(Dispatchers.IO) { PhotoRenderer.decode(ctx, original, 1440) } }
    BackHandler(onBack = onClose)
    NoorPhotoEditor(
        state = editor, bitmap = bitmap, onCancel = onClose,
        onDone = { e ->
            if (e.isUntouched) {
                state.photos = state.photos.map { if (it == uri) original else it }
                state.photoOrigins = state.photoOrigins - uri
                onClose()
            } else {
                saving = true
                scope.launch {
                    val f = withContext(Dispatchers.Default) { PhotoRenderer.renderToFile(ctx, original, e) }
                    saving = false
                    if (f == null) Toast.makeText(ctx, "Couldn't save this edit. Try again.", Toast.LENGTH_SHORT).show()
                    else {
                        val edited = Uri.fromFile(f)
                        state.photos = state.photos.map { if (it == uri) edited else it }
                        state.photoOrigins = state.photoOrigins - uri + (edited to (original to e))
                        onClose()
                    }
                }
            }
        },
        positionLabel = if (state.photos.size > 1) "Photo ${index + 1} of ${state.photos.size}" else null,
        stickerLabels = composerStickerLabels(state), saving = saving
    )
}

/** Al-Noor tracks whose licence allows reuse in a new video (public domain / CC BY) — short duas only. */
internal fun reelSoundsFrom(ctx: android.content.Context, limit: Int = 40): List<ReelSound> = runCatching {
    FreeAudioCatalog.duaGroups(ctx).flatMap { it.tracks }.mapNotNull(ReelSounds::from).distinctBy { it.url }.take(limit)
}.getOrDefault(emptyList())

@Composable
fun ReelEditorHost(state: ComposerUiState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val source = state.reelSource ?: state.video
    if (source == null) { LaunchedEffect(Unit) { onClose() }; return }
    val sourceMs = state.reelEdit?.sourceDurationMs ?: state.videoDurationMs
    val editor = remember(source) {
        ReelEditorState(state.reelEdit ?: ReelEdit(sourceMs).let { if (state.trimEndMs > 0) it.trim(state.trimStartMs, state.trimEndMs) else it })
    }
    var frame by remember { mutableStateOf<Bitmap?>(state.coverFrame) }
    var strip by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var sounds by remember { mutableStateOf<List<ReelSound>>(emptyList()) }
    var cancelExport by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(source) {
        sounds = withContext(Dispatchers.IO) { reelSoundsFrom(ctx) }
        strip = withContext(Dispatchers.IO) {
            if (sourceMs <= 0) emptyList() else (0 until 8).mapNotNull { i -> ReelEditor.frameAt(ctx, source, sourceMs * i / 8 + 200, 200) }
        }
    }
    val at = if (editor.tab == ReelTab.COVER) editor.edit.coverMs else editor.playheadMs
    LaunchedEffect(source, at, editor.edit.clips, editor.edit.speed) {
        delay(120)
        val src = editor.edit.sourceAt(at)?.second ?: return@LaunchedEffect
        withContext(Dispatchers.IO) { ReelEditor.frameAt(ctx, source, src, 720) }?.let { frame = it }
    }
    BackHandler { if (editor.exportProgress != null) cancelExport?.invoke() else onClose() }

    fun applyCredit(e: ReelEdit) {
        val old = state.reelEdit?.sound?.credit
        if (e.sound != null && (state.credit.isBlank() || state.credit == old)) state.credit = e.sound.credit
        if (e.sound == null && old != null && state.credit == old) state.credit = ""
    }

    NoorReelEditor(
        state = editor, frame = frame, filmstrip = strip, sounds = sounds, onCancel = onClose,
        stickerLabels = composerStickerLabels(state),
        onCancelExport = { cancelExport?.invoke() },
        onDone = { e ->
            if (!ReelExportParams.needsFullExport(e)) {
                // Plain trim: keep the 1.1.1 path (trim happens at post time).
                applyCredit(e)
                state.video = source; state.reelSource = null; state.reelEdit = e
                state.videoDurationMs = e.sourceDurationMs
                state.trimStartMs = e.trimStartMs; state.trimEndMs = e.trimEndMs
                if (e.coverMs > 0) { state.coverMs = e.trimStartMs + e.coverMs; state.coverTouched = true }
                onClose(); return@NoorReelEditor
            }
            editor.exportProgress = 0f; editor.exportError = null
            val (w, h) = ReelExporter.videoSize(ctx, source)
            val params = ReelExportParams.from(e, w, h)
            cancelExport = ReelExporter.export(ctx, source, params, onProgress = { editor.exportProgress = it }) { file ->
                cancelExport = null
                editor.exportProgress = null
                if (file == null) {
                    editor.exportError = "Couldn't apply the edits on this phone. Try a shorter clip or fewer effects."
                    Toast.makeText(ctx, editor.exportError, Toast.LENGTH_LONG).show()
                } else {
                    applyCredit(e)
                    state.reelSource = source; state.reelEdit = e
                    state.video = Uri.fromFile(file); state.coverFrame = null
                    state.videoDurationMs = e.outputMs
                    state.trimStartMs = 0; state.trimEndMs = e.outputMs
                    state.coverMs = e.coverMs; state.coverTouched = e.coverMs > 0
                    onClose()
                }
            }
        }
    )
}
