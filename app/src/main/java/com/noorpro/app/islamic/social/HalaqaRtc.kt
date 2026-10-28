package com.noorpro.app.islamic.social

import android.content.Context
import android.media.AudioManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.noorpro.app.calls.CallConfig
import com.noorpro.app.calls.CallSession
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Audio-only WebRTC mesh for a halaqa room, built on the 1:1 call stack: same PeerConnectionFactory
 * init, same ICE servers ([CallConfig] — STUN + optional TURN from Remote Config), same Firestore
 * signalling shape (offer/answer on a doc + candidate sub-collections), now per pair under
 * halaqa_rooms/{room}/links/{lowUid_highUid}. Every peer adds one mic track that stays disabled
 * unless the member is on stage and unmuted, so promotion to speaker needs no renegotiation.
 * Room size is capped ([HalaqaLogic.MAX_MEMBERS]); listeners only link to people on stage.
 */
class HalaqaRtc(private val context: Context, private val roomId: String, private val myUid: String, private val onHealth: (HalaqaLink) -> Unit) {
    private val db = FirebaseFirestore.getInstance()
    private val linksRef = db.collection("halaqa_rooms").document(roomId).collection("links")
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val hasTurn = CallConfig.hasTurn()
    private var factory: PeerConnectionFactory? = null
    private var mic: AudioTrack? = null
    private val peers = mutableMapOf<String, Peer>()
    private val lock = Any()
    @Volatile private var closed = false

    private inner class Peer(val other: String, val notBefore: Long) {
        val offerer = HalaqaLogic.isOfferer(myUid, other)
        val ref = linksRef.document(HalaqaLogic.linkId(myUid, other))
        var pc: PeerConnection? = null
        var state = HalaqaLink.CONNECTING
        var remoteSet = false
        var docReady = !offerer
        val pendingRemote = mutableListOf<IceCandidate>()
        val pendingLocal = mutableListOf<IceCandidate>()
        val regs = mutableListOf<ListenerRegistration>()
        val mySide get() = if (offerer) "fromCandidates" else "toCandidates"
        val theirSide get() = if (offerer) "toCandidates" else "fromCandidates"
    }

    private fun start() {
        if (factory != null) return
        CallSession.ensureInit(context)
        val f = PeerConnectionFactory.builder().setAudioDeviceModule(JavaAudioDeviceModule.builder(context).createAudioDeviceModule()).createPeerConnectionFactory()
        factory = f
        mic = f.createAudioTrack("halaqa-mic", f.createAudioSource(MediaConstraints())).apply { setEnabled(false) }
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeaker(true)
    }

    /** Re-evaluates links after a members snapshot. */
    fun sync(me: HalaqaMember, members: List<HalaqaMember>) {
        if (closed) return
        start()
        mic?.setEnabled(HalaqaLogic.micLive(me))
        val needed = HalaqaLogic.neededPeers(me, members).toSet()
        val (drop, add) = synchronized(lock) { (peers.keys - needed).toList() to (needed - peers.keys).toList() }
        drop.forEach { close(it) }
        // A link doc older than either side's current join is a leftover from an earlier session.
        add.forEach { o -> open(o, maxOf(me.joinedAt, members.firstOrNull { it.uid == o }?.joinedAt ?: 0L) - 5_000L) }
        report()
    }

    private fun report() = onHealth(HalaqaLogic.health(synchronized(lock) { peers.values.map { it.state } }, hasTurn))

    private fun open(other: String, notBefore: Long) {
        val f = factory ?: return
        val p = Peer(other, notBefore)
        synchronized(lock) { peers[other] = p }
        val rtc = PeerConnection.RTCConfiguration(CallConfig.iceServers()).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
        p.pc = f.createPeerConnection(rtc, object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate) {
                val send = synchronized(lock) { if (p.docReady) true else { p.pendingLocal += c; false } }
                if (send) writeCandidate(p, c)
            }
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState) {
                p.state = when (s) {
                    PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> HalaqaLink.CONNECTED
                    PeerConnection.IceConnectionState.FAILED -> if (hasTurn) HalaqaLink.FAILED else HalaqaLink.NEEDS_RELAY
                    else -> p.state
                }
                report()
            }
            override fun onTrack(transceiver: RtpTransceiver) {}
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
        mic?.let { p.pc?.addTrack(it, listOf("halaqa")) }
        if (p.offerer) offer(p) else answerWhenOffered(p)
    }

    private open inner class Sdp(val p: Peer) : SdpObserver {
        override fun onCreateSuccess(d: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(e: String?) { p.state = HalaqaLink.FAILED; report() }
        override fun onSetFailure(e: String?) { p.state = HalaqaLink.FAILED; report() }
    }

    private fun offer(p: Peer) {
        val c = MediaConstraints().apply { mandatory += MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true") }
        p.pc?.createOffer(object : Sdp(p) {
            override fun onCreateSuccess(d: SessionDescription?) {
                d ?: return
                p.pc?.setLocalDescription(Sdp(p), d)
                p.ref.set(mapOf("from" to myUid, "to" to p.other, "offer" to mapOf("type" to d.type.canonicalForm(), "sdp" to d.description), "at" to FieldValue.serverTimestamp()))
                    .addOnSuccessListener {
                        val batch = synchronized(lock) { p.docReady = true; p.pendingLocal.toList().also { p.pendingLocal.clear() } }
                        batch.forEach { writeCandidate(p, it) }
                        listenCandidates(p)
                        var applied = false
                        p.regs += p.ref.addSnapshotListener { s, _ ->
                            @Suppress("UNCHECKED_CAST") val a = s?.get("answer") as? Map<String, Any?>
                            if (a != null && !applied) { applied = true; applyRemote(p, SessionDescription(SessionDescription.Type.ANSWER, a["sdp"] as? String ?: "")) }
                        }
                    }
                    .addOnFailureListener { p.state = HalaqaLink.FAILED; report() }
            }
        }, c)
    }

    private fun answerWhenOffered(p: Peer) {
        var answered = false
        p.regs += p.ref.addSnapshotListener { s, _ ->
            @Suppress("UNCHECKED_CAST") val o = s?.get("offer") as? Map<String, Any?> ?: return@addSnapshotListener
            val at = s.getTimestamp("at")?.toDate()?.time ?: return@addSnapshotListener
            if (answered || at < p.notBefore || s.get("answer") != null) return@addSnapshotListener
            answered = true
            listenCandidates(p)
            applyRemote(p, SessionDescription(SessionDescription.Type.OFFER, o["sdp"] as? String ?: "")) {
                p.pc?.createAnswer(object : Sdp(p) {
                    override fun onCreateSuccess(d: SessionDescription?) {
                        d ?: return
                        p.pc?.setLocalDescription(Sdp(p), d)
                        p.ref.update("answer", mapOf("type" to d.type.canonicalForm(), "sdp" to d.description))
                    }
                }, MediaConstraints())
            }
        }
    }

    private fun applyRemote(p: Peer, desc: SessionDescription, then: () -> Unit = {}) {
        p.pc?.setRemoteDescription(object : Sdp(p) {
            override fun onSetSuccess() {
                val batch = synchronized(lock) { p.remoteSet = true; p.pendingRemote.toList().also { p.pendingRemote.clear() } }
                batch.forEach { p.pc?.addIceCandidate(it) }
                then()
            }
        }, desc)
    }

    private fun writeCandidate(p: Peer, c: IceCandidate) {
        if (closed) return
        p.ref.collection(p.mySide).add(mapOf("uid" to myUid, "sdpMid" to c.sdpMid, "sdpMLineIndex" to c.sdpMLineIndex, "candidate" to c.sdp))
    }

    private fun listenCandidates(p: Peer) {
        p.regs += p.ref.collection(p.theirSide).addSnapshotListener { s, _ ->
            s?.documentChanges.orEmpty().filter { it.type == DocumentChange.Type.ADDED }.forEach { ch ->
                val d = ch.document
                val c = IceCandidate(d.getString("sdpMid"), (d.getLong("sdpMLineIndex") ?: 0L).toInt(), d.getString("candidate").orEmpty())
                val now = synchronized(lock) { if (p.remoteSet) true else { p.pendingRemote += c; false } }
                if (now) p.pc?.addIceCandidate(c)
            }
        }
    }

    private fun close(other: String) {
        val p = synchronized(lock) { peers.remove(other) } ?: return
        p.regs.forEach { it.remove() }
        runCatching { p.pc?.dispose() }
        if (p.offerer) p.ref.delete()
    }

    fun setSpeaker(on: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= 31) runCatching {
            if (on) audio.availableCommunicationDevices.firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.let { audio.setCommunicationDevice(it) }
            else audio.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audio.isSpeakerphoneOn = on
        }
    }

    fun release() {
        if (closed) return
        closed = true
        synchronized(lock) { peers.keys.toList() }.forEach { close(it) }
        runCatching { factory?.dispose() }
        setSpeaker(false)
        audio.mode = AudioManager.MODE_NORMAL
    }
}
