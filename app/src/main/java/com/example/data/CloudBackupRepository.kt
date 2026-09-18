package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class CloudBackupSnapshot(
    val settingsJson: String,
    val updatedAtMillis: Long,
    val appVersion: String
)

class CloudBackupRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun backupSettings(settingsJson: String, appVersion: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onResult(false, "Please sign in to back up settings to cloud.")
        val payload = mapOf(
            "settingsJson" to settingsJson,
            "appVersion" to appVersion,
            "email" to user.email.orEmpty(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("user_backups").document(user.uid)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener { onResult(true, "Cloud backup saved.") }
            .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "Cloud backup failed.") }
    }

    fun restoreSettings(onResult: (CloudBackupSnapshot?, String?) -> Unit) {
        val user = auth.currentUser ?: return onResult(null, "Please sign in to restore cloud backup.")
        firestore.collection("user_backups").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val settingsJson = document.getString("settingsJson").orEmpty()
                if (!document.exists() || settingsJson.isBlank()) {
                    onResult(null, "No cloud backup found.")
                } else {
                    onResult(
                        CloudBackupSnapshot(
                            settingsJson = settingsJson,
                            updatedAtMillis = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                            appVersion = document.getString("appVersion").orEmpty()
                        ),
                        null
                    )
                }
            }
            .addOnFailureListener { error -> onResult(null, error.localizedMessage ?: "Unable to restore cloud backup.") }
    }
}
