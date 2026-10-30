package com.noorpro.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.noorpro.app.audio.catalog.FreeAudioCatalog
import com.noorpro.app.data.ReelEditor
import com.noorpro.app.data.media.edit.*
import com.noorpro.app.data.ComposerDestination
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
    val sources = rememberEditorStickerSources(state)
    NoorPhotoEditor(
        state = editor, bitmap = bitmap, onCancel = onClose,
        onDone = { e ->
            if (e.isUntouched) {
                state.photos = state.photos.map { if (it == uri) original else it }
                state.photoOrigins = state.photoOrigins - uri
                onClose()
            } else {
                saving = true
                // Story + animated text → a short motion story (photo still + animated layers), like Instagram.
                val motion = state.destination == ComposerDestination.STORY && e.layers.any { it.anim != TextAnim.NONE }
                scope.launch {
                    val f = withContext(Dispatchers.Default) { PhotoRenderer.renderToFile(ctx, original, e) }
                    if (f == null) { saving = false; Toast.makeText(ctx, "Couldn't save this edit. Try again.", Toast.LENGTH_SHORT).show(); return@launch }
                    val edited = Uri.fromFile(f)
                    state.photos = state.photos.map { if (it == uri) edited else it }
                    state.photoOrigins = state.photoOrigins - uri + (edited to (original to e))
                    if (!motion) { saving = false; onClose(); return@launch }
                    val still = withContext(Dispatchers.Default) { PhotoRenderer.renderToFile(ctx, original, e.copy(layers = emptyList())) }
                    val size = still?.let { android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { o -> android.graphics.BitmapFactory.decodeFile(it.path, o) } }
                    if (still == null || size == null) { saving = false; onClose(); return@launch }
                    ReelExporter.exportStoryMotion(ctx, still, e.layers, size.outWidth, size.outHeight, {}) { mp4 ->
                        saving = false
                        if (mp4 != null) { state.video = Uri.fromFile(mp4); state.videoDurationMs = 6_000; state.trimStartMs = 0; state.trimEndMs = 6_000 }
                        else Toast.makeText(ctx, "Saved as a photo — this phone couldn't animate the text.", Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                }
            }
        },
        positionLabel = if (state.photos.size > 1) "Photo ${index + 1} of ${state.photos.size}" else null,
        stickerLabels = composerStickerLabels(state), saving = saving,
        stickerSources = sources, interactive = StoryInteractive.enabled && state.destination == ComposerDestination.STORY
    )
}

/** Offline Islamic sticker content + the next prayer label (same snapshot as the prayer widgets). */
@Composable
internal fun rememberEditorStickerSources(state: ComposerUiState): StickerSources {
    val ctx = LocalContext.current
    var prayer by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { prayer = withContext(Dispatchers.IO) { EditorStickerData.prayerLabel(ctx) } }
    return remember(prayer) { EditorStickerData.sources(ctx, prayer) }
}

/** Layout / collage over the composer's photos; the collage replaces the photos it uses. */
@Composable
fun CollageEditorHost(state: ComposerUiState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val uris = state.photos.take(6)
    if (uris.size < 2) { LaunchedEffect(Unit) { onClose() }; return }
    val editor = remember { CollageEditorState(CollageEdit(layout = CollageLayout.forCount(uris.size).last())) }
    var photos by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(uris) { photos = withContext(Dispatchers.IO) { uris.mapNotNull { ComposerPreview.images[it.toString()] ?: PhotoRenderer.decode(ctx, it, 1080) } } }
    BackHandler(onBack = onClose)
    NoorCollageEditor(editor, photos, onClose, { e ->
        saving = true
        scope.launch {
            val f = withContext(Dispatchers.Default) {
                val full = uris.mapNotNull { PhotoRenderer.decode(ctx, it, 1600) }
                runCatching {
                    val b = PhotoRenderer.collage(full, e)
                    java.io.File(ctx.cacheDir, "noor_collage_${System.currentTimeMillis()}.jpg").also { out -> out.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 92, it) } }
                }.getOrNull()
            }
            saving = false
            if (f == null) { Toast.makeText(ctx, "Couldn't make this layout. Try again.", Toast.LENGTH_SHORT).show(); return@launch }
            val used = (0 until e.layout.count).map { e.photoFor(it, uris.size) }.distinct().map { uris[it] }.toSet()
            val collage = Uri.fromFile(f)
            state.photos = listOf(collage) + state.photos.filterNot { it in used }
            state.photoOrigins = state.photoOrigins.filterKeys { it !in used }
            onClose()
        }
    }, saving)
}

/** Al-Noor tracks whose licence allows reuse in a new video (public domain / CC BY); 1.1.9 shared sound library. */
internal fun reelSoundsFrom(ctx: android.content.Context, limit: Int = 120): List<ReelSound> =
    com.noorpro.app.data.media.edit.ReelSoundLibrary.load(ctx).map { it.sound }.take(limit)

@Composable
fun ReelEditorHost(state: ComposerUiState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val source = state.reelSource ?: state.video
    if (source == null) { LaunchedEffect(Unit) { onClose() }; return }
    val sourceMs = state.reelEdit?.sourceDurationMs ?: state.videoDurationMs
    val editor = remember(source) {
        ReelEditorState((state.reelEdit ?: ReelEdit(sourceMs).let { if (state.trimEndMs > 0) it.trim(state.trimStartMs, state.trimEndMs) else it })
            .let { e -> if (state.sound != null && e.sound != state.sound) e.copy(sound = state.sound) else e })
    }
    var frame by remember { mutableStateOf<Bitmap?>(state.coverFrame) }
    var strip by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var sounds by remember { mutableStateOf<List<ReelSound>>(emptyList()) }
    var cancelExport by remember { mutableStateOf<(() -> Unit)?>(null) }
    val sources = rememberEditorStickerSources(state)
    fun uriOf(src: Int): Uri = editor.edit.sources.getOrNull(src)?.uri?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: source
    LaunchedEffect(source) {
        sounds = withContext(Dispatchers.IO) { listOfNotNull(state.sound) + reelSoundsFrom(ctx).filter { it.trackId != state.sound?.trackId } }
        strip = withContext(Dispatchers.IO) {
            if (sourceMs <= 0) emptyList() else (0 until 8).mapNotNull { i -> ReelEditor.frameAt(ctx, source, sourceMs * i / 8 + 200, 200) }
        }
    }
    val at = if (editor.tab == ReelTab.COVER) editor.edit.coverMs else editor.playheadMs
    LaunchedEffect(source, at, editor.edit.clips, editor.edit.speed, editor.edit.ramp, editor.playing) {
        if (editor.playing) return@LaunchedEffect
        delay(120)
        val (clip, src) = editor.edit.sourceAt(at) ?: return@LaunchedEffect
        val uri = uriOf(editor.edit.clips.getOrNull(clip)?.source ?: 0)
        withContext(Dispatchers.IO) { ReelEditor.frameAt(ctx, uri, src, 720) }?.let { frame = it }
    }

    // Live preview (ExoPlayer) — drives the playhead while playing.
    val live = remember(source) { runCatching { ReelLivePlayer(ctx, source) }.getOrNull() }
    DisposableEffect(live) { onDispose { live?.release() } }
    LaunchedEffect(live, editor.edit) { live?.load(editor.edit) }
    LaunchedEffect(live, editor.playing) {
        val p = live ?: run { editor.playing = false; return@LaunchedEffect }
        if (!editor.playing) { p.pause(); return@LaunchedEffect }
        p.play(editor.playheadMs)
        while (editor.playing) {
            withFrameMillis { }
            editor.playheadMs = p.tick()
            if (p.ended) editor.playing = false
        }
        p.pause()
    }

    // Voiceover (RECORD_AUDIO is already declared for voice features; asked only when the user taps record).
    var recorder by remember { mutableStateOf<VoiceRecorder?>(null) }
    var recFile by remember { mutableStateOf<java.io.File?>(null) }
    var recStart by remember { mutableLongStateOf(0L) }
    lateinit var voice: VoiceControl
    fun startRecording() {
        val f = java.io.File(ctx.cacheDir, "reel_voice/v_${System.currentTimeMillis()}.wav").apply { parentFile?.mkdirs() }
        val r = VoiceRecorder(f)
        if (!r.start()) { Toast.makeText(ctx, "Couldn't start the microphone.", Toast.LENGTH_SHORT).show(); return }
        recorder = r; recFile = f; recStart = editor.playheadMs
        voice.recording = true; editor.playing = live != null
        scope.launch { while (recorder === r) { voice.level = r.level; voice.elapsedMs = r.recordedMs; delay(80) } }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startRecording() else Toast.makeText(ctx, "Microphone permission is needed for voiceover", Toast.LENGTH_SHORT).show()
    }
    voice = remember {
        VoiceControl(
            onStart = {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
                else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            },
            onStop = {
                val r = recorder; val f = recFile
                recorder = null; voice.recording = false; editor.playing = false
                val dur = r?.stop() ?: 0L
                if (f != null && dur >= 300) editor.commit(editor.edit.upsertVoice(VoiceClip(editor.newId("v"), f.absolutePath, recStart, dur.coerceAtMost((editor.edit.outputMs - recStart).coerceAtLeast(300)))))
                else f?.delete()
            }
        )
    }
    DisposableEffect(Unit) { onDispose { recorder?.stop() } }

    // Multi-clip: add from gallery (copied into the app's cache so drafts / export keep working) or record.
    fun addClip(uri: Uri, label: String) {
        scope.launch {
            val copy = withContext(Dispatchers.IO) {
                runCatching {
                    val f = java.io.File(ctx.cacheDir, "reel_sources/clip_${System.currentTimeMillis()}.mp4").apply { parentFile?.mkdirs() }
                    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { i.copyTo(it) } } ?: return@runCatching null
                    f
                }.getOrNull()
            }
            val u = copy?.let(Uri::fromFile) ?: uri
            val dur = withContext(Dispatchers.IO) { ReelEditor.durationMs(ctx, u) }
            if (dur < ReelEdit.MIN_CLIP_MS) { Toast.makeText(ctx, "That clip is too short.", Toast.LENGTH_SHORT).show(); return@launch }
            editor.commit(editor.edit.addSource(ReelSource(u.toString(), dur, label)))
            editor.selectedClip = editor.edit.clips.lastIndex
        }
    }
    val clipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) addClip(u, "Clip ${editor.edit.sourceCount + 1}") }
    var pendingRec by remember { mutableStateOf<Uri?>(null) }
    val clipRecorder = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok -> val u = pendingRec; if (ok && u != null) addClip(u, "Recording") }
    fun launchClipRecorder() {
        val f = java.io.File(ctx.cacheDir, "reel_rec_${System.currentTimeMillis()}.mp4")
        val u = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f); pendingRec = u
        runCatching { clipRecorder.launch(u) }.onFailure { Toast.makeText(ctx, "No camera app available", Toast.LENGTH_SHORT).show() }
    }
    val camPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) launchClipRecorder() }

    BackHandler {
        when {
            editor.exportProgress != null -> cancelExport?.invoke()
            voice.recording -> voice.onStop()
            else -> onClose()
        }
    }

    fun applyCredit(e: ReelEdit) {
        val old = state.reelEdit?.sound?.credit
        if (e.sound != null && (state.credit.isBlank() || state.credit == old)) state.credit = e.sound.credit
        if (e.sound == null && old != null && state.credit == old) state.credit = ""
    }

    NoorReelEditor(
        state = editor, frame = frame, filmstrip = strip, sounds = sounds, onCancel = onClose,
        stickerLabels = composerStickerLabels(state),
        onCancelExport = { cancelExport?.invoke() },
        livePreview = if (live != null && editor.playing) { m -> AndroidView(factory = { c -> TextureView(c).also { live.attach(it) } }, modifier = m, onRelease = { live.detach(it) }) } else null,
        stickerSources = sources,
        onAddClip = { clipPicker.launch("video/*") },
        onRecordClip = {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchClipRecorder()
            else camPermission.launch(Manifest.permission.CAMERA)
        },
        voice = voice,
        onDone = { e ->
            editor.playing = false
            if (!ReelExportParams.needsFullExport(e)) {
                // Plain trim: keep the 1.1.1 path (trim happens at post time).
                applyCredit(e)
                state.sound = e.sound; state.soundMixed = false
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
                    state.sound = e.sound; state.soundMixed = e.sound != null
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
