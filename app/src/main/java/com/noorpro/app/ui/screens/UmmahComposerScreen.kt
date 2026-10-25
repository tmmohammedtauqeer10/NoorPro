package com.noorpro.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.data.ComposerDestination
import com.noorpro.app.data.ComposerDraft
import com.noorpro.app.data.ComposerDraftStore
import com.noorpro.app.data.ComposerFlags
import com.noorpro.app.data.ComposerLaunch
import com.noorpro.app.data.ComposerLimits
import com.noorpro.app.data.ReelEditor
import com.noorpro.app.data.StoryRepository
import com.noorpro.app.data.UmmahRepository
import com.noorpro.app.data.composerExtraFields
import com.noorpro.app.data.search.PeopleRanker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 1.1.1: create screen for feed posts, reels and stories. Wires NoorComposer to the pickers,
 * the camera, local drafts and the existing (unchanged) upload calls.
 */
@Composable
fun NoorCreateScreen(viewModel: DeenViewModel, reelMode: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 1.1.2: "Your story" + in the Ummah stories row preselects Story (consumed once).
    val launchDestination = remember { ComposerLaunch.consume() }
    if (!viewModel.isLoggedIn) {
        StitchSignInRequiredScreen(
            title = when { launchDestination == ComposerDestination.STORY -> "New story"; reelMode -> "New reel"; else -> "New post" },
            message = "Please sign in before sharing posts or reels.",
            onBack = { viewModel.goBack() },
            onSignIn = { viewModel.navigateTo(DeenScreen.LOGIN) }
        )
        return
    }
    LaunchedEffect(Unit) { ComposerFlags.load() }
    val state = remember { ComposerUiState(launchDestination ?: if (reelMode) ComposerDestination.REEL else ComposerDestination.FEED) }
    val repository = remember { UmmahRepository() }
    val store = remember { ComposerDraftStore(context) }
    var drafts by remember { mutableStateOf(store.all()) }
    val termsGate = rememberUgcTermsGate()
    var confirmLeave by remember { mutableStateOf(false) }

    // People for @mentions — same profiles + follow graph the people search uses.
    val profiles = rememberStitchProfiles()
    val following = rememberStitchFollowing(true)
    val me = remember { repository.currentUserUid().orEmpty() }
    val people = remember(profiles, following) {
        profiles.values.filter { it.uid != me && it.handle.isNotBlank() }
            .map { PeopleRanker.Person(it.uid, it.name, it.handle.removePrefix("@"), it.photoUrl, following = it.uid in following) }
    }

    fun setVideo(uri: Uri) {
        state.video = uri; state.photos = emptyList(); state.coverFrame = null
        state.reelSource = null; state.reelEdit = null; state.photoOrigins = emptyMap()
        state.trimStartMs = 0; state.trimEndMs = 0; state.coverMs = 0; state.coverTouched = false
        scope.launch {
            val d = withContext(Dispatchers.IO) { ReelEditor.durationMs(context, uri) }
            state.videoDurationMs = d
            state.trimEndMs = d
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val max = if (state.destination == ComposerDestination.STORY) 1 else ComposerLimits.MAX_PHOTOS
        if (state.photos.size + uris.size > max) Toast.makeText(context, "Up to $max photo${if (max == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
        state.photos = (state.photos + uris).distinct().take(max)
        state.video = null; state.coverFrame = null; state.reelSource = null; state.reelEdit = null
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) setVideo(uri) }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }
    val recorder = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val u = pendingCapture
        if (ok && u != null) setVideo(u) else if (!ok) Toast.makeText(context, "Recording cancelled", Toast.LENGTH_SHORT).show()
    }
    fun launchRecorder() {
        val file = File(context.cacheDir, "reel_rec_${System.currentTimeMillis()}.mp4")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCapture = uri
        runCatching { recorder.launch(uri) }.onFailure { Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show() }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchRecorder() else Toast.makeText(context, "Camera permission is needed to record", Toast.LENGTH_SHORT).show()
    }

    // Cover / preview frame follows the cover slider (debounced), or the trim start before a cover is picked.
    LaunchedEffect(state.video, state.coverMs, state.trimStartMs, state.coverTouched) {
        val v = state.video ?: return@LaunchedEffect
        delay(150)
        val at = if (state.coverTouched) state.coverMs else state.trimStartMs
        withContext(Dispatchers.IO) { ReelEditor.frameAt(context, v, at) }?.let { state.coverFrame = it }
    }

    fun saveDraft(then: () -> Unit = {}) {
        val id = state.draftId ?: "d${System.currentTimeMillis()}"
        val draft = state.toDraft(id)
        scope.launch {
            val saved = withContext(Dispatchers.IO) { store.save(draft) }
            state.load(saved.copy()) // switch to the private copies of the media
            state.draftId = saved.id
            drafts = store.all()
            Toast.makeText(context, "Draft saved", Toast.LENGTH_SHORT).show()
            then()
        }
    }

    fun finish(success: Boolean, message: String?, target: DeenScreen) {
        state.submitting = false; state.progress = -1f; state.stage = ""
        if (success) {
            state.draftId?.let { store.delete(it) }
            Toast.makeText(context, message ?: "Shared", Toast.LENGTH_LONG).show()
            viewModel.navigateTo(target)
        } else state.error = message ?: "Couldn't share. Your post is still here — try again."
    }

    fun post() {
        if (!termsGate.check()) return
        if (state.blocker(ComposerFeatures()) != null) return
        state.submitting = true; state.error = null
        val caption = state.text.text.trim()
        val arabic = if (state.verseOpen) state.arabic.trim() else ""
        val reference = if (state.verseOpen) state.reference.trim() else ""
        val handle = "@${viewModel.ummahUsername.ifBlank { viewModel.userEmail.substringBefore("@") }}"
        val progress: (Float) -> Unit = { p -> android.os.Handler(android.os.Looper.getMainLooper()).post { state.progress = p } }
        when (state.destination) {
            ComposerDestination.STORY -> {
                state.stage = "Sharing your story…"
                val story = StoryRepository()
                val v = state.video
                if (v != null) story.createVideoStory(context, v, caption, viewModel.userDisplayName, handle, viewModel.userPhotoUrl, progress) { ok, m -> finish(ok, m, DeenScreen.UMMAH) }
                else story.createImageStory(context, state.photos.first(), caption, viewModel.userDisplayName, handle, viewModel.userPhotoUrl, progress) { ok, m -> finish(ok, m, DeenScreen.UMMAH) }
            }
            ComposerDestination.FEED -> {
                state.stage = "Sharing to the Ummah…"
                val v = state.video
                val type = when { v != null -> "video"; state.photos.isNotEmpty() -> "image"; else -> "text" }
                repository.submitPost(
                    context = context, caption = caption, arabicText = arabic, category = state.topic, type = type,
                    mediaUri = v ?: state.photos.firstOrNull(), sourceReference = reference,
                    creatorHandle = handle, creatorDisplayName = viewModel.userDisplayName, creatorPhotoUrlOverride = viewModel.userPhotoUrl,
                    onProgress = progress,
                    extraImageUris = if (type == "image") state.photos.drop(1) else emptyList(),
                    extraFields = composerExtraFields(if (state.pollOpen) state.poll else null, state.aiLabel, state.location, state.followersOnly)
                ) { ok, m -> finish(ok, m ?: "Published to Ummah", DeenScreen.UMMAH) }
            }
            ComposerDestination.REEL -> {
                val src = state.video ?: return
                val credit = state.credit.trim()
                val source = listOf(reference, if (credit.isNotBlank()) "Sound: $credit" else "").filter { it.isNotBlank() }.joinToString(" · ")
                fun submit(media: Uri, cover: String) {
                    state.stage = "Uploading your reel…"; state.progress = -1f
                    repository.submitPost(
                        context = context, caption = caption, arabicText = arabic, category = state.topic, type = "reel",
                        mediaUri = media, sourceReference = source,
                        creatorHandle = handle, creatorDisplayName = viewModel.userDisplayName, creatorPhotoUrlOverride = viewModel.userPhotoUrl,
                        onSubmitted = { id -> viewModel.reelFocusId = id }, onProgress = progress,
                        thumbnailUrlOverride = cover,
                        extraFields = composerExtraFields(null, state.aiLabel, state.location, state.followersOnly)
                    ) { ok, m -> finish(ok, m ?: "Reel shared", DeenScreen.REELS) }
                }
                fun withCover(media: Uri) {
                    val frame = state.coverFrame
                    if (!state.coverTouched || frame == null) return submit(media, "")
                    state.stage = "Saving your cover…"
                    scope.launch {
                        val f = withContext(Dispatchers.IO) { ReelEditor.saveCover(context, frame) }
                        if (f == null) submit(media, "") else repository.uploadCover(f) { url -> f.delete(); submit(media, url.orEmpty()) }
                    }
                }
                if (ReelEditor.needsTrim(state.trimStartMs, state.trimEndMs, state.videoDurationMs)) {
                    state.stage = "Trimming your clip…"
                    ReelEditor.trim(context, src, state.trimStartMs, state.trimEndMs, progress) { out ->
                        if (out == null) finish(false, "Couldn't trim this video. Try a different range or clip.", DeenScreen.REELS)
                        else withCover(Uri.fromFile(out))
                    }
                } else withCover(src)
            }
        }
    }

    // 1.1.3 in-app editors (shown over the composer; they only replace the media file).
    var editingPhoto by remember { mutableStateOf<Int?>(null) }
    var editingVideo by remember { mutableStateOf(false) }
    var editingCollage by remember { mutableStateOf(false) }

    BackHandler(enabled = state.draftsOpen || (state.hasContent && !state.submitting)) {
        if (state.draftsOpen) state.draftsOpen = false else confirmLeave = true
    }

    androidx.compose.foundation.layout.Box {
    NoorComposer(
        state = state,
        actions = ComposerActions(
            onBack = { if (state.hasContent && !state.submitting) confirmLeave = true else viewModel.goBack() },
            onPickPhotos = { photoPicker.launch("image/*") },
            onPickVideo = { videoPicker.launch("video/*") },
            onRecordVideo = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchRecorder()
                else cameraPermission.launch(Manifest.permission.CAMERA)
            },
            onSaveDraft = { saveDraft() },
            onOpenDraft = { d: ComposerDraft ->
                state.load(d); state.draftsOpen = false
                d.media.firstOrNull()?.takeIf { d.isVideo }?.let { u ->
                    scope.launch { state.videoDurationMs = withContext(Dispatchers.IO) { ReelEditor.durationMs(context, Uri.parse(u)) } }
                }
            },
            onDeleteDraft = { d -> store.delete(d.id); drafts = store.all(); if (state.draftId == d.id) state.draftId = null },
            onPost = { post() },
            onEditPhoto = { i -> if (!state.submitting) editingPhoto = i },
            onEditVideo = { if (!state.submitting && state.video != null) editingVideo = true },
            onCollage = { if (!state.submitting && state.photos.size >= 2) editingCollage = true }
        ),
        userName = viewModel.userDisplayName,
        userPhotoUrl = viewModel.userPhotoUrl,
        people = people,
        drafts = drafts
    )
    editingPhoto?.let { i -> PhotoEditorHost(state, i) { editingPhoto = null } }
    if (editingVideo) ReelEditorHost(state) { editingVideo = false }
    if (editingCollage) CollageEditorHost(state) { editingCollage = false }
    }

    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text("Keep this for later?") },
        text = { Text("Save it as a draft on this phone, or discard it.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; saveDraft { viewModel.goBack() } }) { Text("Save draft") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false; viewModel.goBack() }) { Text("Discard") } }
    )
}
