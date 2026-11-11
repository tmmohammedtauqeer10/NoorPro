package com.noorpro.app.data

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * One place that answers "is Firebase usable in this process?".
 *
 * NoorProApplication deliberately keeps running when FirebaseApp.initializeApp() returns null
 * (no google-services config: local/dev builds, Robolectric). Repositories used to call
 * FirebaseFirestore.getInstance() eagerly in their constructors, so merely composing the profile
 * screen crashed in that case. They now read Firebase lazily and treat "not configured" like
 * "signed out / no data" instead of throwing.
 */
object FirebaseGate {
    /** Tests can force a value; null = detect. */
    @Volatile internal var override: Boolean? = null

    val ready: Boolean
        get() = override ?: runCatching { FirebaseApp.getInstance() }.isSuccess

    /** The signed-in user, or null when signed out or Firebase is not configured. */
    val currentUser: FirebaseUser?
        get() = if (ready) runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() else null

    const val OFFLINE = "Community is not available right now."
}
