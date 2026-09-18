package com.noorpro.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.firebase.auth.FirebaseUser
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedAuthAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
    val providerType: String,
    val lastUsedAt: Long
)

/**
 * Secure on-device account metadata cache for Instagram-style account switching.
 *
 * Stores only account identity metadata, encrypted with an Android Keystore AES-GCM key.
 * Passwords, refresh tokens, ID tokens and credentials are never stored here.
 */
class SecureAccountStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("secure_account_switcher", Context.MODE_PRIVATE)

    fun loadAccounts(): List<SavedAuthAccount> {
        val encrypted = prefs.getString(KEY_ACCOUNTS, null).orEmpty()
        if (encrypted.isBlank()) return emptyList()
        return runCatching {
            val json = decrypt(encrypted)
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val uid = item.optString("uid")
                    val email = item.optString("email")
                    if (uid.isBlank() && email.isBlank()) continue
                    add(
                        SavedAuthAccount(
                            uid = uid,
                            email = email,
                            displayName = item.optString("displayName"),
                            photoUrl = item.optString("photoUrl"),
                            providerType = item.optString("providerType", "unknown"),
                            lastUsedAt = item.optLong("lastUsedAt", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.lastUsedAt }
        }.getOrDefault(emptyList())
    }

    fun saveAccount(account: SavedAuthAccount): List<SavedAuthAccount> {
        if (account.uid.isBlank() && account.email.isBlank()) return loadAccounts()
        val updated = (listOf(account.copy(lastUsedAt = System.currentTimeMillis())) + loadAccounts())
            .distinctBy { it.uid.ifBlank { it.email.lowercase() } }
            .sortedByDescending { it.lastUsedAt }
        persist(updated)
        return updated
    }

    fun saveFirebaseUser(user: FirebaseUser, fallbackProvider: String = ""): List<SavedAuthAccount> {
        val provider = user.providerData
            .firstOrNull { it.providerId != "firebase" }
            ?.providerId
            .orEmpty()
            .ifBlank { fallbackProvider.ifBlank { "unknown" } }
        return saveAccount(
            SavedAuthAccount(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName.orEmpty().ifBlank { user.email?.substringBefore("@").orEmpty() },
                photoUrl = user.photoUrl?.toString().orEmpty(),
                providerType = provider,
                lastUsedAt = System.currentTimeMillis()
            )
        )
    }

    fun removeAccount(uidOrEmail: String): List<SavedAuthAccount> {
        val key = uidOrEmail.trim()
        val updated = loadAccounts().filterNot { it.uid == key || it.email.equals(key, ignoreCase = true) }
        persist(updated)
        return updated
    }

    private fun persist(accounts: List<SavedAuthAccount>) {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("uid", account.uid)
                    .put("email", account.email)
                    .put("displayName", account.displayName)
                    .put("photoUrl", account.photoUrl)
                    .put("providerType", account.providerType)
                    .put("lastUsedAt", account.lastUsedAt)
            )
        }
        prefs.edit().putString(KEY_ACCOUNTS, encrypt(array.toString())).apply()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipherText, Base64.NO_WRAP))
            .toString()
    }

    private fun decrypt(payload: String): String {
        val json = JSONObject(payload)
        val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
        val cipherText = Base64.decode(json.getString("data"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "noor_pro_account_switcher_key"
        const val KEY_ACCOUNTS = "accounts_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
