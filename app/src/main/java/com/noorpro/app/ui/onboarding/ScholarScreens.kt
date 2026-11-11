package com.noorpro.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.noorpro.app.data.halal.ModeratorRepository
import com.noorpro.app.data.onboarding.ApplicationStatus
import com.noorpro.app.data.onboarding.ScholarApplication
import com.noorpro.app.data.onboarding.ScholarApplicationRepository
import com.noorpro.app.data.onboarding.ScholarRules
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

private const val MAX_CERT_BYTES = 10L * 1024 * 1024

/** 1.2.0 (115) "Apply as a scholar" (route DeenScreen.SCHOLAR_APPLY). */
@Composable
fun ScholarApplyScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ScholarApplicationRepository() }
    val signedIn = repo.myUid != null
    var ui by remember { mutableStateOf(ScholarApplyUiState(loading = signedIn, signedIn = signedIn, form = ScholarApplication(name = viewModel.userDisplayName))) }
    var cert by remember { mutableStateOf<Uri?>(null) }
    var certMime by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(signedIn) {
        if (!signedIn) return@LaunchedEffect
        val mine = runCatching { repo.mine() }
        ui = mine.fold(
            { m -> ui.copy(loading = false, existing = m, form = m?.copy(status = ApplicationStatus.NONE) ?: ui.form) },
            { ui.copy(loading = false, error = "Couldn't load your application. Check your connection.") })
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cr = ctx.contentResolver
        val mime = cr.getType(uri).orEmpty()
        var name = "certificate"; var size = 0L
        runCatching { cr.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) {
            c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
            c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) } } } }
        when {
            !(mime.startsWith("image/") || mime == "application/pdf") -> Toast.makeText(ctx, "Choose a photo or a PDF.", Toast.LENGTH_SHORT).show()
            size > MAX_CERT_BYTES -> Toast.makeText(ctx, "That file is over 10 MB.", Toast.LENGTH_SHORT).show()
            else -> { cert = uri; certMime = mime; ui = ui.copy(certLabel = name, certPreview = if (mime.startsWith("image/")) uri else null) }
        }
    }

    BackHandler(enabled = ui.editing && ui.existing != null) { ui = ui.copy(editing = false) }
    ScholarApplyContent(ui, ScholarApplyActions(
        onBack = { viewModel.goBack() },
        onForm = { ui = ui.copy(form = it, error = null) },
        onPickCertificate = { picker.launch("*/*") },
        onApplyAgain = { ui = ui.copy(editing = true, showErrors = false) },
        onSubmit = submit@{
            val hasCert = cert != null || ui.form.certPath.isNotBlank()
            if (ScholarRules.problems(ui.form, hasCert).isNotEmpty()) { ui = ui.copy(showErrors = true, error = "Please fix the highlighted fields."); return@submit }
            ui = ui.copy(submitting = true, error = null)
            scope.launch {
                val ext = when { certMime == "application/pdf" -> "pdf"; certMime == "image/png" -> "png"; certMime == "image/webp" -> "webp"; else -> "jpg" }
                runCatching { repo.submit(ui.form, cert, certMime, ext) }
                    .onSuccess {
                        val uid = repo.myUid.orEmpty()
                        ui = ui.copy(submitting = false, editing = false, existing = ui.form.copy(uid = uid, status = ApplicationStatus.PENDING, submittedAt = System.currentTimeMillis()))
                        Toast.makeText(ctx, "Application sent. JazakAllahu khairan.", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { e ->
                        val denied = generateSequence(e) { it.cause }.any { it.message.orEmpty().contains("PERMISSION", true) || it.message.orEmpty().contains("not authorized", true) }
                        ui = ui.copy(submitting = false, error = if (denied) "Scholar applications aren't open on the server yet. Please try again later." else "Couldn't send. Check your connection and try again.")
                    }
            }
        },
    ))
}

/** 1.2.0 (115) admin review (route DeenScreen.SCHOLAR_REVIEW). Admin = `admin` custom claim or an admins/{uid} doc. */
@Composable
fun ScholarReviewScreen(viewModel: DeenViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ScholarApplicationRepository() }
    var ui by remember { mutableStateOf(ScholarReviewUiState()) }

    fun reload() {
        ui = ui.copy(loading = true, error = null)
        scope.launch {
            val p = runCatching { repo.pending() }
            val v = runCatching { repo.verifiedScholars() }
            ui = ui.copy(loading = false, pending = p.getOrDefault(ui.pending), approved = v.getOrDefault(ui.approved), now = System.currentTimeMillis(),
                error = if (p.isFailure) "Couldn't load applications. If this persists, the 1.2.0 rules may not be deployed yet." else null)
        }
    }
    LaunchedEffect(Unit) { ModeratorRepository().checkModerator { ok -> ui = ui.copy(isModerator = ok); if (ok) reload() } }

    fun close() { ui = ui.copy(selected = null, certUrl = null, certIsPdf = false, decision = null, reason = "", error = null, busy = false) }
    BackHandler(enabled = ui.selected != null) { close() }

    ScholarReviewContent(ui, ScholarReviewActions(
        onBack = { viewModel.goBack() },
        onTab = { ui = ui.copy(tab = it) },
        onOpen = { app ->
            ui = ui.copy(selected = app, certUrl = null, certIsPdf = app.certPath.endsWith(".pdf", true), decision = null, reason = "", error = null)
            if (app.certPath.isNotBlank()) scope.launch { val u = repo.certificateUrl(app.certPath); if (ui.selected?.uid == app.uid) ui = ui.copy(certUrl = u) }
        },
        onCloseDetail = ::close,
        onOpenCertificate = { ui.certUrl?.let { u -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } },
        onApprove = approve@{
            val app = ui.selected ?: return@approve
            ui = ui.copy(busy = true, error = null)
            scope.launch {
                runCatching { repo.approve(app) }
                    .onSuccess { Toast.makeText(ctx, "Approved ${app.name}.", Toast.LENGTH_SHORT).show(); close(); reload() }
                    .onFailure { ui = ui.copy(busy = false, error = "Couldn't approve: ${it.message?.take(120)}") }
            }
        },
        onStartDecision = { ui = ui.copy(decision = it, reason = "", error = null) },
        onReason = { ui = ui.copy(reason = it) },
        onConfirmDecision = confirm@{
            val app = ui.selected ?: return@confirm
            if (ScholarRules.rejectProblem(ui.reason) != null) return@confirm
            val revoke = ui.decision == "revoke"
            ui = ui.copy(busy = true, error = null)
            scope.launch {
                runCatching { if (revoke) repo.revoke(app, ui.reason) else repo.reject(app, ui.reason) }
                    .onSuccess { Toast.makeText(ctx, if (revoke) "Verification revoked." else "Application rejected.", Toast.LENGTH_SHORT).show(); close(); reload() }
                    .onFailure { ui = ui.copy(busy = false, error = "Couldn't save: ${it.message?.take(120)}") }
            }
        },
    ))
}
