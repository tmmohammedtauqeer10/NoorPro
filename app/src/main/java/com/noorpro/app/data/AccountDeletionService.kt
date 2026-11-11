package com.noorpro.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Best-effort client-side erasure of everything an account owns, run BEFORE the Firebase Auth
 * user is deleted (afterwards the security rules no longer recognise the uid).
 *
 * Every step is independent and wrapped so one failure never blocks account deletion. Items the
 * security rules deliberately do not let a client remove (1:1 chat documents, pending
 * submissions, reels stored on the CDN) are purged by the operator on request; the public
 * deletion page documents this.
 */
object AccountDeletionService {
    private val chatMediaTypes = listOf("images", "videos", "audios", "files", "pdfs")

    suspend fun purgeUserData(uid: String, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        suspend fun deleteDocs(query: Query) {
            val docs = runCatching { query.get().await().documents }.getOrDefault(emptyList())
            docs.forEach { runCatching { it.reference.delete().await() } }
        }

        suspend fun deleteStorageFolder(ref: StorageReference) {
            val listing = runCatching { ref.listAll().await() }.getOrNull() ?: return
            listing.items.forEach { runCatching { it.delete().await() } }
            listing.prefixes.forEach { deleteStorageFolder(it) }
        }

        onProgress("Removing your posts and comments")
        // Reads of ummah_posts require approved + published, so the query states both.
        runCatching {
            deleteDocs(
                db.collection("ummah_posts")
                    .whereEqualTo("creatorUid", uid)
                    .whereEqualTo("approved", true)
                    .whereEqualTo("status", "published")
            )
        }
        runCatching { deleteDocs(db.collection("ummah_comments").whereEqualTo("creatorUid", uid)) }

        onProgress("Removing your messages")
        val chatIds = runCatching {
            db.collection("ummah_chats").whereArrayContains("participantUids", uid).get().await().documents.map { it.id }
        }.getOrDefault(emptyList())
        chatIds.forEach { chatId ->
            runCatching {
                deleteDocs(db.collection("ummah_chats").document(chatId).collection("messages").whereEqualTo("senderUid", uid))
            }
            chatMediaTypes.forEach { type ->
                runCatching { deleteStorageFolder(storage.reference.child("ummah_chats/$chatId/$uid/$type")) }
            }
        }
        val groups = runCatching {
            db.collection("ummah_groups").whereArrayContains("memberUids", uid).get().await().documents
        }.getOrDefault(emptyList())
        groups.forEach { group ->
            val groupId = group.id
            runCatching {
                deleteDocs(db.collection("ummah_groups").document(groupId).collection("messages").whereEqualTo("senderUid", uid))
            }
            chatMediaTypes.forEach { type ->
                runCatching { deleteStorageFolder(storage.reference.child("ummah_groups/$groupId/$uid/$type")) }
            }
            if (group.getString("ownerUid") == uid) {
                runCatching { group.reference.delete().await() }
            }
        }

        onProgress("Removing your uploads")
        listOf("images", "videos").forEach { type ->
            runCatching { deleteStorageFolder(storage.reference.child("ummah_submissions/$uid/$type")) }
        }
        runCatching { deleteStorageFolder(storage.reference.child("ummah_avatars/$uid")) }

        onProgress("Removing your profile and saved items")
        // Remove this account from the follower lists of people it followed.
        val followed = runCatching {
            db.collection("ummah_users").document(uid).collection("following").get().await().documents.map { it.id }
        }.getOrDefault(emptyList())
        followed.forEach { other ->
            runCatching { db.collection("ummah_users").document(other).collection("followers").document(uid).delete().await() }
        }
        listOf("likes", "saved", "saved_collections", "blocked", "following").forEach { sub ->
            runCatching { deleteDocs(db.collection("ummah_users").document(uid).collection(sub)) }
        }
        listOf("posts", "settings", "journeys", "savedPilgrimPlaces").forEach { sub ->
            runCatching { deleteDocs(db.collection("users").document(uid).collection(sub)) }
        }
        runCatching { db.collection("users").document(uid).delete().await() }
        runCatching { db.collection("user_backups").document(uid).delete().await() }
        runCatching { db.collection("ummah_profiles").document(uid).delete().await() }
    }

    /** True when the signed-in account uses an email + password provider. */
    fun usesPassword(): Boolean =
        FirebaseAuth.getInstance().currentUser?.providerData.orEmpty().any { it.providerId == "password" }
}
