package com.noorpro.app.ui.screens

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

fun friendlyPhoneError(e: Exception?): String = when (e) {
    is FirebaseTooManyRequestsException -> "Too many attempts from this device. Please wait a while and try again."
    is FirebaseAuthUserCollisionException -> "This number is already linked to another Noor account."
    is FirebaseAuthInvalidCredentialsException ->
        if (e.errorCode == "ERROR_INVALID_VERIFICATION_CODE") "That code is incorrect. Check the SMS and try again." else "Enter a valid mobile number."
    else -> {
        val m = e?.localizedMessage.orEmpty()
        when {
            m.contains("BILLING", true) || m.contains("quota", true) -> "Phone sign-in is temporarily unavailable. Please use Google or email."
            m.contains("region", true) -> "SMS to this country is not enabled yet."
            m.contains("app", true) && m.contains("not authorized", true) -> "This app build is not authorised for phone sign-in yet."
            else -> m.ifBlank { "Unable to verify the number. Try again." }
        }
    }
}

/**
 * Full-screen phone flow. [linkMode] = attach the number to the signed-in account instead of
 * signing in. [onDone] receives the signed-in/linked user.
 */
@Composable
fun PhoneAuthFlow(linkMode: Boolean, onBack: () -> Unit, previewStep: String? = null, previewCode: String = "", onDone: (FirebaseUser) -> Unit) {
    val context = LocalContext.current
    val auth = remember { if (previewStep != null) null else runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    var country by remember { mutableStateOf(DIAL_COUNTRIES.first()) }
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(previewStep ?: "phone") }
    var code by remember { mutableStateOf(previewCode) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resendIn by remember { mutableStateOf(0) }
    var pickCountry by remember { mutableStateOf(false) }
    val fullNumber = country.dial + number.filter { it.isDigit() }.trimStart('0')

    fun finish(credential: PhoneAuthCredential) {
        val auth = auth ?: return
        busy = true; error = null
        val current = auth.currentUser
        val task = if (linkMode && current != null) current.linkWithCredential(credential) else auth.signInWithCredential(credential)
        task.addOnCompleteListener { t ->
            val user = t.result?.user
            if (t.isSuccessful && user != null) {
                if (!linkMode && user.displayName.isNullOrBlank() && name.isNotBlank()) {
                    user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())
                        .addOnCompleteListener { busy = false; onDone(auth.currentUser ?: user) }
                } else { busy = false; onDone(user) }
            } else { busy = false; error = friendlyPhoneError(t.exception) }
        }
    }

    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // Instant verification or SMS auto-retrieval (Play services reads the OTP for us).
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                credential.smsCode?.let { code = it }
                finish(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) { busy = false; error = friendlyPhoneError(e) }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id; resendToken = token; busy = false; step = "otp"; resendIn = 60
            }
        }
    }

    fun send(resend: Boolean = false) {
        val auth = auth ?: return
        val activity = context.findActivity() ?: return
        val digits = number.filter { it.isDigit() }
        if (digits.length < 6) { error = "Enter your mobile number."; return }
        busy = true; error = null
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (resend) resendToken?.let { builder.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    if (previewStep == "otp" && resendIn == 0 && previewCode.length < 6) resendIn = 42
    LaunchedEffect(resendIn) { if (previewStep == null && resendIn > 0) { delay(1000); resendIn -= 1 } }
    LaunchedEffect(code) {
        if (code.length == 6 && step == "otp" && !busy) verificationId?.let { finish(PhoneAuthProvider.getCredential(it, code)) }
    }

    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
        ) {
            IconButton(onClick = { if (step == "otp") { step = "phone"; code = "" } else onBack() }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(step, label = "phoneStep") { st ->
                Column {
                    if (st == "phone") {
                        Text(if (linkMode) "Add your mobile number" else "Continue with phone", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                        Spacer(Modifier.height(6.dp))
                        Text("We'll text you a 6-digit code. Standard SMS rates may apply.", color = scheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                Modifier.height(56.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                                    .clickable { pickCountry = true }.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(flagEmoji(country.iso), fontSize = 20.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(country.dial, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose country")
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedTextField(
                                value = number, onValueChange = { v -> number = v.filter { it.isDigit() || it == ' ' }.take(16); error = null },
                                placeholder = { Text("Mobile number") }, singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
                            )
                        }
                        if (!linkMode) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                                label = { Text("Your name (for new accounts)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { send() }, enabled = !busy, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Send code", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Enter the code", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                        Spacer(Modifier.height(6.dp))
                        Text("Sent to $fullNumber. We'll fill it in automatically if this phone receives the SMS.", color = scheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.height(26.dp))
                        OtpBoxes(code) { code = it; error = null }
                        Spacer(Modifier.height(22.dp))
                        Button(
                            onClick = { verificationId?.let { finish(PhoneAuthProvider.getCredential(it, code)) } },
                            enabled = !busy && code.length == 6, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (linkMode) "Verify & link" else "Verify", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = { send(resend = true) }, enabled = resendIn == 0 && !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(if (resendIn > 0) "Resend code in 0:${"%02d".format(resendIn)}" else "Resend code", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            error?.let { Text(it, color = scheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)) }
        }
    }
    if (pickCountry) CountryPicker(onPick = { country = it; pickCountry = false }, onDismiss = { pickCountry = false })
}

@Composable
fun OtpBoxes(code: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = code,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(6)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(focus),
        textStyle = TextStyle(color = Color.Transparent),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { i ->
                    val ch = code.getOrNull(i)?.toString().orEmpty()
                    val active = i == code.length
                    Box(
                        Modifier.size(width = 46.dp, height = 56.dp).clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(if (active) 2.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(ch, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
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
    var phone by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty()) }
    if (FirebaseAuth.getInstance().currentUser == null) return
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
