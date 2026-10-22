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

/** A 24-hour Ummah story (1.0.75). Images only; see report for video stories. */
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
    val expiresAt: Long
)

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
    private val db = FirebaseFirestore.getInstance()
    private val main = Handler(Looper.getMainLooper())

    fun currentUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    /** Live, signed-in-only listener for unexpired stories. Expired ones are hidden client-side too. */
    fun observeActive(onResult: (List<UmmahStory>) -> Unit): ListenerRegistration? {
        if (FirebaseAuth.getInstance().currentUser == null) { onResult(emptyList()); return null }
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
                        expiresAt = exp
                    )
                })
            }
    }

    fun createImageStory(
        context: Context, uri: Uri, caption: String, name: String, handle: String, avatarUrl: String,
        onProgress: (Float) -> Unit, onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in to add a story.")
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

    fun delete(story: UmmahStory, onResult: (Boolean) -> Unit) {
        db.collection("ummah_stories").document(story.id).delete().addOnCompleteListener { t ->
            if (t.isSuccessful && story.mediaPath.isNotBlank()) FirebaseStorage.getInstance().reference.child(story.mediaPath).delete()
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
