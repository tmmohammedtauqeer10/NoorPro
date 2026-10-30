package com.noorpro.app.calls

import android.content.Context
import android.media.AudioManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/** 1.0.78: one 1:1 WebRTC call with Firestore signaling (ummah_calls/{id} + candidate
 *  sub-collections). STUN by default; TURN comes from Remote Config (see [CallConfig]). */
class CallSession(
    private val context: Context,
    val callId: String,
    val isCaller: Boolean,
    val video: Boolean,
    private val listener: Listener
) {
    interface Listener {
        fun onState(state: String)
        fun onRemoteVideo(track: VideoTrack)
        fun onLocalVideo(track: VideoTrack)
    }

    companion object {
        val egl: EglBase by lazy { EglBase.create() }
        @Volatile private var initialized = false
        /** 1.1.6: shared with HalaqaRtc (audio rooms). */
        internal fun ensureInit(context: Context) {
            if (initialized) return
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions()
            )
            initialized = true
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val callRef = db.collection("ummah_calls").document(callId)
    private val listeners = mutableListOf<ListenerRegistration>()
    private var factory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private var capturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var audioTrack: AudioTrack? = null
    private var localVideo: VideoTrack? = null
    // 1.0.92: all PeerConnection callbacks + Firestore listeners touch these; guard with [lock].
    private val lock = Any()
    private var remoteSet = false
    private var answerApplied = false
    private val pendingRemote = mutableListOf<IceCandidate>()
    // 1.0.92: the caller's candidates arrive before the call doc exists; the candidate rule does
    // get(callDoc), so writing them early was silently DENIED (lost candidates => failed calls on
    // mobile networks). Buffer until the call doc write succeeds.
    private var docReady = !isCaller
    private val pendingLocal = mutableListOf<IceCandidate>()
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    @Volatile var closed = false
        private set

    private fun build() {
        ensureInit(context)
        val f = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .setAudioDeviceModule(JavaAudioDeviceModule.builder(context).createAudioDeviceModule())
            .createPeerConnectionFactory()
        factory = f
        val rtc = PeerConnection.RTCConfiguration(CallConfig.iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        pc = f.createPeerConnection(rtc, object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate) {
                val send = synchronized(lock) { if (docReady) true else { pendingLocal += c; false } }
                if (send) writeCandidate(c)
            }
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState) {
                when (s) {
                    PeerConnection.IceConnectionState.CONNECTED -> listener.onState("connected")
                    PeerConnection.IceConnectionState.DISCONNECTED -> listener.onState("reconnecting")
                    PeerConnection.IceConnectionState.FAILED -> listener.onState("failed")
                    else -> Unit
                }
            }
            override fun onTrack(transceiver: RtpTransceiver) {
                (transceiver.receiver.track() as? VideoTrack)?.let { listener.onRemoteVideo(it) }
            }
            override fun onSignalingChange(s: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>) {}
            override fun onAddStream(s: MediaStream) {}
            override fun onRemoveStream(s: MediaStream) {}
            override fun onDataChannel(d: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: RtpReceiver, s: Array<out MediaStream>) {}
        })
        val aSrc = f.createAudioSource(MediaConstraints())
        audioTrack = f.createAudioTrack("noor-audio", aSrc).also { pc?.addTrack(it, listOf("noor")) }
        if (video) startCamera(f)
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeaker(video)
    }

    private fun startCamera(f: PeerConnectionFactory) {
        val enumerator = Camera2Enumerator(context)
        val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: enumerator.deviceNames.firstOrNull() ?: return
        val cap = enumerator.createCapturer(name, null) ?: return
        val helper = SurfaceTextureHelper.create("noor-capture", egl.eglBaseContext)
        val src = f.createVideoSource(false)
        cap.initialize(helper, context, src.capturerObserver)
        cap.startCapture(1280, 720, 30)
        capturer = cap; surfaceHelper = helper; videoSource = src
        localVideo = f.createVideoTrack("noor-video", src).also {
            pc?.addTrack(it, listOf("noor"))
            listener.onLocalVideo(it)
        }
    }

    private abstract inner class Sdp : SdpObserver {
        override fun onCreateSuccess(d: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(e: String?) { listener.onState("failed") }
        override fun onSetFailure(e: String?) { listener.onState("failed") }
    }

    private fun writeCandidate(c: IceCandidate) {
        if (closed) return
        callRef.collection(if (isCaller) "callerCandidates" else "calleeCandidates")
            .add(mapOf("sdpMid" to c.sdpMid, "sdpMLineIndex" to c.sdpMLineIndex, "candidate" to c.sdp))
    }

    private fun flushLocal() {
        val batch = synchronized(lock) { docReady = true; pendingLocal.toList().also { pendingLocal.clear() } }
        batch.forEach { writeCandidate(it) }
    }

    private fun addRemoteCandidate(c: IceCandidate) {
        val now = synchronized(lock) { if (remoteSet) true else { pendingRemote += c; false } }
        if (now) pc?.addIceCandidate(c)
    }

    private fun listenCandidates(side: String) {
        listeners += callRef.collection(side).addSnapshotListener { snap, _ ->
            snap?.documentChanges.orEmpty().filter { it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED }.forEach { ch ->
                val d = ch.document
                addRemoteCandidate(IceCandidate(d.getString("sdpMid"), (d.getLong("sdpMLineIndex") ?: 0L).toInt(), d.getString("candidate").orEmpty()))
            }
        }
    }

    private fun applyRemote(desc: SessionDescription, then: () -> Unit = {}) {
        pc?.setRemoteDescription(object : Sdp() {
            override fun onSetSuccess() {
                val batch = synchronized(lock) { remoteSet = true; pendingRemote.toList().also { pendingRemote.clear() } }
                batch.forEach { pc?.addIceCandidate(it) }
                then()
            }
        }, desc)
    }

    /** Caller: build the offer, then create the ringing call document (rules enforce who may call). */
    fun startOutgoing(calleeUid: String, calleeName: String, chatId: String, onCreated: (Boolean, String?) -> Unit) {
        val me = FirebaseAuth.getInstance().currentUser ?: return onCreated(false, "Please sign in.")
        build()
        val constraints = MediaConstraints().apply {
            mandatory += MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
            mandatory += MediaConstraints.KeyValuePair("OfferToReceiveVideo", video.toString())
        }
        pc?.createOffer(object : Sdp() {
            override fun onCreateSuccess(d: SessionDescription?) {
                d ?: return
                pc?.setLocalDescription(object : Sdp() {}, d)
                callRef.set(
                    mapOf(
                        "callerUid" to me.uid,
                        "callerName" to (me.displayName ?: "Noor member"),
                        "calleeUid" to calleeUid,
                        "calleeName" to calleeName,
                        "chatId" to chatId,
                        "video" to video,
                        "status" to "ringing",
                        "offer" to mapOf("type" to d.type.canonicalForm(), "sdp" to d.description),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).addOnCompleteListener { t ->
                    if (!t.isSuccessful) { onCreated(false, t.exception?.localizedMessage); return@addOnCompleteListener }
                    onCreated(true, null)
                    flushLocal()
                    listenCandidates("calleeCandidates")
                    listeners += callRef.addSnapshotListener { snap, _ ->
                        val status = snap?.getString("status").orEmpty()
                        @Suppress("UNCHECKED_CAST")
                        val answer = snap?.get("answer") as? Map<String, Any?>
                        val apply = answer != null && synchronized(lock) { if (answerApplied) false else { answerApplied = true; true } }
                        if (apply && answer != null) {
                            applyRemote(SessionDescription(SessionDescription.Type.ANSWER, answer["sdp"] as? String ?: ""))
                        }
                        if (status.isNotBlank()) listener.onState(status)
                    }
                }
            }
        }, constraints)
    }

    /** Callee: answer the offer stored on the call document. */
    fun acceptIncoming() {
        build()
        callRef.get().addOnSuccessListener { snap ->
            @Suppress("UNCHECKED_CAST")
            val offer = snap.get("offer") as? Map<String, Any?> ?: return@addOnSuccessListener listener.onState("failed")
            applyRemote(SessionDescription(SessionDescription.Type.OFFER, offer["sdp"] as? String ?: "")) {
                pc?.createAnswer(object : Sdp() {
                    override fun onCreateSuccess(d: SessionDescription?) {
                        d ?: return
                        pc?.setLocalDescription(object : Sdp() {}, d)
                        callRef.update(
                            mapOf(
                                "answer" to mapOf("type" to d.type.canonicalForm(), "sdp" to d.description),
                                "status" to "accepted",
                                "acceptedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }, MediaConstraints())
            }
            listenCandidates("callerCandidates")
            listeners += callRef.addSnapshotListener { s, _ -> s?.getString("status")?.let { listener.onState(it) } }
        }
    }

    fun setMuted(muted: Boolean) { audioTrack?.setEnabled(!muted) }
    fun setCameraOff(off: Boolean) { localVideo?.setEnabled(!off) }
    fun switchCamera() { capturer?.switchCamera(null) }
    /** 1.0.92: API 31+ routes via setCommunicationDevice (isSpeakerphoneOn is ignored on many new devices). */
    fun setSpeaker(on: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            runCatching {
                if (on) {
                    audio.availableCommunicationDevices.firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        ?.let { audio.setCommunicationDevice(it) }
                } else audio.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            audio.isSpeakerphoneOn = on
        }
    }

    fun hangUp(status: String = "ended") {
        if (closed) return
        callRef.update(mapOf("status" to status, "endedAt" to FieldValue.serverTimestamp()))
        release()
    }

    fun release() {
        if (closed) return
        closed = true
        listeners.forEach { it.remove() }
        listeners.clear()
        runCatching { capturer?.stopCapture() }
        runCatching { capturer?.dispose() }
        runCatching { videoSource?.dispose() }
        runCatching { surfaceHelper?.dispose() }
        runCatching { pc?.dispose() }
        runCatching { factory?.dispose() }
        setSpeaker(false)
        audio.mode = AudioManager.MODE_NORMAL
    }
}

/** ICE servers come from Remote Config ([IceConfig.KEY_ICE_SERVERS], default STUN only). TURN uses
 *  short-lived credentials fetched from [IceConfig.KEY_TURN_URL] with the user's Firebase ID token, so
 *  no relay secret is compiled into the app or stored long-term in Remote Config. Legacy static keys
 *  call_turn_urls/username/credential are still honoured for 1:1 calls but should stay empty. */
object CallConfig {
    @Volatile private var creds: TurnCreds? = null

    private fun rc() = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()

    fun specs(): List<IceSpec> {
        val now = System.currentTimeMillis()
        val json = runCatching { rc().getString(IceConfig.KEY_ICE_SERVERS) }.getOrNull()
        val list = IceConfig.merge(json, creds, now).toMutableList()
        runCatching {
            val urls = rc().getString("call_turn_urls").split(',').map { it.trim() }.filter { it.startsWith("turn") }
            if (urls.isNotEmpty()) list += IceSpec(urls, rc().getString("call_turn_username"), rc().getString("call_turn_credential"))
        }
        return list
    }

    fun iceServers(): List<PeerConnection.IceServer> = specs().map { s ->
        PeerConnection.IceServer.builder(s.urls).apply {
            s.username?.let { setUsername(it) }
            s.credential?.let { setPassword(it) }
        }.createIceServer()
    }

    /** Blocking; call off the main thread. Fetches short-lived TURN creds if an endpoint is configured. */
    fun refreshTurnCredentials() {
        if (creds?.valid(System.currentTimeMillis()) == true) return
        runCatching {
            val url = rc().getString(IceConfig.KEY_TURN_URL).trim()
            if (!url.startsWith("https://")) return
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val token = com.google.android.gms.tasks.Tasks.await(user.getIdToken(false)).token ?: return
            val req = okhttp3.Request.Builder().url(url).header("Authorization", "Bearer $token")
                .post(ByteArray(0).let { okhttp3.RequestBody.create(null, it) }).build()
            okhttp3.OkHttpClient.Builder().callTimeout(8, java.util.concurrent.TimeUnit.SECONDS).build()
                .newCall(req).execute().use { r ->
                    if (r.isSuccessful) IceConfig.parseCreds(r.body?.string(), System.currentTimeMillis())?.let { creds = it }
                }
        }
    }

    /** True when a TURN relay is available (static config or fetched short-lived creds). */
    fun hasTurn(): Boolean = specs().any { it.isTurn }
}
