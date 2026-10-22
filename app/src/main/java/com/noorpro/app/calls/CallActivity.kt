package com.noorpro.app.calls

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.noorpro.app.data.UmmahRepository
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/** Full-screen 1:1 call UI (outgoing, incoming and in-call). Works over the lock screen. */
class CallActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CALL_ID = "callId"
        const val EXTRA_INCOMING = "incoming"
        const val EXTRA_VIDEO = "video"
        const val EXTRA_OTHER_UID = "otherUid"
        const val EXTRA_OTHER_NAME = "otherName"
        @Volatile var activeCallId: String? = null

        fun startOutgoing(context: Context, otherUid: String, otherName: String, video: Boolean) {
            val id = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ummah_calls").document().id
            context.startActivity(Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, id); putExtra(EXTRA_INCOMING, false); putExtra(EXTRA_VIDEO, video)
                putExtra(EXTRA_OTHER_UID, otherUid); putExtra(EXTRA_OTHER_NAME, otherName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        fun incomingIntent(context: Context, callId: String, callerUid: String, callerName: String, video: Boolean) =
            Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId); putExtra(EXTRA_INCOMING, true); putExtra(EXTRA_VIDEO, video)
                putExtra(EXTRA_OTHER_UID, callerUid); putExtra(EXTRA_OTHER_NAME, callerName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }

    private var session: CallSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
        if (callId.isBlank() || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) { finish(); return }
        activeCallId = callId
        val incoming = intent.getBooleanExtra(EXTRA_INCOMING, false)
        val video = intent.getBooleanExtra(EXTRA_VIDEO, false)
        val otherUid = intent.getStringExtra(EXTRA_OTHER_UID).orEmpty()
        val otherName = intent.getStringExtra(EXTRA_OTHER_NAME).orEmpty().ifBlank { "Noor member" }
        getSystemService(NotificationManager::class.java)?.cancel(CallNotifications.notificationId(callId))
        setContent {
            CallScreen(callId, incoming, video, otherUid, otherName, onSession = { session = it }, onFinish = { finish() })
        }
    }

    override fun onDestroy() {
        session?.let { if (!it.closed) it.hangUp() }
        if (activeCallId == intent.getStringExtra(EXTRA_CALL_ID)) activeCallId = null
        super.onDestroy()
    }
}

private fun formatDuration(sec: Long) = "%d:%02d".format(sec / 60, sec % 60)

@Composable
internal fun CallScreen(
    callId: String,
    incoming: Boolean,
    video: Boolean,
    otherUid: String,
    otherName: String,
    onSession: (CallSession) -> Unit,
    onFinish: () -> Unit,
    previewState: String? = null,
    previewElapsed: Long = 0
) {
    val context = LocalContextCompat()
    var state by remember { mutableStateOf(previewState ?: if (incoming) "incoming" else "starting") }
    var session by remember { mutableStateOf<CallSession?>(null) }
    var remoteTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var localTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(video) }
    var cameraOff by remember { mutableStateOf(false) }
    var connectedAt by remember { mutableStateOf(0L) }
    var elapsed by remember { mutableStateOf(previewElapsed) }
    var error by remember { mutableStateOf<String?>(null) }
    var logged by remember { mutableStateOf(false) }

    fun logCall(final: String) {
        if (incoming || logged || otherUid.isBlank()) return
        logged = true
        val kind = if (video) "video call" else "voice call"
        val text = when {
            connectedAt > 0 -> "${kind.replaceFirstChar { it.uppercase() }} · ${formatDuration(elapsed)}"
            final == "declined" -> "Declined $kind"
            else -> "Missed $kind"
        }
        UmmahRepository().sendChatMessage(context, otherUid, otherName, text, "call", null) { _, _ -> }
    }

    fun finishWith(final: String) {
        logCall(final)
        session?.let { if (!it.closed) it.hangUp(if (final == "missed" || final == "declined") final else "ended") }
        onFinish()
    }

    val callbacks = remember {
        object : CallSession.Listener {
            override fun onState(s: String) {
                when (s) {
                    "connected" -> { if (connectedAt == 0L) connectedAt = System.currentTimeMillis(); state = "connected" }
                    "accepted" -> if (state != "connected") state = "connecting"
                    "ringing" -> if (!incoming && state != "connected") state = "ringing"
                    "reconnecting" -> state = "reconnecting"
                    "ended", "declined", "missed", "busy", "failed" -> {
                        state = s
                    }
                }
            }
            override fun onRemoteVideo(track: VideoTrack) { remoteTrack = track }
            override fun onLocalVideo(track: VideoTrack) { localTrack = track }
        }
    }

    fun begin() {
        val s = CallSession(context, callId, isCaller = !incoming, video = video, listener = callbacks)
        session = s; onSession(s)
        if (incoming) {
            state = "connecting"; s.acceptIncoming()
        } else {
            val chatId = listOfNotNull(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid, otherUid).sorted().joinToString("_")
            s.startOutgoing(otherUid, otherName, chatId) { ok, err ->
                if (ok) state = "ringing" else {
                    error = if (err?.contains("PERMISSION", true) == true)
                        "$otherName needs to follow you before you can call." else (err ?: "Unable to start the call.")
                    state = "failed"; logged = true; s.release()
                }
            }
        }
    }

    val needed = remember { buildList { add(Manifest.permission.RECORD_AUDIO); if (video) add(Manifest.permission.CAMERA) }.toTypedArray() }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) begin() else { error = "Microphone${if (video) " and camera" else ""} permission is needed for calls."; state = "failed" }
    }
    fun requestAndBegin() {
        if (needed.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) begin() else permissions.launch(needed)
    }

    // Incoming: watch for the caller hanging up before we answer.
    if (previewState == null) DisposableEffect(callId, incoming) {
        val reg = if (incoming) com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ummah_calls").document(callId)
            .addSnapshotListener { snap, _ ->
                val st = snap?.getString("status").orEmpty()
                if (state == "incoming" && st.isNotBlank() && st != "ringing") state = if (st == "accepted") "ended" else st
            } else null
        onDispose { reg?.remove() }
    }
    if (previewState == null) LaunchedEffect(Unit) { if (!incoming) requestAndBegin() }
    if (previewState == null) LaunchedEffect(state) {
        when (state) {
            "ringing" -> { delay(45_000); if (state == "ringing") { state = "missed" } }
            "incoming" -> { delay(50_000); if (state == "incoming") onFinish() }
            "ended", "declined", "missed", "busy", "failed" -> {
                if (state != "failed" || error == null) logCall(state)
                session?.let { if (!it.closed) it.hangUp(if (state == "missed" || state == "declined") state else "ended") }
                delay(if (error != null) 2_500 else 1_200); onFinish()
            }
        }
    }
    LaunchedEffect(connectedAt) {
        while (connectedAt > 0) { elapsed = (System.currentTimeMillis() - connectedAt) / 1000; delay(1000) }
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF06231C), Color(0xFF0B3D31), Color(0xFF041611))))
    ) {
        if (video && remoteTrack != null && state != "incoming") VideoView(remoteTrack!!, mirror = false, modifier = Modifier.fillMaxSize())
        if (video && localTrack != null && !cameraOff) {
            VideoView(localTrack!!, mirror = true, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).size(110.dp, 160.dp).clip(RoundedCornerShape(16.dp)))
        }
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!(video && remoteTrack != null && state == "connected")) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val s by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
                val animate = state in listOf("incoming", "ringing", "starting", "connecting")
                Box(
                    Modifier.size(116.dp).scale(if (animate) s else 1f).clip(CircleShape).background(Color(0xFF138A6B)),
                    contentAlignment = Alignment.Center
                ) { Text(otherName.take(1).uppercase(), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(18.dp))
            }
            Text(otherName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            val label = when (state) {
                "incoming" -> if (video) "Incoming video call…" else "Incoming voice call…"
                "starting" -> "Starting…"
                "ringing" -> "Ringing…"
                "connecting" -> "Connecting…"
                "reconnecting" -> "Reconnecting…"
                "connected" -> formatDuration(elapsed)
                "declined" -> "Call declined"
                "missed" -> "No answer"
                "busy" -> "Busy"
                "failed" -> error ?: "Call failed"
                else -> "Call ended"
            }
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, modifier = Modifier.padding(horizontal = 24.dp))
            Text("End-to-end encrypted (WebRTC)", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state == "incoming") {
                Row(horizontalArrangement = Arrangement.spacedBy(72.dp)) {
                    CallButton(Icons.Default.CallEnd, "Decline", Color(0xFFE5484D)) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("ummah_calls").document(callId)
                            .update(mapOf("status" to "declined", "endedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                        onFinish()
                    }
                    CallButton(if (video) Icons.Default.Videocam else Icons.Default.Call, "Accept", Color(0xFF22C55E)) { requestAndBegin() }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    ToggleButton(if (muted) Icons.Default.MicOff else Icons.Default.Mic, "Mute", muted) { muted = !muted; session?.setMuted(muted) }
                    ToggleButton(Icons.AutoMirroredVolume, "Speaker", speaker) { speaker = !speaker; session?.setSpeaker(speaker) }
                    if (video) {
                        ToggleButton(if (cameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam, "Camera", cameraOff) { cameraOff = !cameraOff; session?.setCameraOff(cameraOff) }
                        ToggleButton(Icons.Default.Cameraswitch, "Flip", false) { session?.switchCamera() }
                    }
                }
                Spacer(Modifier.height(26.dp))
                CallButton(Icons.Default.CallEnd, "End", Color(0xFFE5484D)) { finishWith(if (state == "ringing") "missed" else "ended") }
            }
        }
    }
}

private val Icons.AutoMirroredVolume: ImageVector get() = Icons.AutoMirrored.Filled.VolumeUp

@Composable
private fun LocalContextCompat(): Context = androidx.compose.ui.platform.LocalContext.current

@Composable
private fun VideoView(track: VideoTrack, mirror: Boolean, modifier: Modifier) {
    val renderer = remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(CallSession.egl.eglBaseContext, null)
                setMirror(mirror)
                setEnableHardwareScaler(true)
                if (mirror) setZOrderMediaOverlay(true)
                renderer.value = this
            }
        },
        modifier = modifier
    )
    DisposableEffect(track, renderer.value) {
        val r = renderer.value
        if (r != null) track.addSink(r)
        onDispose { if (r != null) runCatching { track.removeSink(r) } }
    }
    DisposableEffect(Unit) { onDispose { renderer.value?.release() } }
}

@Composable
private fun CallButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).clip(CircleShape).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ToggleButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(if (active) Color.White else Color.White.copy(alpha = 0.16f)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = label, tint = if (active) Color(0xFF0B3D31) else Color.White) }
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}
