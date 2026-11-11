package com.noorpro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuthException
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/** 1.0.78: Firebase Phone Auth - sign in with a mobile number, or link one to the current account
 *  (so friends can reach you by phone and calls are tied to a verified number). */
data class DialCountry(val name: String, val iso: String, val dial: String)

val DIAL_COUNTRIES = listOf(
    DialCountry("India", "IN", "+91"), DialCountry("Pakistan", "PK", "+92"), DialCountry("Bangladesh", "BD", "+880"),
    DialCountry("Saudi Arabia", "SA", "+966"), DialCountry("United Arab Emirates", "AE", "+971"), DialCountry("Qatar", "QA", "+974"),
    DialCountry("Kuwait", "KW", "+965"), DialCountry("Oman", "OM", "+968"), DialCountry("Bahrain", "BH", "+973"),
    DialCountry("Egypt", "EG", "+20"), DialCountry("Turkey", "TR", "+90"), DialCountry("Indonesia", "ID", "+62"),
    DialCountry("Malaysia", "MY", "+60"), DialCountry("Singapore", "SG", "+65"), DialCountry("Sri Lanka", "LK", "+94"),
    DialCountry("Nepal", "NP", "+977"), DialCountry("Maldives", "MV", "+960"), DialCountry("Afghanistan", "AF", "+93"),
    DialCountry("Iran", "IR", "+98"), DialCountry("Iraq", "IQ", "+964"), DialCountry("Jordan", "JO", "+962"),
    DialCountry("Morocco", "MA", "+212"), DialCountry("Algeria", "DZ", "+213"), DialCountry("Tunisia", "TN", "+216"),
    DialCountry("Nigeria", "NG", "+234"), DialCountry("Kenya", "KE", "+254"), DialCountry("South Africa", "ZA", "+27"),
    DialCountry("United Kingdom", "GB", "+44"), DialCountry("United States", "US", "+1"), DialCountry("Canada", "CA", "+1"),
    DialCountry("Australia", "AU", "+61"), DialCountry("Germany", "DE", "+49"), DialCountry("France", "FR", "+33"),
    DialCountry("Netherlands", "NL", "+31"), DialCountry("Sweden", "SE", "+46"), DialCountry("Uzbekistan", "UZ", "+998"),
    DialCountry("Kazakhstan", "KZ", "+7"), DialCountry("Azerbaijan", "AZ", "+994"), DialCountry("Somalia", "SO", "+252"),
    DialCountry("Sudan", "SD", "+249")
)

fun flagEmoji(iso: String): String = iso.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

/** Normalises what people actually type ("098765 43210", "+91 98765…", "919876543210") to E.164. */
internal fun normalizePhone(country: DialCountry, raw: String): String? {
    var d = raw.filter { it.isDigit() }
    val cc = country.dial.removePrefix("+")
    if (raw.trim().startsWith("+")) return if (d.length in 8..15) "+$d" else null
    if (d.startsWith("00")) d = d.drop(2)
    d = d.trimStart('0')
    if (country.iso == "IN") {
        if (d.length == 12 && d.startsWith("91")) d = d.drop(2)
        return if (d.length == 10 && d[0] in '6'..'9') "+91$d" else null
    }
    if (d.startsWith(cc) && d.length > cc.length + 6) d = d.drop(cc.length)
    return if (d.length in 6..13) "+$cc$d" else null
}

fun friendlyPhoneError(e: Exception?): String {
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    val m = e?.localizedMessage.orEmpty()
    return when {
        e is FirebaseTooManyRequestsException || code == "ERROR_TOO_MANY_REQUESTS" ->
            "Too many attempts from this device. Please wait a few minutes, or continue with Google or email."
        code == "ERROR_QUOTA_EXCEEDED" || m.contains("quota", true) || m.contains("BILLING", true) ->
            "SMS limit reached for now. Please try again later, or continue with Google or email."
        e is FirebaseAuthUserCollisionException || code == "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
            "This number is already linked to another Noor account. Sign in with that number instead."
        code == "ERROR_INVALID_VERIFICATION_CODE" -> "That code is incorrect. Check the SMS and try again."
        code == "ERROR_SESSION_EXPIRED" || code == "ERROR_CODE_EXPIRED" -> "This code has expired. Tap Resend to get a new one."
        code == "ERROR_INVALID_PHONE_NUMBER" || (e is FirebaseAuthInvalidCredentialsException && m.contains("phone", true)) ->
            "That doesn't look like a valid mobile number. Check the country code and number."
        code == "ERROR_MISSING_CLIENT_IDENTIFIER" || code == "ERROR_APP_NOT_AUTHORIZED" || code == "ERROR_INVALID_APP_CREDENTIAL" ||
            (m.contains("app", true) && m.contains("not authorized", true)) ->
            "Couldn't verify this copy of the app with Google Play. Update Noor Pro from the Play Store, or continue with Google or email."
        m.contains("region", true) -> "SMS to this country isn't available yet. Continue with Google or email."
        isTransientPhoneNetworkError(e) ->
            "Couldn't reach the SMS verification service. Check your internet, turn off VPN or Private DNS, then tap Resend code."
        e is FirebaseAuthInvalidCredentialsException -> "Enter a valid mobile number."
        else -> "Unable to send or verify the code right now. Please try again, or continue with Google or email."
    }
}

internal fun isTransientPhoneNetworkError(e: Exception?): Boolean {
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    val message = e?.localizedMessage.orEmpty()
    return code == "ERROR_NETWORK_REQUEST_FAILED" ||
        e is com.google.firebase.FirebaseNetworkException ||
        listOf(
            "software caused connection abort",
            "connection reset",
            "failed to connect",
            "socket",
            "network",
            "timed out",
            "timeout",
            "unable to resolve host"
        ).any { message.contains(it, ignoreCase = true) }
}

/** What the new-account step collected (name, @username, optional photo). */
data class PhoneSignup(val name: String, val username: String, val photo: android.net.Uri?)

private val USERNAME_RE = Regex("^[a-z0-9._]{3,24}$")

/** Live @username check against public profile cards (handle is the public @username). */
private suspend fun usernameTaken(handle: String, myUid: String?): Boolean = runCatching {
    val docs = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ummah_profiles")
        .whereEqualTo("handle", handle).limit(3).get().await().documents
    docs.any { it.id != myUid }
}.getOrDefault(false)

private fun suggestUsername(name: String, phone: String): String {
    val base = name.lowercase().filter { it.isLetterOrDigit() || it == '.' || it == '_' }.take(16).ifBlank { "noor" }
    return (base + phone.filter { it.isDigit() }.takeLast(3)).take(24)
}

/**
 * Full-screen phone flow: number -> 6-box OTP (auto-read) -> profile (new accounts only).
 * [linkMode] = attach the number to the signed-in account instead of signing in.
 * [onDone] receives the user and, for brand-new accounts, the profile they set up.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PhoneAuthFlow(
    linkMode: Boolean,
    onBack: () -> Unit,
    previewStep: String? = null,
    previewCode: String = "",
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onSignedUp: (FirebaseUser, PhoneSignup?) -> Unit = { _, _ -> },
    onDone: (FirebaseUser) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { if (previewStep != null) null else runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    var country by remember { mutableStateOf(DIAL_COUNTRIES.first()) }
    var number by remember { mutableStateOf(if (previewStep != null) "98765 43210" else "") }
    var step by remember { mutableStateOf(previewStep ?: "phone") }
    var code by remember { mutableStateOf(previewCode) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resendIn by remember { mutableStateOf(0) }
    var pickCountry by remember { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(linkMode || previewStep != null) }
    var shake by remember { mutableStateOf(0) }
    var signedInUser by remember { mutableStateOf<FirebaseUser?>(null) }
    // profile step
    var pName by remember { mutableStateOf(if (previewStep == "profile") "Aisha Khan" else "") }
    var pUser by remember { mutableStateOf(if (previewStep == "profile") "aisha.khan" else "") }
    var pPhoto by remember { mutableStateOf<android.net.Uri?>(null) }
    var userState by remember { mutableStateOf(if (previewStep == "profile") "ok" else "") } // "", checking, ok, taken, invalid
    val e164 = normalizePhone(country, number)
    val shown = e164 ?: (country.dial + " " + number)

    fun complete(user: FirebaseUser, isNew: Boolean) {
        busy = false
        if (!linkMode && (isNew || user.displayName.isNullOrBlank())) {
            signedInUser = user
            pName = user.displayName.orEmpty()
            if (pUser.isBlank()) pUser = suggestUsername(pName, user.phoneNumber.orEmpty())
            step = "profile"
        } else onDone(user)
    }

    fun finish(credential: PhoneAuthCredential) {
        val auth = auth ?: return
        busy = true; error = null
        val current = auth.currentUser
        val task = if (linkMode && current != null) current.linkWithCredential(credential) else auth.signInWithCredential(credential)
        task.addOnCompleteListener { t ->
            val user = t.result?.user
            if (t.isSuccessful && user != null) complete(user, t.result?.additionalUserInfo?.isNewUser == true)
            else {
                busy = false; error = friendlyPhoneError(t.exception)
                if ((t.exception as? FirebaseAuthException)?.errorCode == "ERROR_INVALID_VERIFICATION_CODE") { shake++; code = "" }
            }
        }
    }

    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // Instant verification or SMS auto-retrieval (Play services SMS Retriever reads the OTP).
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                credential.smsCode?.let { code = it }
                finish(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                busy = false; error = friendlyPhoneError(e)
                if (isTransientPhoneNetworkError(e)) resendIn = 0
                android.util.Log.w("NoorPhoneAuth", "verification failed: ${(e as? FirebaseAuthException)?.errorCode} ${e.message}")
            }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id; resendToken = token; busy = false; step = "otp"; resendIn = 30
            }
            override fun onCodeAutoRetrievalTimeOut(id: String) { verificationId = id }
        }
    }

    fun send(resend: Boolean = false) {
        val auth = auth ?: return
        val activity = context.findActivity() ?: run {
            error = "Unable to open phone verification. Close this screen and try again."
            return
        }
        val target = e164 ?: run { error = "That doesn't look like a valid mobile number. Check the country code and number."; return }
        if (!accepted) { error = "Please accept the Terms and Privacy Policy to continue."; return }
        busy = true; error = null
        auth.setLanguageCode(java.util.Locale.getDefault().language)
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(target)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (resend) resendToken?.let { builder.setForceResendingToken(it) }
        // Play Integrity first; Firebase falls back to a reCAPTCHA page automatically if needed.
        try {
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Exception) {
            busy = false
            error = friendlyPhoneError(e)
            if (isTransientPhoneNetworkError(e)) resendIn = 0
            android.util.Log.w("NoorPhoneAuth", "verification start failed", e)
        }
    }

    if (previewStep == "otp" && resendIn == 0 && previewCode.length < 6) resendIn = 24
    LaunchedEffect(resendIn) { if (previewStep == null && resendIn > 0) { delay(1000); resendIn -= 1 } }
    LaunchedEffect(code) {
        if (code.length == 6 && step == "otp" && !busy) verificationId?.let { finish(PhoneAuthProvider.getCredential(it, code)) }
    }
    LaunchedEffect(pUser) {
        if (previewStep != null) return@LaunchedEffect
        val h = pUser.trim().removePrefix("@").lowercase()
        if (h.isEmpty()) { userState = ""; return@LaunchedEffect }
        if (!USERNAME_RE.matches(h)) { userState = "invalid"; return@LaunchedEffect }
        userState = "checking"; delay(450)
        userState = if (usernameTaken(h, signedInUser?.uid ?: auth?.currentUser?.uid)) "taken" else "ok"
    }
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) pPhoto = uri }

    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
        ) {
            if (step != "profile") IconButton(onClick = { if (step == "otp") { step = "phone"; code = ""; error = null } else onBack() }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            } else Spacer(Modifier.height(56.dp))
            StepDots(listOf("phone", "otp", "profile").indexOf(step).coerceAtLeast(0), if (linkMode) 2 else 3)
            Spacer(Modifier.height(18.dp))
            AnimatedContent(
                step, label = "phoneStep",
                transitionSpec = {
                    val fwd = listOf("phone", "otp", "profile").indexOf(targetState) > listOf("phone", "otp", "profile").indexOf(initialState)
                    (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { w -> if (fwd) w / 3 else -w / 3 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(300)) { w -> if (fwd) -w / 4 else w / 4 } + fadeOut(tween(220)))
                }
            ) { st ->
                Column {
                    when (st) {
                        "phone" -> PhoneStep(linkMode, country, number, busy, accepted, e164 != null,
                            onCountry = { pickCountry = true }, onNumber = { number = it; error = null }, onAccept = { accepted = it; error = null },
                            onTerms = onTerms, onPrivacy = onPrivacy, onSend = { send() })
                        "otp" -> OtpStep(shown, code, shake, busy, resendIn, linkMode,
                            onCode = { code = it; error = null }, onVerify = { verificationId?.let { finish(PhoneAuthProvider.getCredential(it, code)) } },
                            onResend = { send(resend = true) }, onEdit = { step = "phone"; code = ""; error = null })
                        else -> ProfileStep(pName, pUser, pPhoto, userState, busy,
                            onName = { pName = it.take(40); if (pUser.isBlank()) pUser = suggestUsername(it, signedInUser?.phoneNumber.orEmpty()) },
                            onUser = { pUser = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }.take(24) },
                            onPhoto = { photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onContinue = {
                                val u = signedInUser ?: auth?.currentUser
                                when {
                                    pName.isBlank() -> error = "Please enter your name."
                                    userState != "ok" -> error = if (userState == "taken") "That username is taken. Try another." else "Choose a username: 3–24 letters, numbers, . or _"
                                    u != null -> scope.launch {
                                        busy = true
                                        val h = pUser.trim().lowercase()
                                        if (usernameTaken(h, u.uid)) { busy = false; userState = "taken"; error = "That username was just taken. Try another."; return@launch }
                                        runCatching { u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(pName.trim()).build()).await() }
                                        busy = false
                                        onSignedUp(u, PhoneSignup(pName.trim(), h, pPhoto))
                                        onDone(auth?.currentUser ?: u)
                                    }
                                }
                            })
                    }
                }
            }
            AnimatedVisibility(error != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(scheme.errorContainer.copy(alpha = 0.6f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = scheme.error, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(error.orEmpty(), color = scheme.onErrorContainer, fontSize = 13.sp)
                }
            }
        }
    }
    if (pickCountry) CountryPicker(onPick = { country = it; pickCountry = false; error = null }, onDismiss = { pickCountry = false })
}

@Composable
private fun StepDots(index: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val w by animateDpAsState(if (i == index) 26.dp else 8.dp, spring(dampingRatio = 0.7f, stiffness = 400f), label = "dot")
            Box(Modifier.height(8.dp).width(w).clip(RoundedCornerShape(50))
                .background(if (i <= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

@Composable
private fun PrimaryCta(text: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.98f, label = "cta")
    Button(onClick = onClick, enabled = enabled && !busy, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
        AnimatedContent(busy, label = "ctaBusy") { b ->
            if (b) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
            else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PhoneStep(
    linkMode: Boolean, country: DialCountry, number: String, busy: Boolean, accepted: Boolean, valid: Boolean,
    onCountry: () -> Unit, onNumber: (String) -> Unit, onAccept: (Boolean) -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit, onSend: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text(if (linkMode) "Add your mobile number" else "What's your number?", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = scheme.onBackground)
    Spacer(Modifier.height(8.dp))
    Text("We'll send a 6-digit code by SMS to verify it's you. Carrier rates may apply.", color = scheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 21.sp)
    Spacer(Modifier.height(28.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.height(58.dp).clip(RoundedCornerShape(16.dp)).background(scheme.surfaceVariant.copy(alpha = 0.45f))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp)).clickable(onClick = onCountry).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(flagEmoji(country.iso), fontSize = 22.sp); Spacer(Modifier.width(6.dp))
            Text(country.dial, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose country (${country.name})")
        }
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = number, onValueChange = { v -> onNumber(v.filter { it.isDigit() || it == ' ' || it == '+' }.take(18)) },
            placeholder = { Text(if (country.iso == "IN") "98765 43210" else "Mobile number") }, singleLine = true,
            trailingIcon = { AnimatedVisibility(valid, enter = scaleIn(spring(0.5f)) + fadeIn(), exit = fadeOut()) { Icon(Icons.Default.CheckCircle, null, tint = scheme.primary) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
        )
    }
    if (!linkMode) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onAccept(!accepted) }) {
            Checkbox(accepted, onAccept)
            Text(androidx.compose.ui.text.buildAnnotatedString {
                append("I agree to the ")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = scheme.primary, fontWeight = FontWeight.SemiBold)); append("Terms"); pop()
                append(" and ")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = scheme.primary, fontWeight = FontWeight.SemiBold)); append("Privacy Policy"); pop()
                append(", including the community rules.")
            }, fontSize = 13.sp, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
        Row(Modifier.padding(start = 48.dp)) {
            Text("Terms", color = scheme.primary, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onTerms).padding(4.dp))
            Text("Privacy", color = scheme.primary, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onPrivacy).padding(4.dp))
        }
    }
    Spacer(Modifier.height(22.dp))
    PrimaryCta("Send code", valid && accepted, busy, onSend)
}

@Composable
private fun ColumnScope.OtpStep(
    shown: String, code: String, shake: Int, busy: Boolean, resendIn: Int, linkMode: Boolean,
    onCode: (String) -> Unit, onVerify: () -> Unit, onResend: () -> Unit, onEdit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text("Enter the code", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = scheme.onBackground)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Sent to ", color = scheme.onSurfaceVariant, fontSize = 15.sp)
        Text(shown, color = scheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("Edit", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onEdit).padding(4.dp))
    }
    Text("We'll fill it in automatically when the SMS arrives.", color = scheme.onSurfaceVariant, fontSize = 13.sp)
    Spacer(Modifier.height(28.dp))
    val offset = remember { Animatable(0f) }
    LaunchedEffect(shake) { if (shake > 0) for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) offset.animateTo(x, tween(45)) }
    Box(Modifier.fillMaxWidth().graphicsLayer { translationX = offset.value }, contentAlignment = Alignment.Center) { OtpBoxes(code, onCode) }
    Spacer(Modifier.height(26.dp))
    PrimaryCta(if (linkMode) "Verify & link" else "Verify", code.length == 6, busy, onVerify)
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onResend, enabled = resendIn == 0 && !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(if (resendIn > 0) "Resend code in 0:${"%02d".format(resendIn)}" else "Didn't get it? Resend code", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ColumnScope.ProfileStep(
    name: String, username: String, photo: android.net.Uri?, userState: String, busy: Boolean,
    onName: (String) -> Unit, onUser: (String) -> Unit, onPhoto: () -> Unit, onContinue: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text("Create your profile", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = scheme.onBackground)
    Spacer(Modifier.height(8.dp))
    Text("This is how the Ummah will see you. You can change it any time.", color = scheme.onSurfaceVariant, fontSize = 15.sp)
    Spacer(Modifier.height(24.dp))
    Box(Modifier.align(Alignment.CenterHorizontally)) {
        Box(Modifier.size(108.dp).clip(CircleShape).background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))).clickable(onClick = onPhoto),
            contentAlignment = Alignment.Center) {
            if (photo != null) coil.compose.AsyncImage(photo, "Profile photo", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(name.trim().firstOrNull()?.uppercase() ?: "🙂", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(scheme.primary).border(3.dp, scheme.background, CircleShape).clickable(onClick = onPhoto),
            contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, "Add photo", tint = scheme.onPrimary, modifier = Modifier.size(16.dp)) }
    }
    Text("Add a photo (optional)", color = scheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(name, onName, singleLine = true, label = { Text("Your name") }, leadingIcon = { Icon(Icons.Default.Person, null) },
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(username, onUser, singleLine = true, label = { Text("Username") }, prefix = { Text("@", fontWeight = FontWeight.Bold) },
        isError = userState == "taken" || userState == "invalid",
        trailingIcon = {
            AnimatedContent(userState, label = "uState") { s ->
                when (s) {
                    "checking" -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    "ok" -> Icon(Icons.Default.CheckCircle, "Available", tint = Color(0xFF1E9E63))
                    "taken", "invalid" -> Icon(Icons.Default.ErrorOutline, "Unavailable", tint = scheme.error)
                    else -> Spacer(Modifier.size(18.dp))
                }
            }
        },
        supportingText = {
            Text(when (userState) {
                "ok" -> "@$username is available"
                "taken" -> "@$username is already taken"
                "invalid" -> "3–24 characters: letters, numbers, . and _"
                "checking" -> "Checking…"
                else -> "Pick a unique @username"
            }, color = when (userState) { "ok" -> Color(0xFF1E9E63); "taken", "invalid" -> scheme.error; else -> scheme.onSurfaceVariant })
        },
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(18.dp))
    PrimaryCta("Continue", name.isNotBlank() && userState == "ok", busy, onContinue)
}

@Composable
fun OtpBoxes(code: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = code,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(6)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(focus).semantics { contentType = androidx.compose.ui.autofill.ContentType.SmsOtpCode },
        textStyle = TextStyle(color = Color.Transparent),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                repeat(6) { i ->
                    val ch = code.getOrNull(i)?.toString().orEmpty()
                    val active = i == code.length
                    val filled = ch.isNotEmpty()
                    val scale by animateFloatAsState(if (filled) 1f else if (active) 1.04f else 0.96f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "otp$i")
                    val border by animateColorAsState(if (active || filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "otpb$i")
                    Box(
                        Modifier.size(width = 48.dp, height = 58.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(14.dp))
                            .background(if (filled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(if (active) 2.dp else 1.dp, border, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(ch, transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() }, label = "otpc$i") { c ->
                            Text(c, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun CountryPicker(onPick: (DialCountry) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Choose country") },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Search") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(DIAL_COUNTRIES.filter { q.isBlank() || it.name.contains(q, true) || it.dial.contains(q) }) { c ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(c) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(flagEmoji(c.iso), fontSize = 20.sp); Spacer(Modifier.width(12.dp))
                            Text(c.name, modifier = Modifier.weight(1f)); Text(c.dial, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    )
}

/** "Phone number" row for Account settings: shows the verified number or lets the member link one. */
@Composable
fun PhoneLinkRow(row: @Composable (title: String, subtitle: String, onClick: () -> Unit) -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf(com.noorpro.app.data.FirebaseGate.currentUser?.phoneNumber.orEmpty()) }
    if (com.noorpro.app.data.FirebaseGate.currentUser == null) return
    row(
        "Phone number",
        if (phone.isNotBlank()) "Verified $phone" else "Add a mobile number for calls and account recovery"
    ) { if (phone.isBlank()) open = true else android.widget.Toast.makeText(context, "Your number $phone is verified.", android.widget.Toast.LENGTH_SHORT).show() }
    if (open) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { open = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            PhoneAuthFlow(linkMode = true, onBack = { open = false }) { user ->
                phone = user.phoneNumber.orEmpty(); open = false
                android.widget.Toast.makeText(context, "Mobile number linked", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
