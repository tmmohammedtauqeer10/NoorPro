package com.noorpro.app.data.onboarding

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

/** Result of the post-login check. [complete] false => show onboarding starting at the first missing step. */
data class OnboardingStatus(val draft: OnboardingDraft, val complete: Boolean)

class UsernameTakenException : Exception("That username is taken.")

/** 1.2.0 (115) onboarding data access (Firestore). Private details never go to the public profile. */
class OnboardingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val uid get() = auth.currentUser?.uid

    private fun privateRef(u: String) = db.collection("users").document(u).collection("private").document("profile")

    /** Loads what this account already has (private doc, public card, Google name/photo). Throws when offline. */
    suspend fun load(today: SimpleDate): OnboardingStatus? {
        val user = auth.currentUser ?: return null
        val p = privateRef(user.uid).get().await()
        val pub = runCatching { db.collection("ummah_profiles").document(user.uid).get().await() }.getOrNull()
        val held = p.getString("username").orEmpty()
        val heldOk = held.isNotBlank() && runCatching { db.collection("usernames").document(held).get().await().getString("uid") == user.uid }.getOrDefault(false)
        val suggested = UsernameRules.normalize(pub?.getString("handle").orEmpty()).takeIf { UsernameRules.isValid(it) }.orEmpty()
        val draft = OnboardingDraft(
            fullName = p.getString("fullName").orEmpty().ifBlank { pub?.getString("name").orEmpty() }.ifBlank { user.displayName.orEmpty() },
            username = if (heldOk) held else suggested,
            reservedUsername = if (heldOk) held else "",
            gender = p.getString("gender")?.let { OnbGender.of(it) },
            profileType = ProfileType.of(p.getString("profileType")),
            dob = SimpleDate.parse(p.getString("dob")),
            country = p.getString("country").orEmpty(), state = p.getString("state").orEmpty(), city = p.getString("city").orEmpty(),
            street = p.getString("street").orEmpty(), postalCode = p.getString("postalCode").orEmpty(),
            showLocation = p.getBoolean("showLocation") ?: true,
            language = p.getString("language").orEmpty(),
            photoUrl = user.photoUrl?.toString().orEmpty(),
        )
        // A username that isn't reserved by this account in usernames/ still needs the username step.
        val complete = heldOk && p.getBoolean("completed") == true && OnboardingRules.isComplete(draft, today)
        return OnboardingStatus(draft, complete)
    }

    suspend fun isAvailable(name: String): Boolean {
        val u = uid ?: return false
        val d = db.collection("usernames").document(name).get().await()
        return !d.exists() || d.getString("uid") == u
    }

    /**
     * Reserves usernames/{name} for this account in a transaction (fails with [UsernameTakenException] if someone
     * else holds it) and releases the previous one this account held.
     */
    suspend fun reserve(name: String, previous: String) {
        val u = uid ?: error("Not signed in")
        db.runTransaction { tx ->
            val ref = db.collection("usernames").document(name)
            val snap = tx.get(ref)
            val prevRef = previous.takeIf { it.isNotBlank() && it != name }?.let { db.collection("usernames").document(it) }
            val prevSnap = prevRef?.let { tx.get(it) }
            if (snap.exists() && snap.getString("uid") != u) throw UsernameTakenException()
            if (!snap.exists()) tx.set(ref, mapOf("uid" to u, "at" to FieldValue.serverTimestamp()))
            if (prevRef != null && prevSnap?.getString("uid") == u) tx.delete(prevRef)
            null
        }.await()
    }

    /** Saves the private doc and the public card. */
    suspend fun save(d: OnboardingDraft, today: SimpleDate) {
        val u = uid ?: error("Not signed in")
        val batch = db.batch()
        batch.set(privateRef(u), OnboardingRules.privateDoc(d, today) + mapOf("completed" to true, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        batch.set(db.collection("ummah_profiles").document(u), OnboardingRules.publicFields(d, today) + mapOf("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        batch.commit().await()
    }
}

/** 1.2.0 (115) scholar applications + admin review. */
class ScholarApplicationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: () -> FirebaseStorage = { FirebaseStorage.getInstance() },
) {
    val myUid get() = auth.currentUser?.uid

    private fun parse(id: String, m: Map<String, Any?>?): ScholarApplication? {
        if (m == null) return null
        fun s(k: String) = (m[k] as? String).orEmpty()
        fun ms(k: String) = (m[k] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
        return ScholarApplication(
            uid = id, name = s("name"), title = s("title"), institution = s("institution"), maslak = Maslak.of(s("maslak")), silsila = s("silsila"),
            years = (m["years"] as? Number)?.toInt(), refName = s("refName"), refPhone = s("refPhone"), bio = s("bio"), certPath = s("certPath"),
            status = ApplicationStatus.of(s("status")), reason = s("reason"), submittedAt = ms("submittedAt"), decidedAt = ms("decidedAt"),
        )
    }

    suspend fun mine(): ScholarApplication? {
        val u = myUid ?: return null
        val d = db.collection("scholar_applications").document(u).get().await()
        return if (d.exists()) parse(d.id, d.data) else null
    }

    /** Uploads the certificate (private to you and admins), then writes the application with status pending. */
    suspend fun submit(a: ScholarApplication, cert: Uri?, mime: String?, ext: String) {
        val u = myUid ?: error("Please sign in first.")
        var path = a.certPath
        if (cert != null) {
            path = ScholarRules.certPath(u, ext, System.currentTimeMillis())
            storage().reference.child(path).putFile(cert, StorageMetadata.Builder().setContentType(mime ?: "image/jpeg").build()).await()
        }
        db.collection("scholar_applications").document(u)
            .set(ScholarRules.applicationDoc(a, u, path) + mapOf("submittedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun pending(): List<ScholarApplication> =
        db.collection("scholar_applications").whereEqualTo("status", ApplicationStatus.PENDING.code).limit(100).get().await()
            .documents.mapNotNull { parse(it.id, it.data) }.sortedBy { it.submittedAt }

    suspend fun verifiedScholars(): List<ScholarApplication> =
        db.collection("scholar_applications").whereEqualTo("status", ApplicationStatus.APPROVED.code).limit(200).get().await()
            .documents.mapNotNull { parse(it.id, it.data) }.sortedByDescending { it.decidedAt }

    suspend fun certificateUrl(path: String): String? = runCatching { storage().reference.child(path).downloadUrl.await().toString() }.getOrNull()

    /** Approve: scholars/{uid} verified + application approved in one batch; then the profile badge (best effort). */
    suspend fun approve(a: ScholarApplication) {
        val me = myUid ?: error("Not signed in")
        val b = db.batch()
        b.set(db.collection("scholars").document(a.uid), ScholarRules.scholarDoc(a) + mapOf("approvedBy" to me, "approvedAt" to FieldValue.serverTimestamp()))
        b.update(db.collection("scholar_applications").document(a.uid), mapOf("status" to ApplicationStatus.APPROVED.code, "reason" to "", "decidedBy" to me, "decidedAt" to FieldValue.serverTimestamp()))
        b.commit().await()
        runCatching { db.collection("ummah_profiles").document(a.uid).update(mapOf("scholarBadge" to true)).await() }
    }

    suspend fun reject(a: ScholarApplication, reason: String) {
        val me = myUid ?: error("Not signed in")
        db.collection("scholar_applications").document(a.uid)
            .update(mapOf("status" to ApplicationStatus.REJECTED.code, "reason" to reason.trim(), "decidedBy" to me, "decidedAt" to FieldValue.serverTimestamp())).await()
    }

    /** Revoke: scholars/{uid}.verified = false (Ask a Scholar stops accepting their answers) + status revoked + badge off. */
    suspend fun revoke(a: ScholarApplication, reason: String) {
        val me = myUid ?: error("Not signed in")
        val b = db.batch()
        b.set(db.collection("scholars").document(a.uid), ScholarRules.scholarDoc(a) + mapOf("verified" to false, "approvedBy" to me, "approvedAt" to FieldValue.serverTimestamp(), "revokedReason" to reason.trim()))
        b.update(db.collection("scholar_applications").document(a.uid), mapOf("status" to ApplicationStatus.REVOKED.code, "reason" to reason.trim(), "decidedBy" to me, "decidedAt" to FieldValue.serverTimestamp()))
        b.commit().await()
        runCatching { db.collection("ummah_profiles").document(a.uid).update(mapOf("scholarBadge" to false)).await() }
    }
}
