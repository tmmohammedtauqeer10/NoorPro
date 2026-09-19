package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri

interface PilgrimMapProvider {
    fun openPlace(context: Context, query: String, latitude: Double? = null, longitude: Double? = null): Boolean
    fun openDirections(context: Context, destination: String, latitude: Double? = null, longitude: Double? = null): Boolean
    fun shareLocation(context: Context, latitude: Double, longitude: Double, label: String): Boolean
}

/** Google Maps compatible intent provider. It uses no embedded key and safely falls back to a browser/map app. */
class GoogleMapsIntentProvider : PilgrimMapProvider {
    override fun openPlace(context: Context, query: String, latitude: Double?, longitude: Double?): Boolean {
        val target = if (latitude != null && longitude != null) "$latitude,$longitude (${Uri.encode(query)})" else Uri.encode(query)
        return launch(context, Uri.parse("geo:0,0?q=$target")) ||
            launch(context, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"))
    }

    override fun openDirections(context: Context, destination: String, latitude: Double?, longitude: Double?): Boolean {
        val target = if (latitude != null && longitude != null) "$latitude,$longitude" else destination
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(target)}&travelmode=walking")
        return launch(context, uri)
    }

    override fun shareLocation(context: Context, latitude: Double, longitude: Double, label: String): Boolean {
        val url = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        return runCatching {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "$label\n$url")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    "Share location"
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }

    private fun launch(context: Context, uri: Uri): Boolean = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}

