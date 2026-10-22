package com.noorpro.app.ui.screens // Forced update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.theme.*
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.data.AuthEmailService
import kotlinx.coroutines.launch

import android.util.Patterns
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.style.TextDecoration
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: DeenViewModel) {
    var isRegistering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var googleErrorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    fun finishGoogleSignIn(idToken: String?, fallbackEmail: String?, fallbackName: String?) {
        if (idToken.isNullOrBlank()) {
            googleErrorMessage = "Google did not complete sign-in. Please try again."
            isLoading = false
            return
        }

        val auth = FirebaseAuth.getInstance()
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signedUser = auth.currentUser
                    val resolvedEmail = signedUser?.email ?: fallbackEmail.orEmpty()
                    val resolvedName = signedUser?.displayName
                        ?: fallbackName
                        ?: resolvedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                    viewModel.handleFirebaseUser(signedUser, resolvedEmail, resolvedName)
                    viewModel.navigateTo(DeenScreen.DASHBOARD)
                } else {
                    googleErrorMessage = friendlyGoogleAuthError(task.exception)
                }
                isLoading = false
            }
    }

    fun launchGoogleSignIn() {
        val clientId = com.noorpro.app.BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank() || clientId.startsWith("YOUR_")) {
            googleErrorMessage = "Google Sign-In is not configured for this build."
            return
        }
        isLoading = true
        errorMessage = null
        googleErrorMessage = null
        coroutineScope.launch {
            try {
                // Explicit button flow supports account selection, adding an account,
                // and reauthentication when the bottom-sheet flow has no credentials.
                val googleIdOption = GetSignInWithGoogleOption.Builder(clientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val resolvedEmail = googleCredential.id
                    val resolvedName = googleCredential.displayName
                        ?: resolvedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                    finishGoogleSignIn(googleCredential.idToken, resolvedEmail, resolvedName)
                } else {
                    googleErrorMessage = "Choose a Google account to continue."
                    isLoading = false
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                isLoading = false
                throw e
            } catch (e: Exception) {
                googleErrorMessage = friendlyGoogleAuthError(e)
                isLoading = false
            }
        }
    }

    fun submitEmail() {
                    googleErrorMessage = null
                    val normalizedEmail = email.trim()
                    if (normalizedEmail.isBlank() || password.isBlank() || (isRegistering && name.isBlank())) {
                        errorMessage = "Please fill in all fields."
                        return
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                        errorMessage = "Enter a valid email address."
                        return
                    }
                    if (isRegistering && !acceptedTerms) {
                        errorMessage = "Please accept the Terms of Service to create an account."
                        return
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must contain at least 6 characters."
                        return
                    }
                    isLoading = true
                    errorMessage = null
                    try {
                        val auth = FirebaseAuth.getInstance()
                        val task = if (isRegistering) {
                            auth.createUserWithEmailAndPassword(normalizedEmail, password)
                        } else {
                            auth.signInWithEmailAndPassword(normalizedEmail, password)
                        }
                        task.addOnCompleteListener { result ->
                            if (result.isSuccessful) {
                                val user = auth.currentUser
                                val displayName = if (isRegistering) name.trim() else {
                                    user?.displayName ?: normalizedEmail.substringBefore("@")
                                }
                                if (isRegistering && user != null) {
                                    val profile = UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build()
                                    user.updateProfile(profile)
                                }
                                viewModel.handleFirebaseUser(user, normalizedEmail, displayName)
                                viewModel.navigateTo(DeenScreen.DASHBOARD)
                            } else {
                                errorMessage = friendlyEmailAuthError(result.exception, isRegistering)
                            }
                            if (result.isSuccessful) password = ""
                            isLoading = false
                        }
                    } catch (_: Exception) {
                        isLoading = false
                        errorMessage = "Authentication is not configured for this build. Continue as a guest."
                    }
    }

    fun forgotPassword() {
                    if (isRegistering) {
                        isRegistering = false
                        return
                    }
                    val normalizedEmail = email.trim()
                    if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                        errorMessage = "Enter your email address first."
                        return
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        AuthEmailService.sendPasswordReset(normalizedEmail)
                            .onSuccess {
                                errorMessage = "Password reset email sent. Check your inbox."
                            }
                            .onFailure {
                                errorMessage = it.localizedMessage ?: "Unable to send reset email."
                            }
                        isLoading = false
                    }
    }

    var showPhone by remember { mutableStateOf(false) }
    if (showPhone) {
        androidx.activity.compose.BackHandler { showPhone = false }
        PhoneAuthFlow(linkMode = false, onBack = { showPhone = false }) { user ->
            showPhone = false
            viewModel.handleFirebaseUser(user, "", user.displayName.orEmpty().ifBlank { "Member " + user.phoneNumber.orEmpty().takeLast(4) })
            viewModel.navigateTo(DeenScreen.DASHBOARD)
        }
        return
    }
    AuthContent(
        onPhone = { showPhone = true },
        isRegistering = isRegistering, name = name, email = email, password = password,
        passwordVisible = passwordVisible, acceptedTerms = acceptedTerms, isLoading = isLoading,
        errorMessage = errorMessage, googleErrorMessage = googleErrorMessage,
        onName = { name = it }, onEmail = { email = it; errorMessage = null }, onPassword = { password = it; errorMessage = null },
        onTogglePassword = { passwordVisible = !passwordVisible }, onAcceptTerms = { acceptedTerms = it; errorMessage = null },
        onSubmit = { submitEmail() }, onForgot = { forgotPassword() }, onGoogle = { launchGoogleSignIn() },
        onToggleMode = { isRegistering = !isRegistering; errorMessage = null; googleErrorMessage = null },
        onGuest = { viewModel.handleLogout(); viewModel.navigateTo(DeenScreen.DASHBOARD) },
        onTerms = { UgcTerms.openUrl(context, UgcTerms.TERMS_URL) }, onPrivacy = { UgcTerms.openUrl(context, UgcTerms.PRIVACY_URL) },
    )
}

private fun friendlyEmailAuthError(exception: Exception?, registering: Boolean): String {
    val message = exception?.localizedMessage.orEmpty()
    return when {
        exception is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
            "This email is already registered. Use the password you created, or Continue with Google if you first signed up with Google."
        exception is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
            "Password is too weak. Use at least 6 characters."
        exception is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
            "No account found for this email. Tap Sign Up to create one."
        exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException && registering ->
            "Use a valid email address and a password with at least 6 characters."
        exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
            "Email or app password is incorrect. If you use a Google account, tap Continue with Google. Otherwise, use Forgot password or Sign Up."
        message.contains("provider", ignoreCase = true) || message.contains("disabled", ignoreCase = true) ->
            "Email/password sign-in is not enabled in Firebase Authentication."
        message.contains("network", ignoreCase = true) ->
            "Network error. Check your internet connection and try again."
        else -> message.ifBlank { "Authentication failed. Try again or continue with Google." }
    }
}

internal fun friendlyGoogleAuthError(exception: Exception?): String {
    val message = exception?.localizedMessage.orEmpty()
    return when {
        exception is androidx.credentials.exceptions.GetCredentialCancellationException ->
            "Google sign-in did not finish. If you selected an account, try again. If it keeps happening, contact support."
        exception is androidx.credentials.exceptions.NoCredentialException ->
            "Google sign-in is unavailable right now. Try again, check your connection, and make sure Google Play services is up to date."
        message.contains("app check", ignoreCase = true) ->
            "Firebase App Check rejected this tester build. Install from Play testing, then check Play Integrity/App Check setup in Firebase."
        message.contains("network", ignoreCase = true) ->
            "Network error. Check your internet connection and try again."
        message.contains("configuration", ignoreCase = true) ||
            message.contains("DEVELOPER_ERROR", ignoreCase = true) ->
            "Google sign-in is not configured correctly for this app version. Please contact support."
        else -> message.ifBlank { "Google Sign-In failed. Check Firebase SHA fingerprints and Google provider settings." }
    }
}



/** Stateless sign-in / sign-up layout (auth logic lives in [LoginScreen]). */
@Composable
fun AuthContent(
    isRegistering: Boolean, name: String, email: String, password: String,
    passwordVisible: Boolean, acceptedTerms: Boolean, isLoading: Boolean,
    errorMessage: String?, googleErrorMessage: String?,
    onName: (String) -> Unit, onEmail: (String) -> Unit, onPassword: (String) -> Unit,
    onTogglePassword: () -> Unit, onAcceptTerms: (Boolean) -> Unit,
    onSubmit: () -> Unit, onForgot: () -> Unit, onGoogle: () -> Unit,
    onToggleMode: () -> Unit, onGuest: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit,
    onPhone: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surface, unfocusedContainerColor = scheme.surface,
        focusedBorderColor = scheme.primary, unfocusedBorderColor = scheme.outlineVariant,
        focusedLeadingIconColor = scheme.primary, unfocusedLeadingIconColor = scheme.onSurfaceVariant,
    )
    Box(Modifier.fillMaxSize().background(scheme.background)) {
        IslamicPattern(scheme.primary.copy(alpha = 0.04f), Modifier.fillMaxWidth().height(320.dp))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(36.dp))
                NoorBrandMark(76.dp)
                Spacer(Modifier.height(20.dp))
                androidx.compose.animation.AnimatedContent(isRegistering, label = "title") { reg ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (reg) "Create your account" else "Welcome back", color = scheme.onBackground,
                            fontSize = 30.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (reg) "Save your progress and join the ummah." else "Sign in to sync your progress, bookmarks and reels.",
                            color = scheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onGoogle, enabled = !isLoading, shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Continue with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                googleErrorMessage?.let { InlineMessage(it, isError = true) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPhone, enabled = !isLoading, shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Phone, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Continue with phone", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
                    Text("or use email", color = scheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
                }

                androidx.compose.animation.AnimatedVisibility(isRegistering) {
                    OutlinedTextField(
                        value = name, onValueChange = onName, label = { Text("Full name") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null) }, shape = RoundedCornerShape(16.dp), colors = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
                OutlinedTextField(
                    value = email, onValueChange = onEmail, label = { Text("Email") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null) }, shape = RoundedCornerShape(16.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = password, onValueChange = onPassword, label = { Text("Password") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = onTogglePassword) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus(); onSubmit() }),
                    supportingText = if (isRegistering) { { Text("At least 6 characters") } } else null,
                    shape = RoundedCornerShape(16.dp), colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isRegistering) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = onForgot, enabled = !isLoading) { Text("Forgot password?", fontWeight = FontWeight.SemiBold) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Checkbox(checked = acceptedTerms, onCheckedChange = onAcceptTerms)
                        Text(
                            "I agree to the Terms of Service and community rules (no objectionable content; reports reviewed within 24 hours).",
                            color = scheme.onSurface, fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
                errorMessage?.let { InlineMessage(it, isError = !it.startsWith("Password reset email sent")) }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = { focus.clearFocus(); onSubmit() }, enabled = !isLoading, shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    else Text(if (isRegistering) "Create account" else "Sign in", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isRegistering) "Already have an account?" else "New to Noor Pro?", color = scheme.onSurfaceVariant, fontSize = 14.sp)
                    TextButton(onClick = onToggleMode) { Text(if (isRegistering) "Sign in" else "Create account", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGuest, enabled = !isLoading, shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outline),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Explore, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue as guest", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("By continuing you agree to our ", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("Terms", color = scheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onTerms))
                    Text(" and ", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("Privacy Policy", color = scheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onPrivacy))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InlineMessage(text: String, isError: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp))
            .background(if (isError) scheme.errorContainer else scheme.primary.copy(alpha = 0.12f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null,
            tint = if (isError) scheme.onErrorContainer else scheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (isError) scheme.onErrorContainer else scheme.onSurface, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
