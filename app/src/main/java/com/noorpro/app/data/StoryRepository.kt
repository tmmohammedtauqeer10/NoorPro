package com.noorpro.app.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.Date

/** A 24-hour Ummah story (1.0.75 images; 1.0.92 adds <=60 s video + shared-reel stories). */
data class UmmahStory(
    val id: String,
    val authorUid: String,
    val authorName: String,
    val authorHandle: String,
    val avatarUrl: String,
    val mediaUrl: String,
    val mediaPath: String,
    val caption: String,
    val createdAt: Long,
    val expiresAt: Long,
    val type: String = "image",
    val durationMs: Long = 0,
    val thumbUrl: String = "",
    val thumbPath: String = "",
    val sharedPostId: String = ""
) {
    val isVideo get() = type == "video"
    /** Image that represents the story in rings / previews. */
    val coverUrl get() = if (isVideo) thumbUrl else mediaUrl
}

/** Stories grouped per author for the row (Instagram-style). */
data class UmmahStoryGroup(val authorUid: String, val authorName: String, val authorHandle: String, val avatarUrl: String, val stories: List<UmmahStory>)

internal const val STORY_TTL_MS = 24L * 60 * 60 * 1000

internal fun groupStories(stories: List<UmmahStory>, myUid: String?, now: Long = System.currentTimeMillis()): List<UmmahStoryGroup> =
    stories.filter { it.expiresAt > now && it.mediaUrl.startsWith("https://") }
        .groupBy { it.authorUid }
        .map { (uid, list) ->
            val sorted = list.sortedBy { it.createdAt }
            val last = sorted.last()
            UmmahStoryGroup(uid, last.authorName, last.authorHandle, last.avatarUrl, sorted)
        }
        .sortedWith(compareByDescending<UmmahStoryGroup> { it.authorUid == myUid }.thenByDescending { it.stories.last().createdAt })

class StoryRepository {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val main = Handler(Looper.getMainLooper())

    fun currentUid(): String? = FirebaseGate.currentUser?.uid

    /** Live, signed-in-only listener for unexpired stories. Expired ones are hidden client-side too. */
    fun observeActive(onResult: (List<UmmahStory>) -> Unit): ListenerRegistration? {
        if (FirebaseGate.currentUser == null) { onResult(emptyList()); return null }
        return db.collection("ummah_stories")
            .whereGreaterThan("expiresAt", Timestamp.now())
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .limit(300)
            .addSnapshotListener { snap, _ ->
                val now = System.currentTimeMillis()
                onResult(snap?.documents.orEmpty().mapNotNull { d ->
                    val exp = d.getTimestamp("expiresAt")?.toDate()?.time ?: return@mapNotNull null
                    if (exp <= now) return@mapNotNull null
                    UmmahStory(
                        id = d.id,
                        authorUid = d.getString("authorUid").orEmpty(),
                        authorName = d.getString("authorName").orEmpty().ifBlank { "Member" },
                        authorHandle = d.getString("authorHandle").orEmpty(),
                        avatarUrl = d.getString("avatarUrl").orEmpty(),
                        mediaUrl = d.getString("mediaUrl").orEmpty(),
                        mediaPath = d.getString("mediaPath").orEmpty(),
                        caption = d.getString("caption").orEmpty(),
                        createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: (exp - STORY_TTL_MS),
                        expiresAt = exp,
                        type = d.getString("type").orEmpty().ifBlank { "image" },
                        durationMs = d.getLong("durationMs") ?: 0L,
                        thumbUrl = d.getString("thumbUrl").orEmpty(),
                        thumbPath = d.getString("thumbPath").orEmpty(),
                        sharedPostId = d.getString("sharedPostId").orEmpty()
                    )
                })
            }
    }

    fun createImageStory(
        context: Context, uri: Uri, caption: String, name: String, handle: String, avatarUrl: String,
        onProgress: (Float) -> Unit, onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseGate.currentUser ?: return onResult(false, "Please sign in to add a story.")
        val file = ImageCompressor.compress(context, uri, maxSide = 1600)
        val path = "ummah_stories/${user.uid}/${System.currentTimeMillis()}.jpg"
        val ref = FirebaseStorage.getInstance().reference.child(path)
        ref.putFile(file, StorageMetadata.Builder().setContentType("image/jpeg").build())
            .addOnProgressListener { s -> if (s.totalByteCount > 0) onProgress(s.bytesTransferred.toFloat() / s.totalByteCount) }
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    db.collection("ummah_stories").add(
                        mapOf(
                            "authorUid" to user.uid,
                            "authorName" to name.take(60),
                            "authorHandle" to handle.take(40),
                            "avatarUrl" to avatarUrl.take(500),
                            "mediaUrl" to url.toString(),
                            "mediaPath" to path,
                            "type" to "image",
                            "caption" to caption.trim().take(300),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "expiresAt" to Timestamp(Date(System.currentTimeMillis() + STORY_TTL_MS - 60_000L))
                        )
                    ).addOnSuccessListener { main.post { onResult(true, "Story shared for 24 hours.") } }
                        .addOnFailureListener { ref.delete(); main.post { onResult(false, "Couldn't share your story. Try again.") } }
                }.addOnFailureListener { main.post { onResult(false, "Upload failed. Try again.") } }
            }
            .addOnFailureListener { main.post { onResult(false, "Upload failed. Check your connection.") } }
    }

    /**
     * 1.0.92: video story from gallery/camera. Trims to the first 60 s and re-encodes to 720p H.264
     * (media3 Transformer), extracts a poster frame, uploads both, then writes the story doc.
     */
    fun createVideoStory(
        context: Context, uri: Uri, caption: String, name: String, handle: String, avatarUrl: String,
        onProgress: (Float) -> Unit, onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseGate.currentUser ?: return onResult(false, "Please sign in to add a story.")
        val ts = System.currentTimeMillis()
        val out = java.io.File(context.cacheDir, "story_$ts.mp4")
        StoryVideo.compress(context, uri, out, onProgress = { onProgress(it * 0.5f) }) { ok ->
            if (!ok || !out.exists() || out.length() == 0L) { main.post { onResult(false, "Couldn't process this video. Try a shorter clip.") }; return@compress }
            val dur = StoryVideo.durationMs(out).coerceIn(1, 60_000)
            val thumb = StoryVideo.poster(context, out)
            val path = "ummah_stories/${user.uid}/$ts.mp4"
            val thumbPath = "ummah_stories/${user.uid}/${ts}_poster.jpg"
            val storage = FirebaseStorage.getInstance().reference
            fun writeDoc(url: String, thumbUrl: String, tPath: String) {
                db.collection("ummah_stories").add(
                    mapOf(
                        "authorUid" to user.uid, "authorName" to name.take(60), "authorHandle" to handle.take(40),
                        "avatarUrl" to avatarUrl.take(500), "mediaUrl" to url, "mediaPath" to path, "type" to "video",
                        "caption" to caption.trim().take(300), "durationMs" to dur, "thumbUrl" to thumbUrl, "thumbPath" to tPath,
                        "sharedPostId" to "", "createdAt" to FieldValue.serverTimestamp(),
                        "expiresAt" to Timestamp(Date(System.currentTimeMillis() + STORY_TTL_MS - 60_000L))
                    )
                ).addOnSuccessListener { out.delete(); main.post { onResult(true, "Video story shared for 24 hours.") } }
                    .addOnFailureListener { storage.child(path).delete(); if (tPath.isNotBlank()) storage.child(tPath).delete(); main.post { onResult(false, "Couldn't share your story. Try again.") } }
            }
            val ref = storage.child(path)
            ref.putFile(Uri.fromFile(out), StorageMetadata.Builder().setContentType("video/mp4").build())
                .addOnProgressListener { s -> if (s.totalByteCount > 0) onProgress(0.5f + 0.5f * s.bytesTransferred / s.totalByteCount) }
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        if (thumb == null) return@addOnSuccessListener writeDoc(url.toString(), "", "")
                        val tRef = storage.child(thumbPath)
                        tRef.putFile(Uri.fromFile(thumb), StorageMetadata.Builder().setContentType("image/jpeg").build())
                            .continueWithTask { tRef.downloadUrl }
                            .addOnCompleteListener { t -> thumb.delete(); if (t.isSuccessful) writeDoc(url.toString(), t.result.toString(), thumbPath) else writeDoc(url.toString(), "", "") }
                    }.addOnFailureListener { main.post { onResult(false, "Upload failed. Try again.") } }
                }
                .addOnFailureListener { main.post { onResult(false, "Upload failed. Check your connection.") } }
        }
    }

    /** 1.0.92: add an existing Ummah reel to my story (no re-upload; plays the reel's own video, first 60 s). */
    fun createReelStory(
        postId: String, mediaUrl: String, thumbUrl: String, durationMs: Long, caption: String,
        name: String, handle: String, avatarUrl: String, onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseGate.currentUser ?: return onResult(false, "Please sign in to add a story.")
        if (!mediaUrl.startsWith("https://") || postId.isBlank()) return onResult(false, "This reel can't be added to a story.")
        db.collection("ummah_stories").add(
            mapOf(
                "authorUid" to user.uid, "authorName" to name.take(60), "authorHandle" to handle.take(40),
                "avatarUrl" to avatarUrl.take(500), "mediaUrl" to mediaUrl, "mediaPath" to "", "type" to "video",
                "caption" to caption.trim().take(300), "durationMs" to durationMs.coerceIn(1, 60_000),
                "thumbUrl" to thumbUrl.takeIf { it.startsWith("https://") }.orEmpty().take(2000), "thumbPath" to "",
                "sharedPostId" to postId, "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(Date(System.currentTimeMillis() + STORY_TTL_MS - 60_000L))
            )
        ).addOnSuccessListener { main.post { onResult(true, "Added to your story for 24 hours.") } }
            .addOnFailureListener { main.post { onResult(false, "Couldn't add to your story.") } }
    }

    fun delete(story: UmmahStory, onResult: (Boolean) -> Unit) {
        db.collection("ummah_stories").document(story.id).delete().addOnCompleteListener { t ->
            if (t.isSuccessful && story.mediaPath.isNotBlank()) FirebaseStorage.getInstance().reference.child(story.mediaPath).delete()
            if (t.isSuccessful && story.thumbPath.isNotBlank()) FirebaseStorage.getInstance().reference.child(story.thumbPath).delete()
            onResult(t.isSuccessful)
        }
    }

    /** Records that the signed-in member viewed a story (one doc per viewer; only the author can list them). */
    fun markViewed(story: UmmahStory) {
        val uid = currentUid() ?: return
        if (uid == story.authorUid) return
        db.collection("ummah_stories").document(story.id).collection("viewers").document(uid)
            .set(mapOf("viewedAt" to FieldValue.serverTimestamp()))
    }

    fun viewerCount(story: UmmahStory, onResult: (Int) -> Unit) {
        db.collection("ummah_stories").document(story.id).collection("viewers").get()
            .addOnSuccessListener { onResult(it.size()) }.addOnFailureListener { onResult(-1) }
    }
}

/** Per-device "seen" state so rings turn gray after viewing. */
class StorySeenStore(context: Context) {
    private val prefs = context.getSharedPreferences("story_seen", Context.MODE_PRIVATE)
    fun isSeen(id: String) = prefs.contains(id)
    fun markSeen(id: String) {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(id, now).apply()
        // Prune entries older than 2 days.
        prefs.all.filter { (it.value as? Long ?: 0L) < now - 2 * STORY_TTL_MS }.keys.forEach { prefs.edit().remove(it).apply() }
    }
}

/** 1.0.92: media3 Transformer helpers for story videos (trim <=60 s, 720p H.264 + AAC). */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object StoryVideo {
    const val MAX_MS = 60_000L

    fun compress(context: Context, uri: Uri, out: java.io.File, onProgress: (Float) -> Unit, onDone: (Boolean) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        main.post {
            runCatching {
                val item = androidx.media3.common.MediaItem.Builder().setUri(uri)
                    .setClippingConfiguration(androidx.media3.common.MediaItem.ClippingConfiguration.Builder().setEndPositionMs(MAX_MS).build())
                    .build()
                val edited = androidx.media3.transformer.EditedMediaItem.Builder(item)
                    .setEffects(androidx.media3.transformer.Effects(emptyList(), listOf(androidx.media3.effect.Presentation.createForHeight(720))))
                    .build()
                val encoder = androidx.media3.transformer.DefaultEncoderFactory.Builder(context.applicationContext)
                    .setRequestedVideoEncoderSettings(androidx.media3.transformer.VideoEncoderSettings.Builder().setBitrate(2_500_000).build())
                    .build()
                var finished = false
                val transformer = androidx.media3.transformer.Transformer.Builder(context.applicationContext)
                    .setVideoMimeType(androidx.media3.common.MimeTypes.VIDEO_H264)
                    .setAudioMimeType(androidx.media3.common.MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoder)
                    .addListener(object : androidx.media3.transformer.Transformer.Listener {
                        override fun onCompleted(c: androidx.media3.transformer.Composition, r: androidx.media3.transformer.ExportResult) { finished = true; onDone(true) }
                        override fun onError(c: androidx.media3.transformer.Composition, r: androidx.media3.transformer.ExportResult, e: androidx.media3.transformer.ExportException) { finished = true; out.delete(); onDone(false) }
                    })
                    .build()
                transformer.start(edited, out.absolutePath)
                val holder = androidx.media3.transformer.ProgressHolder()
                main.post(object : Runnable {
                    override fun run() {
                        if (finished) return
                        if (transformer.getProgress(holder) == androidx.media3.transformer.Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress / 100f)
                        main.postDelayed(this, 300)
                    }
                })
            }.onFailure { onDone(false) }
        }
    }

    fun durationMs(file: java.io.File): Long = runCatching {
        val r = android.media.MediaMetadataRetriever(); r.setDataSource(file.absolutePath)
        val d = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L; r.release(); d
    }.getOrDefault(0L)

    fun poster(context: Context, file: java.io.File): java.io.File? = runCatching {
        val r = android.media.MediaMetadataRetriever(); r.setDataSource(file.absolutePath)
        val bmp = r.getFrameAtTime(500_000) ?: r.frameAtTime; r.release()
        val f = java.io.File(context.cacheDir, "story_poster_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { bmp!!.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, it) }; f
    }.getOrNull()

    /** Probe a remote/local video's length (for reel re-shares). Runs on a background thread. */
    fun probeDurationMs(url: String): Long = runCatching {
        val r = android.media.MediaMetadataRetriever(); r.setDataSource(url, HashMap())
        val d = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L; r.release(); d
    }.getOrDefault(0L)
}
