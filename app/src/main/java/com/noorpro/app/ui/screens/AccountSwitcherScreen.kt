package com.noorpro.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import coil.compose.AsyncImage
import com.noorpro.app.data.SavedAuthAccount
import com.noorpro.app.ui.components.StitchCream
import com.noorpro.app.ui.components.StitchLine
import com.noorpro.app.ui.components.StitchScreen
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchPrimary
import com.noorpro.app.ui.components.stitchSoftSurface
import com.noorpro.app.ui.components.stitchSurface
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AccountSwitcherScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUid = auth.currentUser?.uid.orEmpty()
    val accounts = viewModel.savedAuthAccounts
    var switchingUid by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var removeTarget by remember { mutableStateOf<SavedAuthAccount?>(null) }

    LaunchedEffect(Unit) {
        viewModel.rememberCurrentFirebaseAccount()
        viewModel.reloadSavedAccounts()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.reloadSavedAccounts() }
    }

    fun addAnotherAccount() {
        viewModel.rememberCurrentFirebaseAccount()
        viewModel.handleLogout(rememberAccount = false)
        viewModel.navigateTo(DeenScreen.LOGIN)
    }

    fun switchTo(account: SavedAuthAccount) {
        if (switchingUid != null) return
        if (account.uid.isNotBlank() && account.uid == auth.currentUser?.uid) {
            viewModel.handleFirebaseUser(auth.currentUser, account.email, account.displayName)
            viewModel.navigateTo(DeenScreen.DASHBOARD)
            return
        }
        val provider = account.providerType.lowercase()
        if (provider != "google.com") {
            viewModel.rememberCurrentFirebaseAccount()
            viewModel.handleLogout(rememberAccount = false)
            Toast.makeText(context, "For this account, sign in again. Noor Pro does not store passwords.", Toast.LENGTH_LONG).show()
            viewModel.navigateTo(DeenScreen.LOGIN)
            return
        }

        scope.launch {
            switchingUid = account.uid
            error = null
            viewModel.rememberCurrentFirebaseAccount()
            viewModel.handleLogout(rememberAccount = false)
            try {
                val clientId = com.noorpro.app.BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (clientId.isBlank() || clientId.startsWith("YOUR_")) {
                    error = "Google Sign-In is not configured for this build."
                    switchingUid = null
                    return@launch
                }
                val googleIdOption = GetSignInWithGoogleOption.Builder(clientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential !is androidx.credentials.CustomCredential ||
                    credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    error = "Choose the saved Google account to continue."
                    switchingUid = null
                    return@launch
                }
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                    val signedUser = auth.currentUser
                    if (task.isSuccessful && signedUser != null && signedUser.uid == account.uid) {
                        viewModel.handleFirebaseUser(signedUser, account.email, account.displayName)
                        viewModel.navigateTo(DeenScreen.DASHBOARD)
                    } else {
                        auth.signOut()
                        viewModel.handleLogout(rememberAccount = false)
                        error = if (task.isSuccessful) {
                            "Wrong Google account selected. Please choose ${account.email}."
                        } else {
                            friendlyGoogleAuthError(task.exception)
                        }
                    }
                    switchingUid = null
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                switchingUid = null
                throw e
            } catch (e: Exception) {
                auth.signOut()
                viewModel.handleLogout(rememberAccount = false)
                error = friendlyGoogleAuthError(e)
                switchingUid = null
            }
        }
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(stitchSurface())
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, top = 18.dp, end = 22.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Text(
                        "Switch Account",
                        color = stitchText(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(stitchSoftSurface())
                        .border(1.dp, StitchLine.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(stitchPrimary().copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = stitchPrimary())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Accounts stay separate", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Noor Pro switches Firebase users by uid. Passwords are never saved on this device.",
                                color = stitchMutedText(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        error.orEmpty(),
                        color = Color(0xFFBA1A1A),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFDAD6))
                            .padding(14.dp)
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("No saved accounts yet", color = stitchText(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Sign in once, then Noor Pro will remember this account securely for quick switching.", color = stitchMutedText(), fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            } else {
                items(accounts, key = { it.uid.ifBlank { it.email } }) { account ->
                    AccountSwitcherRow(
                        account = account,
                        isCurrent = account.uid.isNotBlank() && account.uid == currentUid,
                        switching = switchingUid == account.uid,
                        onSelect = { switchTo(account) },
                        onRemove = { removeTarget = account }
                    )
                }
            }

            item {
                Button(
                    onClick = ::addAnotherAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = stitchPrimary(), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add another account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    removeTarget?.let { account ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            containerColor = stitchSurface(),
            title = { Text("Remove from this device?", color = stitchText(), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This only removes ${account.email.ifBlank { account.displayName }} from the local switcher. It does not delete the Firebase account or Firestore data.",
                    color = stitchMutedText(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeSavedAccountFromDevice(account.uid.ifBlank { account.email })
                        if (account.uid.isNotBlank() && account.uid == auth.currentUser?.uid) {
                            auth.signOut()
                            viewModel.handleLogout(rememberAccount = false)
                        }
                        removeTarget = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) {
                    Text("Cancel", color = stitchMutedText())
                }
            }
        )
    }
}

@Composable
private fun AccountSwitcherRow(
    account: SavedAuthAccount,
    isCurrent: Boolean,
    switching: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(
                1.dp,
                if (isCurrent) stitchPrimary().copy(alpha = 0.45f) else StitchLine.copy(alpha = 0.65f),
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(StitchCream),
            contentAlignment = Alignment.Center
        ) {
            if (account.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = account.photoUrl,
                    contentDescription = "${account.displayName} photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    account.displayName.ifBlank { account.email }.take(1).uppercase().ifBlank { "N" },
                    color = stitchPrimary(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    account.displayName.ifBlank { account.email.substringBefore("@").ifBlank { "Noor account" } },
                    color = stitchText(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrent) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Active",
                        color = stitchPrimary(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(stitchPrimary().copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(account.email, color = stitchMutedText(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(account.providerType.ifBlank { "unknown" }, color = stitchMutedText(), fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (switching) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = stitchPrimary())
        } else {
            Column(horizontalAlignment = Alignment.End) {
                OutlinedButton(
                    onClick = onSelect,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (isCurrent) "Open" else "Switch", color = stitchPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onRemove, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Remove", color = Color(0xFFBA1A1A), fontSize = 11.sp)
                }
            }
        }
    }
}
