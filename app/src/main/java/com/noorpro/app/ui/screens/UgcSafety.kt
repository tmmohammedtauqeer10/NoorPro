package com.noorpro.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.UmmahRepository

/** Local record that the user accepted the community terms (required before any UGC action). */
object UgcTerms {
    const val CURRENT_VERSION = 1
    const val TERMS_URL = "https://noor-pro-d87e3.web.app/terms"
    const val PRIVACY_URL = "https://noor-pro-d87e3.web.app/privacy"
    const val MODERATION_EMAIL = "noorpro.official@gmail.com"
    private const val PREFS = "ugc_terms"
    private const val KEY = "accepted_version"

    fun isAccepted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 0) >= CURRENT_VERSION

    fun accept(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, CURRENT_VERSION).apply()
    }

    /** Report reasons offered on every surface (stored verbatim in `ummah_reports.reason`). */
    val REPORT_REASONS = listOf(
        "Spam or scam",
        "Hate speech or harassment",
        "Nudity or sexual content",
        "Violence or dangerous content",
        "False or misleading religious content",
        "Child safety concern",
        "Copyright or impersonation",
        "Other"
    )

    fun openUrl(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/** Gate used before posting / uploading / commenting / chatting. */
class UgcTermsGate internal constructor(private val context: Context) {
    internal var showing by mutableStateOf(false)

    /** True if the terms are already accepted; otherwise shows the acceptance dialog and returns false. */
    fun check(): Boolean {
        if (UgcTerms.isAccepted(context)) return true
        showing = true
        return false
    }
}

@Composable
fun rememberUgcTermsGate(): UgcTermsGate {
    val context = LocalContext.current
    val gate = remember { UgcTermsGate(context) }
    if (gate.showing) {
        var agreed by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { gate.showing = false },
            title = { Text("Community terms") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Before you post, comment or chat, please accept the Noor Pro Terms. There is no tolerance for " +
                            "hateful, harassing, sexual, violent or otherwise objectionable content. Anyone can report " +
                            "content or block a user, reports are reviewed within 24 hours, and offending content and " +
                            "accounts are removed.",
                        fontSize = 14.sp
                    )
                    Row {
                        Text("Terms of Service", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { UgcTerms.openUrl(context, UgcTerms.TERMS_URL) })
                        Text("  \u00B7  ")
                        Text("Privacy Policy", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { UgcTerms.openUrl(context, UgcTerms.PRIVACY_URL) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { agreed = !agreed }) {
                        Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                        Text("I agree to the Terms of Service and community rules.", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = agreed, onClick = {
                    UgcTerms.accept(context)
                    gate.showing = false
                    Toast.makeText(context, "Thanks. You can now post, comment and chat.", Toast.LENGTH_SHORT).show()
                }) { Text("Accept") }
            },
            dismissButton = { TextButton(onClick = { gate.showing = false }) { Text("Decline") } }
        )
    }
    return gate
}

/** Blocked-user set for the signed-in account (empty when signed out). Live. */
@Composable
fun rememberBlockedUids(): Set<String> {
    var blocked by remember { mutableStateOf(emptySet<String>()) }
    val repo = remember { runCatching { UmmahRepository() }.getOrNull() }
    DisposableEffect(repo) {
        runCatching { repo?.observeBlockedUsers { blocked = it } }
        onDispose { runCatching { repo?.close() } }
    }
    return blocked
}

/** What is being reported. */
data class ReportTarget(
    val type: String,          // post | reel | comment | message | group | user
    val id: String,
    val ownerUid: String = "",
    val ownerLabel: String = "",
    val snippet: String = ""
)

/** Reason picker + optional "also block this user". Self-contained (own repository, no listeners). */
@Composable
fun UgcReportDialog(target: ReportTarget, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { runCatching { UmmahRepository() }.getOrNull() }
    val myUid = remember { repo?.currentUserUid().orEmpty() }
    var reason by remember { mutableStateOf(UgcTerms.REPORT_REASONS.first()) }
    var block by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    val canBlock = target.ownerUid.isNotBlank() && target.ownerUid != myUid
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Report ${target.type}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Why are you reporting this? We review every report within 24 hours.", fontSize = 13.sp)
                UgcTerms.REPORT_REASONS.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { reason = option }
                    ) {
                        RadioButton(selected = reason == option, onClick = { reason = option })
                        Text(option, fontSize = 14.sp)
                    }
                }
                if (canBlock) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable { block = !block }
                    ) {
                        Checkbox(checked = block, onCheckedChange = { block = it })
                        Text("Also block ${target.ownerLabel.ifBlank { "this user" }}", fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !sending, onClick = {
                if (repo == null || myUid.isBlank()) {
                    Toast.makeText(context, "Sign in to report content.", Toast.LENGTH_SHORT).show()
                    onDismiss()
                    return@TextButton
                }
                sending = true
                repo.reportContent(
                    type = target.type, targetId = target.id, targetUid = target.ownerUid,
                    reason = reason, details = target.snippet.take(300)
                ) { ok ->
                    if (ok && block && canBlock) repo.setBlocked(target.ownerUid, true) { }
                    Toast.makeText(
                        context,
                        if (ok) "Thanks. Our team will review this within 24 hours." else "Unable to send the report. Try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    sending = false
                    if (ok) onDismiss()
                }
            }) { Text("Submit report") }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Three-dots button with Report (+ Block/Unblock) for a piece of content. */
@Composable
fun UgcOverflowButton(target: ReportTarget, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    val repo = remember { runCatching { UmmahRepository() }.getOrNull() }
    val myUid = remember { repo?.currentUserUid().orEmpty() }
    val canBlock = target.ownerUid.isNotBlank() && target.ownerUid != myUid
    val isOwn = target.ownerUid.isNotBlank() && target.ownerUid == myUid
    if (!isOwn) IconButton(onClick = { menu = true }, modifier = modifier) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = tint)
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Report") }, onClick = {
                menu = false
                if (myUid.isBlank()) Toast.makeText(context, "Sign in to report content.", Toast.LENGTH_SHORT).show() else report = true
            })
            if (canBlock) {
                DropdownMenuItem(text = { Text("Block ${target.ownerLabel.ifBlank { "user" }}") }, onClick = {
                    menu = false
                    if (myUid.isBlank()) Toast.makeText(context, "Sign in to block users.", Toast.LENGTH_SHORT).show() else confirmBlock = true
                })
            }
        }
    }
    if (report) UgcReportDialog(target) { report = false }
    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text("Block ${target.ownerLabel.ifBlank { "this user" }}?") },
            text = { Text("Their posts, comments and messages will be hidden from you. You can unblock them any time in Settings > Blocked accounts.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBlock = false
                    repo?.setBlocked(target.ownerUid, true) { ok ->
                        Toast.makeText(context, if (ok) "Blocked." else "Unable to block.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text("Cancel") } }
        )
    }
}
