package com.noorpro.app.calls

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.noorpro.app.data.ChatRealtime

/** While the app is in the foreground: ring for calls even if the FCM push is delayed, and keep
 *  the member's online / last-seen presence fresh. */
object IncomingCallWatcher {
    private var reg: ListenerRegistration? = null
    private val shown = mutableSetOf<String>()

    fun start(context: Context) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        ChatRealtime.setOnline(true)
        ChatRealtime.registerPushToken()
        if (reg != null) return
        val app = context.applicationContext
        reg = FirebaseFirestore.getInstance().collection("ummah_calls")
            .whereEqualTo("calleeUid", me)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snap, _ ->
                snap?.documents.orEmpty().forEach { d ->
                    val created = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                    if (System.currentTimeMillis() - created > 45_000L || !shown.add(d.id)) return@forEach
                    if (CallActivity.activeCallId != null) {
                        d.reference.update("status", "busy"); return@forEach
                    }
                    app.startActivity(
                        CallActivity.incomingIntent(app, d.id, d.getString("callerUid").orEmpty(), d.getString("callerName").orEmpty(), d.getBoolean("video") == true)
                    )
                }
            }
    }

    fun stop() {
        reg?.remove(); reg = null
        if (FirebaseAuth.getInstance().currentUser != null) ChatRealtime.setOnline(false)
    }
}
