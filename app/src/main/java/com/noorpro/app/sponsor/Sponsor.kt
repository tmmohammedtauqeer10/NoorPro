package com.noorpro.app.sponsor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate

/** Remote sponsor card config (Firebase Remote Config keys sponsor_*). Hidden unless sponsor_enabled = true. */
data class SponsorConfig(
    val enabled: Boolean = false, val title: String = "", val subtitle: String = "", val imageUrl: String = "",
    val linkUrl: String = "", val buttonText: String = "Learn more", val placement: String = "home",
    val startDate: String = "", val endDate: String = "",
) {
    fun visibleOn(place: String, today: LocalDate = LocalDate.now()): Boolean {
        if (!enabled || title.isBlank() || placement.trim().lowercase() != place) return false
        if (!linkUrl.startsWith("https://") && !linkUrl.startsWith("http://")) return false
        val s = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
        val e = runCatching { LocalDate.parse(endDate.trim()) }.getOrNull()
        if (s != null && today.isBefore(s)) return false
        if (e != null && today.isAfter(e)) return false
        return true
    }
}

object SponsorRepo {
    val config = mutableStateOf(SponsorConfig())
    /** Dismissed for this process/session only. */
    var dismissed by mutableStateOf(false)
    /** Test-only: JVM previews set this to bypass Remote Config. */
    var previewConfig: SponsorConfig? = null
    private var started = false

    fun load() {
        if (started) return
        started = true
        runCatching {
            val rc = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            rc.setConfigSettingsAsync(
                com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build()
            )
            rc.setDefaultsAsync(mapOf<String, Any>("sponsor_enabled" to false))
            fun apply() {
                config.value = SponsorConfig(
                    enabled = rc.getBoolean("sponsor_enabled"),
                    title = rc.getString("sponsor_title"), subtitle = rc.getString("sponsor_subtitle"),
                    imageUrl = rc.getString("sponsor_image_url"), linkUrl = rc.getString("sponsor_link_url").trim(),
                    buttonText = rc.getString("sponsor_button_text").ifBlank { "Learn more" },
                    placement = rc.getString("sponsor_placement").ifBlank { "home" },
                    startDate = rc.getString("sponsor_start_date"), endDate = rc.getString("sponsor_end_date"),
                )
            }
            apply() // last cached values (works offline)
            rc.fetchAndActivate().addOnCompleteListener { runCatching { apply() } }
        }
    }
}

@Composable
fun SponsorCardSlot(placement: String = "home", modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { SponsorRepo.load() }
    val live by SponsorRepo.config
    val cfg = SponsorRepo.previewConfig ?: live
    if (SponsorRepo.dismissed || !cfg.visibleOn(placement)) return
    val ctx = LocalContext.current
    SponsorCard(cfg, modifier, onOpen = {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cfg.linkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }, onDismiss = { SponsorRepo.dismissed = true })
}

@Composable
fun SponsorCard(cfg: SponsorConfig, modifier: Modifier = Modifier, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sponsored", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(scheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 3.dp))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Hide sponsor", tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
                    if (cfg.imageUrl.isNotBlank()) AsyncImage(cfg.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text(cfg.title.take(1).uppercase(), color = scheme.onPrimaryContainer, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(cfg.title, color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (cfg.subtitle.isNotBlank()) Text(cfg.subtitle, color = scheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(cfg.buttonText) }
        }
    }
}
