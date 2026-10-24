package com.noorpro.app.audio.catalog

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 1.0.93: link-out cards for scholars whose content we do NOT host. Each entry only opens the
 * scholar's own official channel in the YouTube app (or the browser) — no streaming, embedding,
 * downloading or extraction of their audio/video, and no photos (neutral initials avatar).
 *
 * To add another channel later, append one [OfficialChannel] below (verified official URL only).
 */
data class OfficialChannel(
    val id: String,
    val name: String,
    val subtitle: String,
    val url: String,
    val language: String,
    val category: String,
    /** Extra search terms (alternate spellings, script variants). */
    val keywords: List<String> = emptyList(),
) {
    val initials: String get() = name.split(' ').filter { it.firstOrNull()?.isLetter() == true }.let { w ->
        ((w.getOrNull(1) ?: w.firstOrNull())?.take(1).orEmpty() + (w.getOrNull(2) ?: w.lastOrNull())?.take(1).orEmpty()).uppercase()
    }
    val searchFields: List<String> get() = listOf(name, subtitle, category, language, "official channel", "youtube", "bayan") + keywords
}

object OfficialChannels {
    val all: List<OfficialChannel> = listOf(
        // Idara Islah-e-Muashra asked us to promote the official channel instead of hosting bayans.
        OfficialChannel(
            id = "ajmal-raza-qadri",
            name = "Muhammad Ajmal Raza Qadri",
            subtitle = "Official YouTube channel",
            url = "https://www.youtube.com/@MuhammdAjmalRazaQadriOfficial",
            language = "Urdu",
            category = "Bayan",
            keywords = listOf("Ajmal Raza Qadri", "Ajmal Raza", "Ajmal Qadri", "Ajmal", "Qadri", "Peer Ajmal Raza Qadri", "Pir Ajmal Raza Qadri",
                "Idara Islah-e-Muashra", "اجمل رضا قادری", "محمد اجمل رضا قادری"),
        ),
    )

    /** Opens [url] in the YouTube app when installed, otherwise in the browser. Never plays in-app. */
    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        val yt = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(yt); return } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: ActivityNotFoundException) { android.widget.Toast.makeText(context, "No app found to open YouTube", android.widget.Toast.LENGTH_SHORT).show() }
    }
}
