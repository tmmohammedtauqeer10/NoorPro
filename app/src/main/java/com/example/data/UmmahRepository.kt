package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata

data class UmmahPost(
    val id: String,
    val type: String,
    val mediaUrl: String,
    val hlsUrl: String = "",
    val thumbnailUrl: String,
    val creatorUid: String,
    val creatorName: String,
    val creatorHandle: String,
    val creatorPhotoUrl: String = "",
    val caption: String,
    val arabicText: String = "",
    val sourceReference: String,
    val publishedAt: Long,
    val category: String = "Reminder",
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val saveCount: Long = 0,
    val mediaWidth: Int = 0,
    val mediaHeight: Int = 0,
    val mediaRotationDegrees: Int = 0,
    val publicationStatus: String = "published",
    val storageProvider: String = ""
)

/** A member shown in a followers / following list (Instagram-style). */
data class UmmahFollowUser(
    val uid: String,
    val name: String,
    val handle: String
)

/** Public profile card for a member. Posts/reels overlay this live so a profile edit
 *  (name, username, photo) updates everywhere instantly — like Instagram/Facebook. */
data class UmmahProfile(
    val uid: String,
    val name: String,
    val handle: String,
    val photoUrl: String,
    val bio: String = "",
    // Privacy flags, published so other clients can respect them.
    val isPrivate: Boolean = false,
    val hideCounts: Boolean = false
)

data class UmmahChat(
    val id: String,
    val participantUids: List<String>,
    val participantNames: Map<String, String>,
    val lastMessage: String,
    val lastMessageType: String,
    val lastSenderUid: String,
    val updatedAt: Long,
    val unreadCount: Long = 0
)

data class UmmahGroup(
    val id: String,
    val name: String,
    val description: String,
    val ownerUid: String,
    val memberUids: List<String>,
    val memberNames: Map<String, String> = emptyMap(),
    val lastMessage: String,
    val updatedAt: Long
)

data class UmmahMessage(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val text: String,
    val mediaUrl: String,
    val type: String,
    val createdAt: Long,
    val replyToMessageId: String = "",
    val replyToName: String = "",
    val replyToText: String = "",
    val replyToType: String = "",
    val reactions: Map<String, String> = emptyMap()
)

data class UmmahComment(
    val id: String,
    val postId: String,
    val text: String,
    val creatorUid: String,
    val creatorName: String,
    val createdAt: Long,
    val parentCommentId: String = "",
    val replyToName: String = ""
)

data class UmmahSavedCollection(
    val id: String,
    val title: String,
    val createdAt: Long
)

private const val MAX_UMMAH_MEDIA_BYTES = 100L * 1024L * 1024L

/**
 * Some Android document providers report videos as application/octet-stream (or no MIME type).
 * Normalize from the selected post type so AWS receives a supported video type and Firebase
 * Storage receives metadata accepted by storage.rules.
 */
internal fun normalizedUmmahContentType(postType: String, reportedType: String?): String {
    val normalized = reportedType.orEmpty().trim().lowercase()
    return when (postType.lowercase()) {
        "image" -> normalized.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        "reel", "video" -> normalized.takeIf { it.startsWith("video/") } ?: "video/mp4"
        else -> normalized.ifBlank { "application/octet-stream" }
    }
}

private fun contentLength(context: Context, uri: Uri): Long {
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                cursor.getLong(index).takeIf { it >= 0L }?.let { return it }
            }
        }
    }
    runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0L }?.let { return it }
        }
    }
    return -1L
}

class UmmahRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private var submissionListener: ListenerRegistration? = null
    private val interactionListeners = mutableListOf<ListenerRegistration>()
    private val chatListeners = mutableListOf<ListenerRegistration>()
    private val blockedListeners = mutableListOf<ListenerRegistration>()
    private val publicationRequestedIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val publicationRetryAttempts = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val publicationRetryHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** A creator can interact with a reel during its brief submission-to-publication window.
     * Retry public counters so those first likes, saves, shares, and views are not lost. */
    private fun incrementPublishedCounter(
        postId: String,
        field: String,
        delta: Long = 1L,
        attempt: Int = 0,
        onResult: (Boolean) -> Unit = {}
    ) {
        firestore.collection("ummah_posts").document(postId)
            .update(field, com.google.firebase.firestore.FieldValue.increment(delta))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true)
                } else if (attempt < 6) {
                    publicationRetryHandler.postDelayed(
                        {
                            incrementPublishedCounter(
                                postId = postId,
                                field = field,
                                delta = delta,
                                attempt = attempt + 1,
                                onResult = onResult
                            )
                        },
                        ((attempt + 1) * 1_500L).coerceAtMost(6_000L)
                    )
                } else {
                    onResult(false)
                }
            }
    }

    /** Retry the trusted publisher for temporary function/network failures. The Firestore trigger
     * remains the primary publisher; this owner-authenticated endpoint recovers uploads after a
     * cold start or interrupted request. */
    private fun recoverPendingPublication(submissionId: String) {
        if (!publicationRequestedIds.add(submissionId)) return
        ReelCdnUploader.requestPublication(submissionId) { published ->
            if (published) {
                publicationRetryAttempts.remove(submissionId)
                return@requestPublication
            }
            publicationRequestedIds.remove(submissionId)
            val attempt = publicationRetryAttempts.merge(submissionId, 1) { current, increment ->
                current + increment
            } ?: 1
            if (attempt <= 6) {
                val delayMs = (attempt * 5_000L).coerceAtMost(30_000L)
                publicationRetryHandler.postDelayed(
                    { recoverPendingPublication(submissionId) },
                    delayMs
                )
            }
        }
    }

    fun currentUserUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun chatIdFor(otherUid: String): String? {
        val uid = currentUserUid() ?: return null
        return listOf(uid, otherUid.trim()).sorted().joinToString("_")
    }

    fun observeApprovedPosts(onResult: (List<UmmahPost>, String?) -> Unit) {
        listener?.remove()
        submissionListener?.remove()
        var publishedPosts = emptyList<UmmahPost>()
        var mySubmissions = emptyList<UmmahPost>()
        var publishedError: String? = null
        var submissionError: String? = null

        fun emit() {
            // A published post and its original submission use the same document ID. Published
            // data comes last so it replaces the temporary pending copy as soon as the backend
            // promotion finishes.
            val merged = (mySubmissions + publishedPosts)
                .associateBy { it.id }
                .values
                .sortedByDescending { it.publishedAt }
            val message = if (merged.isEmpty()) publishedError ?: submissionError else null
            onResult(merged, message)
        }

        listener = firestore.collection("ummah_posts")
            .whereEqualTo("approved", true)
            .whereEqualTo("status", "published")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    publishedError = "Community feed unavailable: ${error.localizedMessage}"
                    emit()
                    return@addSnapshotListener
                }
                publishedError = null
                publishedPosts = snapshot?.documents.orEmpty().mapNotNull { document ->
                    // ummah_posts is the published-only collection. Keep this
                    // check as defense in depth for accidentally copied drafts.
                    if (document.contains("approved") && document.getBoolean("approved") != true) {
                        return@mapNotNull null
                    }
                    document.toUmmahPost()
                }.filter { it.caption.isNotBlank() || it.arabicText.isNotBlank() || it.mediaUrl.isNotBlank() }
                emit()
            }

        // Always show the signed-in creator their own successful upload immediately. This keeps
        // Reels and Profile useful even while the trusted backend publisher is cold-starting or
        // has not yet been deployed. Security rules expose only this user's submissions.
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            submissionListener = firestore.collection("ummah_submissions")
                .whereEqualTo("creatorUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        submissionError = "Your uploads are temporarily unavailable: ${error.localizedMessage}"
                        emit()
                        return@addSnapshotListener
                    }
                    submissionError = null
                    val visibleSubmissions = snapshot?.documents.orEmpty()
                        .filter { it.getString("status") !in setOf("rejected", "removed") }
                    visibleSubmissions
                        .filter { it.getString("status") != "published" }
                        .forEach { document ->
                            recoverPendingPublication(document.id)
                        }
                    visibleSubmissions
                        .filter { it.getString("status") == "published" }
                        .forEach { document ->
                            publicationRequestedIds.remove(document.id)
                            publicationRetryAttempts.remove(document.id)
                        }
                    mySubmissions = visibleSubmissions
                        .map { it.toUmmahPost(fallbackTimestamp = System.currentTimeMillis()) }
                        .filter { it.caption.isNotBlank() || it.arabicText.isNotBlank() || it.mediaUrl.isNotBlank() }
                    emit()
                }
        }
    }

    private fun DocumentSnapshot.toUmmahPost(fallbackTimestamp: Long = 0L): UmmahPost = UmmahPost(
        id = id,
        type = getString("type")?.lowercase() ?: "image",
        mediaUrl = getString("mediaUrl").orEmpty(),
        hlsUrl = getString("hlsUrl").orEmpty(),
        thumbnailUrl = getString("thumbnailUrl").orEmpty(),
        creatorUid = getString("creatorUid").orEmpty(),
        creatorName = getString("creatorName") ?: "Noor Pro",
        creatorHandle = getString("creatorHandle") ?: "@noorpro",
        creatorPhotoUrl = getString("creatorPhotoUrl").orEmpty(),
        caption = getString("caption").orEmpty(),
        arabicText = getString("arabicText").orEmpty(),
        sourceReference = getString("sourceReference").orEmpty(),
        publishedAt = getTimestamp("publishedAt")?.toDate()?.time
            ?: getTimestamp("submittedAt")?.toDate()?.time
            ?: getTimestamp("createdAt")?.toDate()?.time
            ?: fallbackTimestamp,
        category = getString("category") ?: "Reminder",
        likeCount = getLong("likeCount") ?: getLong("likesCount") ?: 0L,
        commentCount = getLong("commentCount") ?: getLong("commentsCount") ?: 0L,
        shareCount = getLong("shareCount") ?: getLong("sharesCount") ?: 0L,
        viewCount = getLong("viewCount") ?: getLong("viewsCount") ?: 0L,
        saveCount = getLong("saveCount") ?: getLong("savesCount") ?: 0L,
        mediaWidth = (get("mediaWidth") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
        mediaHeight = (get("mediaHeight") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
        mediaRotationDegrees = (get("mediaRotationDegrees") as? Number)?.toInt() ?: 0,
        publicationStatus = getString("status") ?: "published",
        storageProvider = getString("storageProvider").orEmpty()
    )

    fun observeUserInteractions(onResult: (liked: Set<String>, saved: Set<String>) -> Unit) {
        interactionListeners.forEach { it.remove() }
        interactionListeners.clear()
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptySet(), emptySet())
            return
        }
        var liked = emptySet<String>()
        var saved = emptySet<String>()
        interactionListeners += firestore.collection("ummah_users").document(user.uid).collection("likes")
            .addSnapshotListener { snapshot, _ ->
                liked = snapshot?.documents.orEmpty().map { it.id }.toSet()
                onResult(liked, saved)
            }
        interactionListeners += firestore.collection("ummah_users").document(user.uid).collection("saved")
            .addSnapshotListener { snapshot, _ ->
                saved = snapshot?.documents.orEmpty().map { it.id }.toSet()
                onResult(liked, saved)
            }
    }

    fun toggleInteraction(postId: String, type: String, active: Boolean, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val document = firestore.collection("ummah_users").document(user.uid).collection(type).document(postId)
        if (type == "likes" || type == "saved") {
            // Persist the member's interaction independently from the public post counter. A freshly
            // uploaded AWS reel exists first in ummah_submissions, so reading/updating a missing
            // ummah_posts document made the whole transaction fail and the icon reverted. The
            // counter is deliberately best-effort; the interaction itself is the source of truth.
            firestore.runTransaction { transaction ->
                val alreadyActive = transaction.get(document).exists()
                var delta = 0L
                when {
                    active && !alreadyActive -> {
                        transaction.set(
                            document,
                            mapOf(
                                "postId" to postId,
                                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                        )
                        delta = 1L
                    }
                    !active && alreadyActive -> {
                        transaction.delete(document)
                        delta = -1L
                    }
                }
                delta
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val delta = task.result ?: 0L
                    if (delta != 0L) {
                        val counterField = if (type == "likes") "likeCount" else "saveCount"
                        incrementPublishedCounter(postId, counterField, delta)
                    }
                }
                onResult(task.isSuccessful)
            }
            return
        }
        val task = if (active) {
            document.set(
                mapOf("postId" to postId, "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } else {
            document.delete()
        }
        task.addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun incrementShare(postId: String, onResult: (Boolean) -> Unit = {}) {
        val cleanPostId = postId.trim()
        if (cleanPostId.isBlank()) return onResult(false)
        incrementPublishedCounter(cleanPostId, "shareCount", onResult = onResult)
    }

    /** Count a real view: +1 to the post's viewCount. Fire-and-forget; the live feed listener then
     *  reflects the new count everywhere (search, profile, Creator Studio). */
    fun incrementViews(postId: String) {
        val cleanPostId = postId.trim()
        if (cleanPostId.isBlank()) return
        incrementPublishedCounter(cleanPostId, "viewCount")
    }

    fun observeSavedCollections(onResult: (List<UmmahSavedCollection>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptyList())
            return
        }
        interactionListeners += firestore.collection("ummah_users").document(user.uid).collection("saved_collections")
            .addSnapshotListener { snapshot, _ ->
                val collections = snapshot?.documents.orEmpty().map { document ->
                    UmmahSavedCollection(
                        id = document.id,
                        title = document.getString("title").orEmpty().ifBlank { "Collection" },
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }.sortedWith(compareBy<UmmahSavedCollection> { it.createdAt == 0L }.thenBy { it.createdAt }.thenBy { it.title })
                onResult(collections)
            }
    }

    fun observeSavedAssignments(onResult: (Map<String, String>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptyMap())
            return
        }
        interactionListeners += firestore.collection("ummah_users").document(user.uid).collection("saved")
            .addSnapshotListener { snapshot, _ ->
                val assignments = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val collectionId = document.getString("collectionId").orEmpty()
                    if (collectionId.isBlank()) null else document.id to collectionId
                }.toMap()
                onResult(assignments)
            }
    }

    fun createSavedCollection(title: String, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val cleanTitle = title.trim().take(36)
        if (cleanTitle.isBlank()) return onResult(false)
        firestore.collection("ummah_users").document(user.uid).collection("saved_collections")
            .add(
                mapOf(
                    "title" to cleanTitle,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun assignSavedToCollection(postId: String, collectionId: String, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val cleanCollectionId = collectionId.trim()
        if (postId.isBlank() || cleanCollectionId.isBlank()) return onResult(false)
        firestore.collection("ummah_users").document(user.uid).collection("saved").document(postId)
            .set(
                mapOf(
                    "postId" to postId,
                    "collectionId" to cleanCollectionId,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    /** Live map of uid → public profile. Posts overlay these so profile edits propagate
     *  to every reel/post instantly without rewriting old documents. */
    fun observeProfiles(onResult: (Map<String, UmmahProfile>) -> Unit) {
        interactionListeners += firestore.collection("ummah_profiles")
            .addSnapshotListener { snapshot, _ ->
                val profiles = snapshot?.documents.orEmpty().associate { doc ->
                    doc.id to UmmahProfile(
                        uid = doc.id,
                        name = doc.getString("name").orEmpty(),
                        handle = doc.getString("handle").orEmpty(),
                        photoUrl = doc.getString("photoUrl").orEmpty(),
                        bio = doc.getString("bio").orEmpty(),
                        isPrivate = doc.getBoolean("isPrivate") ?: false,
                        hideCounts = doc.getBoolean("hideCounts") ?: false
                    )
                }
                onResult(profiles)
            }
    }

    /** Publish/refresh the signed-in member's public profile card. Fire-and-forget. */
    fun upsertMyProfile(name: String, handle: String, photoUrl: String, bio: String? = null) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val cleanHandle = handle.trim().removePrefix("@")
        val updates = mutableMapOf<String, Any>(
            "name" to name.trim().ifBlank { user.displayName ?: "Community member" },
            "handle" to (if (cleanHandle.isBlank()) user.email?.substringBefore("@").orEmpty() else cleanHandle),
            "photoUrl" to photoUrl.trim(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        if (bio != null) updates["bio"] = bio.trim().take(160)
        firestore.collection("ummah_profiles").document(user.uid).set(
            updates,
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    /** Publish the signed-in member's privacy flags onto their public profile so other clients can
     *  respect them (private account, hidden counts). Fire-and-forget. NOTE: full enforcement
     *  (blocking non-followers from reading data) also needs matching Firestore security rules. */
    fun updateMyPrivacy(isPrivate: Boolean, hideCounts: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        firestore.collection("ummah_profiles").document(user.uid).set(
            mapOf(
                "isPrivate" to isPrivate,
                "hideCounts" to hideCounts,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    fun observeFollowing(onResult: (Set<String>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptySet())
            return
        }
        interactionListeners += firestore.collection("ummah_users").document(user.uid).collection("following")
            .addSnapshotListener { snapshot, _ ->
                onResult(snapshot?.documents.orEmpty().map { it.id }.toSet())
            }
    }

    /** Users [targetUid] follows (defaults to the signed-in member), with name + handle for a list view. */
    fun observeFollowingUsers(targetUid: String? = null, onResult: (List<UmmahFollowUser>) -> Unit) {
        val target = targetUid?.trim()?.ifBlank { null } ?: FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onResult(emptyList())
            return
        }
        interactionListeners += firestore.collection("ummah_users").document(target).collection("following")
            .addSnapshotListener { snapshot, _ ->
                onResult(snapshot?.documents.orEmpty().map { doc ->
                    UmmahFollowUser(
                        uid = doc.getString("uid") ?: doc.id,
                        name = doc.getString("name") ?: "Community member",
                        handle = doc.getString("handle") ?: "@ummah"
                    )
                })
            }
    }

    /** Members who follow [uid] (defaults to the signed-in user). Followers are public to read. */
    fun observeFollowers(uid: String? = null, onResult: (List<UmmahFollowUser>) -> Unit) {
        val target = uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onResult(emptyList())
            return
        }
        interactionListeners += firestore.collection("ummah_users").document(target).collection("followers")
            .addSnapshotListener { snapshot, _ ->
                onResult(snapshot?.documents.orEmpty().map { doc ->
                    UmmahFollowUser(
                        uid = doc.getString("uid") ?: doc.id,
                        name = doc.getString("name") ?: "Community member",
                        handle = doc.getString("handle") ?: "@ummah"
                    )
                })
            }
    }

    fun setFollowing(
        creatorUid: String,
        creatorName: String,
        creatorHandle: String,
        following: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val target = creatorUid.trim()
        if (target.isBlank() || target == user.uid) return onResult(false)
        val document = firestore.collection("ummah_users").document(user.uid).collection("following").document(target)
        val task = if (following) {
            document.set(
                mapOf(
                    "uid" to target,
                    "name" to creatorName.ifBlank { "Community member" },
                    "handle" to creatorHandle.ifBlank { "@ummah" },
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        } else {
            document.delete()
        }
        task.addOnCompleteListener { onResult(it.isSuccessful) }

        // Best-effort reverse index so the target gets a real Followers list (Instagram-style).
        // Fire-and-forget: if it's denied (rules not yet deployed) the follow above still succeeds.
        val followerDoc = firestore.collection("ummah_users").document(target)
            .collection("followers").document(user.uid)
        if (following) {
            followerDoc.set(
                mapOf(
                    "uid" to user.uid,
                    "name" to (user.displayName ?: "Community member"),
                    "handle" to "@${user.email?.substringBefore("@").orEmpty()}",
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        } else {
            followerDoc.delete()
        }
    }

    fun submitPost(
        context: Context,
        caption: String,
        arabicText: String,
        category: String,
        type: String,
        mediaUri: Uri?,
        sourceReference: String,
        creatorHandle: String = "",
        creatorDisplayName: String = "",
        creatorPhotoUrlOverride: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in before posting.")
        val resolvedHandle = creatorHandle.trim().ifBlank { "@${user.email?.substringBefore("@").orEmpty()}" }
            .let { if (it.startsWith("@")) it else "@$it" }
        val resolvedName = creatorDisplayName.trim().ifBlank { user.displayName ?: "Community member" }
        val resolvedPhotoUrl = creatorPhotoUrlOverride.trim().ifBlank { user.photoUrl?.toString().orEmpty() }
        // Keep the public profile card fresh so this creator's identity stays current everywhere.
        upsertMyProfile(
            name = resolvedName,
            handle = resolvedHandle,
            photoUrl = resolvedPhotoUrl
        )
        if (caption.isBlank() && arabicText.isBlank() && mediaUri == null) return onResult(false, "Write a caption, dua, reminder, or Arabic text.")
        if (type != "text" && mediaUri == null) return onResult(false, "Choose media before submitting.")
        val videoMetadata = if (mediaUri != null && (type == "reel" || type == "video")) {
            readReelVideoMetadata(context, mediaUri)
        } else {
            ReelVideoMetadata()
        }

        fun publishPost(
            mediaUrl: String,
            mediaPath: String = "",
            hlsUrl: String = "",
            storageProvider: String = "firebase"
        ) {
            val publishedCaption = caption.trim().ifBlank {
                if (mediaUrl.isNotBlank()) "Shared with the Ummah" else ""
            }
            val thumbnailUrl = if (type == "image") mediaUrl else ""
            // The client writes an authenticated submission. A trusted Firestore trigger validates
            // it, then publishes it into ummah_posts so the reel appears in the live feed.
            firestore.collection("ummah_submissions").add(
                mapOf(
                    "caption" to publishedCaption,
                    "arabicText" to arabicText.trim(),
                    "category" to category,
                    "mediaUrl" to mediaUrl,
                    "hlsUrl" to hlsUrl,
                    "thumbnailUrl" to thumbnailUrl,
                    "mediaPath" to mediaPath,
                    "storageProvider" to storageProvider,
                    "sourceReference" to sourceReference.trim(),
                    "type" to type,
                    "creatorUid" to user.uid,
                    "creatorName" to resolvedName,
                    "creatorHandle" to resolvedHandle,
                    "creatorPhotoUrl" to resolvedPhotoUrl,
                    "approved" to false,
                    "status" to "pending",
                    "uploadedMedia" to mediaPath.isNotBlank(),
                    "likeCount" to 0,
                    "commentCount" to 0,
                    "shareCount" to 0,
                    "viewCount" to 0,
                    "saveCount" to 0,
                    "mediaWidth" to videoMetadata.width,
                    "mediaHeight" to videoMetadata.height,
                    "mediaRotationDegrees" to videoMetadata.rotationDegrees,
                    "displayAspectRatio" to videoMetadata.displayAspectRatio.toDouble(),
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "submittedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener { document ->
                ReelCdnUploader.requestPublication(document.id)
                // Keep storage-provider and backend workflow details out of the member experience.
                val message = if (type == "reel" || type == "video") {
                    "Reel uploaded. It will appear shortly."
                } else {
                    "Published to Ummah."
                }
                onResult(true, message)
            }.addOnFailureListener { error ->
                onResult(false, error.localizedMessage ?: "Post submit failed.")
            }
        }

        if (mediaUri == null) {
            publishPost("")
            return
        }

        val contentType = normalizedUmmahContentType(type, context.contentResolver.getType(mediaUri))
        val mediaSize = contentLength(context, mediaUri)
        if (mediaSize >= MAX_UMMAH_MEDIA_BYTES) {
            return onResult(false, "This file is too large. Choose a photo or video smaller than 100 MB.")
        }

        // Upload the raw bytes to Firebase Storage. Used directly for images, and as the fallback
        // for videos when the AWS/CloudFront pipeline is off or unreachable.
        fun uploadViaFirebase() {
            val folder = if (type == "image") "images" else "videos"
            val ref = FirebaseStorage.getInstance().reference
                .child("ummah_submissions/${user.uid}/$folder/${System.currentTimeMillis()}")
            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .build()
            ref.putFile(mediaUri, metadata)
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { downloadUri -> publishPost(downloadUri.toString(), ref.path) }
                        .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Could not read uploaded media URL.") }
                }
                .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Media upload failed.") }
        }

        // Reels/videos: try the AWS S3 + CloudFront pipeline first (fast, cheap playback). On any
        // failure or when it's disabled, fall back to Firebase Storage so uploads never break.
        if (ReelCdnUploader.ENABLED && (type == "reel" || type == "video")) {
            ReelCdnUploader.upload(context, mediaUri, contentType, videoMetadata) { result ->
                if (result != null) {
                    publishPost(result.mp4Url, hlsUrl = result.hlsUrl, storageProvider = "aws")
                } else {
                    uploadViaFirebase()
                }
            }
            return
        }

        uploadViaFirebase()
    }

    fun report(postId: String, reason: String, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false)
            return
        }
        firestore.collection("ummah_reports").add(
            mapOf(
                "postId" to postId,
                "reason" to reason,
                "reporterUid" to user.uid,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).addOnCompleteListener { onResult(it.isSuccessful) }
    }

    /** Live set of UIDs the signed-in user has blocked. Empty for signed-out users. */
    fun observeBlockedUsers(onResult: (Set<String>) -> Unit) {
        blockedListeners.forEach { it.remove() }
        blockedListeners.clear()
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptySet())
            return
        }
        blockedListeners += firestore.collection("ummah_users").document(user.uid).collection("blocked")
            .addSnapshotListener { snapshot, _ ->
                onResult(snapshot?.documents.orEmpty().map { it.id }.toSet())
            }
    }

    /** Block (or unblock) a creator so their posts are hidden from this user's feed. */
    fun setBlocked(uid: String, blocked: Boolean, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val target = uid.trim()
        if (target.isBlank() || target == user.uid) return onResult(false)
        val document = firestore.collection("ummah_users").document(user.uid).collection("blocked").document(target)
        val task = if (blocked) {
            document.set(
                mapOf(
                    "uid" to target,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        } else {
            document.delete()
        }
        task.addOnCompleteListener { onResult(it.isSuccessful) }
    }

    /** Live list of published comments for a post. Returns the registration so the caller
     *  can remove it (comment sheets open and close often — avoid leaking listeners). */
    fun observeComments(postId: String, onResult: (List<UmmahComment>) -> Unit): ListenerRegistration {
        return firestore.collection("ummah_comments")
            .whereEqualTo("postId", postId)
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.documents.orEmpty().mapNotNull { document ->
                    if (document.getString("status") == "removed") return@mapNotNull null
                    UmmahComment(
                        id = document.id,
                        postId = postId,
                        text = document.getString("text").orEmpty(),
                        creatorUid = document.getString("creatorUid").orEmpty(),
                        creatorName = document.getString("creatorName") ?: "Community member",
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        parentCommentId = document.getString("parentCommentId").orEmpty(),
                        replyToName = document.getString("replyToName").orEmpty()
                    )
                }.sortedBy { it.createdAt }
                onResult(comments)
            }
    }

    fun observeUserComments(onResult: (List<UmmahComment>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptyList())
            return
        }
        interactionListeners += firestore.collection("ummah_comments")
            .whereEqualTo("creatorUid", user.uid)
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.documents.orEmpty().mapNotNull { document ->
                    if (document.getString("status") == "removed") return@mapNotNull null
                    UmmahComment(
                        id = document.id,
                        postId = document.getString("postId").orEmpty(),
                        text = document.getString("text").orEmpty(),
                        creatorUid = document.getString("creatorUid").orEmpty(),
                        creatorName = document.getString("creatorName") ?: "Community member",
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        parentCommentId = document.getString("parentCommentId").orEmpty(),
                        replyToName = document.getString("replyToName").orEmpty()
                    )
                }.sortedByDescending { it.createdAt }
                onResult(comments)
            }
    }

    /** Delete a post the signed-in user owns, plus best-effort removal of its uploaded media. */
    fun deletePost(post: UmmahPost, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        if (post.creatorUid.isBlank() || post.creatorUid != user.uid) return onResult(false)
        firestore.collection("ummah_posts").document(post.id).delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && post.mediaUrl.startsWith("http")) {
                    runCatching { FirebaseStorage.getInstance().getReferenceFromUrl(post.mediaUrl).delete() }
                }
                onResult(task.isSuccessful)
            }
    }

    fun submitComment(
        postId: String,
        text: String,
        parentCommentId: String = "",
        replyToName: String = "",
        onResult: (Boolean) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val cleanText = text.trim()
        if (postId.isBlank() || cleanText.isBlank()) return onResult(false)
        val commentRef = firestore.collection("ummah_comments").document()
        commentRef.set(
            mapOf(
                "postId" to postId,
                "text" to cleanText,
                "creatorUid" to user.uid,
                "creatorName" to (user.displayName ?: "Community member"),
                "parentCommentId" to parentCommentId.trim(),
                "replyToName" to replyToName.trim(),
                "status" to "published",
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Pending AWS reels do not have a public post document yet. Keep the successful
                // comment and update the public counter only when that document is available.
                firestore.collection("ummah_posts").document(postId)
                    .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1))
            }
            onResult(task.isSuccessful)
        }
    }

    fun observeChats(onResult: (List<UmmahChat>, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptyList(), "Please sign in to use Friends chat.")
            return
        }
        val registration = firestore.collection("ummah_chats")
            .whereArrayContains("participantUids", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList(), error.localizedMessage ?: "Unable to load chats.")
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents.orEmpty().map { document ->
                    @Suppress("UNCHECKED_CAST")
                    val names = document.get("participantNames") as? Map<String, String> ?: emptyMap()
                    UmmahChat(
                        id = document.id,
                        participantUids = document.get("participantUids") as? List<String> ?: emptyList(),
                        participantNames = names,
                        lastMessage = document.getString("lastMessage").orEmpty(),
                        lastMessageType = document.getString("lastMessageType").orEmpty(),
                        lastSenderUid = document.getString("lastSenderUid").orEmpty(),
                        updatedAt = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                        unreadCount = document.getLong("unread_${user.uid}") ?: 0L
                    )
                }.sortedByDescending { it.updatedAt }
                onResult(chats, null)
            }
        chatListeners += registration
    }

    /** Clear the signed-in user's unread badge after they open a 1:1 conversation. */
    fun markChatRead(chatId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (chatId.isBlank()) return
        firestore.collection("ummah_chats").document(chatId)
            .update("unread_$uid", 0L)
    }

    fun observeGroups(onResult: (List<UmmahGroup>, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(emptyList(), "Please sign in to use community groups.")
            return
        }
        val registration = firestore.collection("ummah_groups")
            .whereArrayContains("memberUids", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList(), error.localizedMessage ?: "Unable to load groups.")
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents.orEmpty().map { document ->
                    @Suppress("UNCHECKED_CAST")
                    UmmahGroup(
                        id = document.id,
                        name = document.getString("name").orEmpty().ifBlank { "Ummah Group" },
                        description = document.getString("description").orEmpty(),
                        ownerUid = document.getString("ownerUid").orEmpty(),
                        memberUids = document.get("memberUids") as? List<String> ?: emptyList(),
                        memberNames = document.get("memberNames") as? Map<String, String> ?: emptyMap(),
                        lastMessage = document.getString("lastMessage").orEmpty(),
                        updatedAt = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.updatedAt }
                onResult(groups, null)
            }
        chatListeners += registration
    }

    fun createGroup(name: String, description: String, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in to create a group.")
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) return onResult(false, "Add a group name.")
        firestore.collection("ummah_groups").add(
            mapOf(
                "name" to cleanName,
                "description" to description.trim().take(180),
                "ownerUid" to user.uid,
                "memberUids" to listOf(user.uid),
                "memberNames" to mapOf(user.uid to (user.displayName ?: user.email?.substringBefore("@") ?: "Owner")),
                "lastMessage" to "Group created",
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).addOnCompleteListener { task ->
            onResult(task.isSuccessful, task.exception?.localizedMessage)
        }
    }

    /** Discover public community groups to search & join (not filtered to your memberships). */
    fun observeDiscoverGroups(onResult: (List<UmmahGroup>, String?) -> Unit) {
        val registration = firestore.collection("ummah_groups")
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(80)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList(), error.localizedMessage ?: "Unable to load groups.")
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents.orEmpty().map { document ->
                    @Suppress("UNCHECKED_CAST")
                    UmmahGroup(
                        id = document.id,
                        name = document.getString("name").orEmpty().ifBlank { "Ummah Group" },
                        description = document.getString("description").orEmpty(),
                        ownerUid = document.getString("ownerUid").orEmpty(),
                        memberUids = document.get("memberUids") as? List<String> ?: emptyList(),
                        memberNames = document.get("memberNames") as? Map<String, String> ?: emptyMap(),
                        lastMessage = document.getString("lastMessage").orEmpty(),
                        updatedAt = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }
                onResult(groups, null)
            }
        chatListeners += registration
    }

    /** Join a public group by adding yourself to its member list (Instagram/WhatsApp-style). */
    fun joinGroup(groupId: String, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in to join groups.")
        if (groupId.isBlank()) return onResult(false, "Invalid group.")
        val name = user.displayName ?: user.email?.substringBefore("@") ?: "Member"
        firestore.collection("ummah_groups").document(groupId).update(
            mapOf(
                "memberUids" to com.google.firebase.firestore.FieldValue.arrayUnion(user.uid),
                "memberNames.${user.uid}" to name,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).addOnCompleteListener { task ->
            onResult(task.isSuccessful, task.exception?.localizedMessage)
        }
    }

    /** Owner adds members to a group (WhatsApp-style). [members] maps uid → display name. */
    fun addGroupMembers(groupId: String, members: Map<String, String>, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in.")
        val cleanMembers = members.filterKeys { it.isNotBlank() }
        if (cleanMembers.isEmpty()) return onResult(false, "Choose at least one member.")
        val groupRef = firestore.collection("ummah_groups").document(groupId)
        val nameUpdates = cleanMembers.entries.associate { (uid, name) ->
            "memberNames.$uid" to name.ifBlank { "Member" }
        }
        groupRef.update(
            mapOf(
                "memberUids" to com.google.firebase.firestore.FieldValue.arrayUnion(*cleanMembers.keys.toTypedArray()),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ) + nameUpdates
        ).addOnCompleteListener { task ->
            onResult(task.isSuccessful, task.exception?.localizedMessage)
        }
    }

    /** Delete your own message from a 1:1 chat. Firestore rules enforce sender-only deletion. */
    fun deleteChatMessage(chatId: String, messageId: String) {
        if (chatId.isBlank() || messageId.isBlank()) return
        firestore.collection("ummah_chats").document(chatId).collection("messages").document(messageId).delete()
    }

    /** Delete your own message from a group chat. Firestore rules enforce sender-only deletion. */
    fun deleteGroupMessage(groupId: String, messageId: String) {
        if (groupId.isBlank() || messageId.isBlank()) return
        firestore.collection("ummah_groups").document(groupId).collection("messages").document(messageId).delete()
    }

    /** Toggle an emoji reaction (keyed by your uid) on a 1:1 message. Empty emoji removes it. */
    fun reactToChatMessage(chatId: String, messageId: String, emoji: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (chatId.isBlank() || messageId.isBlank()) return
        val ref = firestore.collection("ummah_chats").document(chatId).collection("messages").document(messageId)
        val value: Any = if (emoji.isBlank()) com.google.firebase.firestore.FieldValue.delete() else emoji
        ref.update("reactions.$uid", value)
    }

    /** Toggle an emoji reaction (keyed by your uid) on a group message. Empty emoji removes it. */
    fun reactToGroupMessage(groupId: String, messageId: String, emoji: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (groupId.isBlank() || messageId.isBlank()) return
        val ref = firestore.collection("ummah_groups").document(groupId).collection("messages").document(messageId)
        val value: Any = if (emoji.isBlank()) com.google.firebase.firestore.FieldValue.delete() else emoji
        ref.update("reactions.$uid", value)
    }

    fun observeMessages(chatId: String, onResult: (List<UmmahMessage>, String?) -> Unit) {
        val registration = firestore.collection("ummah_chats").document(chatId).collection("messages")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList(), error.localizedMessage ?: "Unable to load messages.")
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().map { document ->
                    UmmahMessage(
                        id = document.id,
                        senderUid = document.getString("senderUid").orEmpty(),
                        senderName = document.getString("senderName").orEmpty(),
                        text = document.getString("text").orEmpty(),
                        mediaUrl = document.getString("mediaUrl").orEmpty(),
                        type = document.getString("type") ?: "text",
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        replyToMessageId = document.getString("replyToMessageId").orEmpty(),
                        replyToName = document.getString("replyToName").orEmpty(),
                        replyToText = document.getString("replyToText").orEmpty(),
                        replyToType = document.getString("replyToType").orEmpty(),
                        reactions = (document.get("reactions") as? Map<String, String>) ?: emptyMap()
                    )
                }
                onResult(messages, null)
            }
        chatListeners += registration
    }

    fun observeGroupMessages(groupId: String, onResult: (List<UmmahMessage>, String?) -> Unit) {
        val registration = firestore.collection("ummah_groups").document(groupId).collection("messages")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList(), error.localizedMessage ?: "Unable to load group messages.")
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().map { document ->
                    UmmahMessage(
                        id = document.id,
                        senderUid = document.getString("senderUid").orEmpty(),
                        senderName = document.getString("senderName").orEmpty(),
                        text = document.getString("text").orEmpty(),
                        mediaUrl = document.getString("mediaUrl").orEmpty(),
                        type = document.getString("type") ?: "text",
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        replyToMessageId = document.getString("replyToMessageId").orEmpty(),
                        replyToName = document.getString("replyToName").orEmpty(),
                        replyToText = document.getString("replyToText").orEmpty(),
                        replyToType = document.getString("replyToType").orEmpty(),
                        reactions = (document.get("reactions") as? Map<String, String>) ?: emptyMap()
                    )
                }
                onResult(messages, null)
            }
        chatListeners += registration
    }

    fun sendGroupMessage(groupId: String, text: String, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in to message.")
        val cleanText = text.trim()
        if (cleanText.isBlank()) return onResult(false, "Write a message.")
        val groupRef = firestore.collection("ummah_groups").document(groupId)
        firestore.runBatch { batch ->
            batch.update(
                groupRef,
                mapOf(
                    "lastMessage" to cleanText.take(180),
                    "lastSenderUid" to user.uid,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            batch.set(
                groupRef.collection("messages").document(),
                mapOf(
                    "senderUid" to user.uid,
                    "senderName" to (user.displayName ?: "Community member"),
                    "text" to cleanText.take(2000),
                    "mediaUrl" to "",
                    "type" to "text",
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
        }.addOnCompleteListener { task ->
            onResult(task.isSuccessful, task.exception?.localizedMessage)
        }
    }

    fun sendGroupMessage(
        context: Context,
        groupId: String,
        text: String,
        type: String,
        mediaUri: Uri?,
        sharedMediaUrl: String = "",
        replyToMessageId: String = "",
        replyToName: String = "",
        replyToText: String = "",
        replyToType: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in to message.")
        if (groupId.isBlank()) return onResult(false, "Open a group first.")

        fun writeMessage(mediaUrl: String) {
            val cleanType = when (type) {
                "image", "video", "audio", "location", "file", "pdf" -> type
                else -> "text"
            }
            val preview = text.ifBlank {
                when (cleanType) {
                    "image" -> "Photo"
                    "video" -> "Video"
                    "audio" -> "Voice message"
                    "location" -> "Location"
                    "pdf" -> "PDF"
                    "file" -> "File"
                    else -> "Message"
                }
            }
            val groupRef = firestore.collection("ummah_groups").document(groupId)
            firestore.runBatch { batch ->
                batch.update(
                    groupRef,
                    mapOf(
                        "lastMessage" to preview.take(180),
                        "lastSenderUid" to user.uid,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    groupRef.collection("messages").document(),
                    mapOf(
                        "senderUid" to user.uid,
                        "senderName" to (user.displayName ?: "Community member"),
                        "text" to text.trim(),
                        "mediaUrl" to mediaUrl,
                        "type" to cleanType,
                        "replyToMessageId" to replyToMessageId.trim(),
                        "replyToName" to replyToName.trim().take(80),
                        "replyToText" to replyToText.trim().take(220),
                        "replyToType" to replyToType.trim().take(24),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
        }

        if (sharedMediaUrl.isNotBlank()) {
            writeMessage(sharedMediaUrl)
            return
        }
        if (mediaUri == null) {
            if (text.isBlank()) return onResult(false, "Write a message or choose media.")
            writeMessage("")
            return
        }

        val contentType = context.contentResolver.getType(mediaUri).orEmpty()
        val folder = when {
            type == "audio" || contentType.startsWith("audio/") -> "audios"
            type == "image" || contentType.startsWith("image/") -> "images"
            type == "pdf" || contentType == "application/pdf" -> "pdfs"
            type == "file" -> "files"
            else -> "videos"
        }
        val fallbackContentType = when (folder) {
            "images" -> "image/jpeg"
            "audios" -> "audio/mp4"
            "pdfs" -> "application/pdf"
            "files" -> "application/octet-stream"
            else -> "video/mp4"
        }
        val ref = FirebaseStorage.getInstance().reference
            .child("ummah_groups/$groupId/${user.uid}/$folder/${System.currentTimeMillis()}")
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType.ifBlank { fallbackContentType })
            .build()
        ref.putFile(mediaUri, metadata)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { uri -> writeMessage(uri.toString()) }
                    .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Could not read group media URL.") }
            }
            .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Group media upload failed.") }
    }

    fun sendChatMessage(
        context: Context,
        otherUid: String,
        otherName: String,
        text: String,
        type: String,
        mediaUri: Uri?,
        sharedMediaUrl: String = "",
        replyToMessageId: String = "",
        replyToName: String = "",
        replyToText: String = "",
        replyToType: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "Please sign in to chat.")
        val targetUid = otherUid.trim()
        if (targetUid.isBlank()) return onResult(false, "Enter a friend user ID.")
        val chatId = listOf(user.uid, targetUid).sorted().joinToString("_")
        val participantNames = mapOf(
            user.uid to (user.displayName ?: user.email?.substringBefore("@") ?: "You"),
            targetUid to otherName.ifBlank { "Friend" }
        )

        fun writeMessage(mediaUrl: String) {
            val cleanType = when (type) {
                "image", "video", "reel", "audio", "location", "file", "pdf" -> type
                else -> "text"
            }
            val preview = text.ifBlank {
                when (cleanType) {
                    "image" -> "Photo"
                    "video" -> "Video"
                    "reel" -> "Reel"
                    "audio" -> "Voice message"
                    "location" -> "Location"
                    "pdf" -> "PDF"
                    "file" -> "File"
                    else -> "Message"
                }
            }
            val chatRef = firestore.collection("ummah_chats").document(chatId)
            firestore.runBatch { batch ->
                batch.set(
                    chatRef,
                    mapOf(
                        "participantUids" to listOf(user.uid, targetUid),
                        "participantNames" to participantNames,
                        "lastMessage" to preview.take(180),
                        "lastMessageType" to cleanType,
                        "lastSenderUid" to user.uid,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "unread_$targetUid" to com.google.firebase.firestore.FieldValue.increment(1)
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                batch.set(
                    chatRef.collection("messages").document(),
                    mapOf(
                        "senderUid" to user.uid,
                        "senderName" to (user.displayName ?: "Community member"),
                        "text" to text.trim(),
                        "mediaUrl" to mediaUrl,
                        "type" to cleanType,
                        "replyToMessageId" to replyToMessageId.trim(),
                        "replyToName" to replyToName.trim().take(80),
                        "replyToText" to replyToText.trim().take(220),
                        "replyToType" to replyToType.trim().take(24),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
            }.addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
        }

        if (sharedMediaUrl.isNotBlank()) {
            writeMessage(sharedMediaUrl)
            return
        }
        if (mediaUri == null) {
            if (text.isBlank()) return onResult(false, "Write a message or choose media.")
            writeMessage("")
            return
        }
        val contentType = context.contentResolver.getType(mediaUri).orEmpty()
        val folder = when {
            type == "audio" || contentType.startsWith("audio/") -> "audios"
            type == "image" || contentType.startsWith("image/") -> "images"
            type == "pdf" || contentType == "application/pdf" -> "pdfs"
            type == "file" -> "files"
            else -> "videos"
        }
        val fallbackContentType = when (folder) {
            "images" -> "image/jpeg"
            "audios" -> "audio/mp4"
            "pdfs" -> "application/pdf"
            "files" -> "application/octet-stream"
            else -> "video/mp4"
        }
        val ref = FirebaseStorage.getInstance().reference
            .child("ummah_chats/$chatId/${user.uid}/$folder/${System.currentTimeMillis()}")
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType.ifBlank { fallbackContentType })
            .build()
        ref.putFile(mediaUri, metadata)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { uri -> writeMessage(uri.toString()) }
                    .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Could not read chat media URL.") }
            }
            .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Chat media upload failed.") }
    }

    fun close() {
        listener?.remove()
        listener = null
        submissionListener?.remove()
        submissionListener = null
        interactionListeners.forEach { it.remove() }
        interactionListeners.clear()
        chatListeners.forEach { it.remove() }
        chatListeners.clear()
        blockedListeners.forEach { it.remove() }
        blockedListeners.clear()
    }
}
