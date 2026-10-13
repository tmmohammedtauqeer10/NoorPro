package com.noorpro.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.noorpro.app.data.AccountDeletionService
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val DELETE_ACCOUNT_URL = "https://noor-pro-d87e3.web.app/delete-account"

/**
 * In-app account deletion (Google Play "account deletion" requirement).
 * Re-authenticates first (so Firebase never throws requires-recent-login half way), erases the
 * account's data, deletes the Firebase Auth user, then signs out locally and returns to Login.
 */
@Composable
fun DeleteAccountDialog(viewModel: DeenViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val needsPassword = remember { AccountDeletionService.usesPassword() }
    var password by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun reauthenticate(): Boolean {
        val current = FirebaseAuth.getInstance().currentUser ?: return false
        return if (needsPassword) {
            val email = current.email.orEmpty()
            if (email.isBlank() || password.isBlank()) {
                error = "Enter your password to confirm."
                false
            } else {
                runCatching { current.reauthenticate(EmailAuthProvider.getCredential(email, password)).await() }
                    .onFailure { error = "That password is not correct." }
                    .isSuccess
            }
        } else {
            val clientId = com.noorpro.app.BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (clientId.isBlank() || clientId.startsWith("YOUR_")) {
                error = "Google re-sign-in is not configured for this build. Use the web page or email us."
                return false
            }
            runCatching {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                    .build()
                val result = CredentialManager.create(context).getCredential(context, request)
                val credential = result.credential
                require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                current.reauthenticate(GoogleAuthProvider.getCredential(google.idToken, null)).await()
            }.onFailure { error = "We could not confirm your Google account. Please try again." }.isSuccess
        }
    }

    fun startDeletion() {
        error = null
        busy = true
        scope.launch {
            val uid = user?.uid.orEmpty()
            try {
                if (!reauthenticate()) {
                    busy = false
                    return@launch
                }
                AccountDeletionService.purgeUserData(uid) { progress = it }
                progress = "Deleting your account"
                FirebaseAuth.getInstance().currentUser?.delete()?.await()
                val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                prefs.edit().remove("ummah_username_$uid").remove("user_bio_$uid").apply()
                viewModel.removeSavedAccountFromDevice(uid)
                viewModel.handleLogout(false)
                Toast.makeText(context, "Your account and data have been deleted.", Toast.LENGTH_LONG).show()
                onDismiss()
                viewModel.navigateTo(DeenScreen.LOGIN)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Unable to delete the account. Please try again or email ${UgcTerms.MODERATION_EMAIL}."
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Delete account") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This permanently deletes your Noor Pro account and the data linked to it: profile, posts, comments, " +
                        "messages you sent, likes, saves, follows, backups and uploaded media. It cannot be undone. " +
                        "A few items (for example reels stored on our CDN and one-to-one chat records) are purged by our team " +
                        "within 30 days of your request.",
                    fontSize = 14.sp
                )
                Text(
                    "More details: $DELETE_ACCOUNT_URL",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().clickable { UgcTerms.openUrl(context, DELETE_ACCOUNT_URL) }
                )
                if (needsPassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("You will be asked to confirm with your Google account.", fontSize = 13.sp)
                }
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it; error = null },
                    label = { Text("Type DELETE to confirm") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy && progress.isNotBlank()) Text("$progress...", fontSize = 13.sp)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && confirmText.trim().equals("DELETE", ignoreCase = true) && user != null,
                onClick = { startDeletion() }
            ) { Text("Delete my account", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}
