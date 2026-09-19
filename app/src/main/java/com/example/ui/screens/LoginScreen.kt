package com.example.ui.screens // Forced update

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
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeenViewModel
import com.example.ui.viewmodel.DeenScreen
import com.example.data.AuthEmailService
import kotlinx.coroutines.launch

import android.util.Patterns
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val spiritualSurface = Color(0xFFF4FAFD)
    val sacredGreen = Color(0xFF0F4D32)
    val sacredGreenDark = Color(0xFF0B3E28)
    val sacredGold = Color(0xFFFED65B)
    val sacredGoldDim = Color(0xFFE9C349)
    val sacredText = Color(0xFF161D1F)
    val sacredMuted = Color(0xFF707972)
    val sacredLine = Color(0xFFC0C9C0)

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
        val clientId = com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(spiritualSurface)
    ) {
        AuthStarPattern(
            tint = sacredGreen.copy(alpha = 0.035f),
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(42.dp))

            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Noor Pro",
                tint = sacredGold,
                modifier = Modifier
                    .size(76.dp)
                    .padding(bottom = 14.dp)
            )

            Text(
                text = if (isRegistering) "Create Your Account" else "Begin Your Journey",
                color = sacredGoldDim,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (isRegistering) "Sign up to save your progress and join the ummah" else "Sign in to continue your journey",
                color = sacredMuted,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 34.dp)
            )

            if (isRegistering) {
                SpiritualAuthField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Full Name",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    sacredGreen = sacredGreen,
                    sacredGold = sacredGold,
                    sacredMuted = sacredMuted,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            SpiritualAuthField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                placeholder = "Email Address",
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                sacredGreen = sacredGreen,
                sacredGold = sacredGold,
                sacredMuted = sacredMuted,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            SpiritualAuthField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                placeholder = "Password",
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                sacredGreen = sacredGreen,
                sacredGold = sacredGold,
                sacredMuted = sacredMuted,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFBA1A1A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    googleErrorMessage = null
                    val normalizedEmail = email.trim()
                    if (normalizedEmail.isBlank() || password.isBlank() || (isRegistering && name.isBlank())) {
                        errorMessage = "Please fill in all fields."
                        return@Button
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                        errorMessage = "Enter a valid email address."
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must contain at least 6 characters."
                        return@Button
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
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = !isLoading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFDE72), sacredGoldDim)
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color(0xFF574500), modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            text = if (isRegistering) "Create Account" else "Sign In",
                            color = Color(0xFF574500),
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isRegistering) "Already have an account? " else "Don't have an account? ",
                    color = sacredMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isRegistering) "Sign In" else "Sign Up",
                    color = sacredGoldDim,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        isRegistering = !isRegistering
                        errorMessage = null
                        googleErrorMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Forgot password?",
                color = sacredText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    if (isRegistering) {
                        isRegistering = false
                        return@clickable
                    }
                    val normalizedEmail = email.trim()
                    if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                        errorMessage = "Enter your email address first."
                        return@clickable
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
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = sacredLine.copy(alpha = 0.55f))
                Text(
                    text = "OR CONTINUE WITH",
                    color = sacredMuted.copy(alpha = 0.74f),
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = sacredLine.copy(alpha = 0.55f))
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Use your Google account without entering a password above.",
                color = sacredMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    launchGoogleSignIn()
                },
                colors = ButtonDefaults.buttonColors(containerColor = sacredGreen, contentColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = !isLoading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = sacredGold,
                        modifier = Modifier.padding(end = 14.dp)
                    )
                    Text(
                        "Continue with Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            googleErrorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFBA1A1A),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "Explore as a Guest",
                color = sacredGoldDim,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    viewModel.handleLogout()
                    viewModel.navigateTo(DeenScreen.DASHBOARD)
                }
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
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

@Composable
private fun AuthStarPattern(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val positions = listOf(
            0.14f to 0.10f,
            0.50f to 0.12f,
            0.86f to 0.10f,
            0.25f to 0.32f,
            0.72f to 0.34f,
            0.12f to 0.58f,
            0.52f to 0.62f,
            0.88f to 0.58f,
            0.30f to 0.84f,
            0.76f to 0.86f
        )
        positions.forEach { (x, y) ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = (360 * x).dp, top = (760 * y).dp)
                    .size(54.dp)
            )
        }
    }
}

@Composable
private fun SpiritualAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    sacredGreen: Color,
    sacredGold: Color,
    sacredMuted: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                color = sacredMuted.copy(alpha = 0.9f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            CompositionLocalProvider(LocalContentColor provides sacredGold) {
                leadingIcon()
            }
        },
        trailingIcon = trailingIcon?.let {
            {
                CompositionLocalProvider(LocalContentColor provides sacredMuted.copy(alpha = 0.92f)) {
                    it()
                }
            }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = sacredGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = sacredGold,
            focusedContainerColor = sacredGreen,
            unfocusedContainerColor = sacredGreen,
            focusedPlaceholderColor = sacredMuted,
            unfocusedPlaceholderColor = sacredMuted,
            focusedLeadingIconColor = sacredGold,
            unfocusedLeadingIconColor = sacredGold,
            focusedTrailingIconColor = sacredMuted,
            unfocusedTrailingIconColor = sacredMuted
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    )
}
