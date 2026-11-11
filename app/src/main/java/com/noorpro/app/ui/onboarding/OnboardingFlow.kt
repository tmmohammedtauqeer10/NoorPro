package com.noorpro.app.ui.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.noorpro.app.data.halal.CircleGender
import com.noorpro.app.data.halal.HalalPrefs
import com.noorpro.app.data.onboarding.AgeRules
import com.noorpro.app.data.onboarding.OnbGender
import com.noorpro.app.data.onboarding.OnbStep
import com.noorpro.app.data.onboarding.OnboardingDraft
import com.noorpro.app.data.onboarding.OnboardingRepository
import com.noorpro.app.data.onboarding.OnboardingRules
import com.noorpro.app.data.onboarding.OnboardingStatus
import com.noorpro.app.data.onboarding.SimpleDate
import com.noorpro.app.data.onboarding.UsernameRules
import com.noorpro.app.data.onboarding.UsernameTakenException
import com.noorpro.app.data.onboarding.primaryLanguage
import com.noorpro.app.islamic.IslamicFlags
import com.noorpro.app.islamic.kids.KidsStore
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun todaySimple(): SimpleDate = java.util.Calendar.getInstance().let {
    SimpleDate(it.get(java.util.Calendar.YEAR), it.get(java.util.Calendar.MONTH) + 1, it.get(java.util.Calendar.DAY_OF_MONTH))
}

private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("onboarding_v115", Context.MODE_PRIVATE)
private const val SNOOZE_MS = 3L * 24 * 60 * 60 * 1000

private fun Throwable.permissionDenied(): Boolean {
    var e: Throwable? = this
    while (e != null) { if ((e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return true; e = e.cause }
    return false
}

/**
 * 1.2.0 (115): shown on top of the app right after sign-in, and for signed-in users whose profile is incomplete.
 * Never traps anyone offline: if the profile can't be loaded the app carries on and we ask again next launch.
 */
@Composable
fun OnboardingGate(viewModel: DeenViewModel) {
    if (!IslamicFlags.isOn(IslamicFlags.ONBOARDING)) return
    val screen by viewModel.currentScreen.collectAsState()
    val uid = if (viewModel.isLoggedIn) runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() else null
    if (uid == null || screen == DeenScreen.LOGIN) return
    val ctx = LocalContext.current
    var status by remember(uid) { mutableStateOf<OnboardingStatus?>(null) }
    LaunchedEffect(uid) {
        val p = prefs(ctx)
        if (System.currentTimeMillis() < p.getLong("snooze_$uid", 0)) return@LaunchedEffect
        val today = todaySimple()
        val st = runCatching { OnboardingRepository().load(today) }.getOrNull() ?: return@LaunchedEffect
        if (st.complete) {
            p.edit().putBoolean("done_$uid", true).apply()
            // New device: keep the reserved username as the local/public handle (not the email prefix).
            if (viewModel.ummahUsername != st.draft.username) viewModel.updateUmmahProfile(st.draft.fullName, st.draft.username) { _, _ -> }
        } else status = st
    }
    val st = status ?: return
    OnboardingFlow(viewModel, st, onClose = { status = null })
}

@Composable
fun OnboardingFlow(viewModel: DeenViewModel, start: OnboardingStatus, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { OnboardingRepository() }
    val today = remember { todaySimple() }
    val first = remember { OnboardingRules.firstStep(start.draft, today).takeIf { it != OnbStep.DONE } ?: OnbStep.NAME }
    var backendNotReady by remember { mutableStateOf(false) }
    var ui by remember {
        mutableStateOf(OnboardingUiState(step = first, firstStep = first, today = today,
            draft = start.draft.let { if (it.language.isBlank()) it.copy(language = defaultLanguage(viewModel)) else it }))
    }

    // Username availability (debounced) while on that step.
    LaunchedEffect(ui.step, ui.draft.username) {
        if (ui.step != OnbStep.USERNAME) return@LaunchedEffect
        val n = ui.draft.username
        if (!UsernameRules.isValid(n)) { ui = ui.copy(nameCheck = NameCheck.IDLE); return@LaunchedEffect }
        if (n == ui.draft.reservedUsername) { ui = ui.copy(nameCheck = NameCheck.AVAILABLE); return@LaunchedEffect }
        ui = ui.copy(nameCheck = NameCheck.CHECKING)
        delay(450)
        val free = runCatching { repo.isAvailable(n) }
        ui = ui.copy(nameCheck = free.fold({ if (it) NameCheck.AVAILABLE else NameCheck.TAKEN }, { NameCheck.OFFLINE }))
    }

    fun go(step: OnbStep) {
        var d = ui.draft
        var sugg = ui.suggestions
        if (step == OnbStep.USERNAME) {
            sugg = UsernameRules.suggestions(d.fullName, (System.currentTimeMillis() % 997).toInt()).filter { it != d.username }.take(4)
            if (d.username.isBlank()) d = d.copy(username = sugg.firstOrNull().orEmpty())
        }
        ui = ui.copy(step = step, draft = d, suggestions = sugg, error = null, saving = false)
    }
    fun next(of: OnbStep) = OnboardingRules.STEPS.getOrNull(OnboardingRules.STEPS.indexOf(of) + 1) ?: OnbStep.DONE

    fun applyLocal(d: OnboardingDraft) {
        viewModel.updateUmmahProfile(d.fullName.trim(), d.username) { _, _ -> }
        d.gender?.let { g ->
            HalalPrefs.gender = when (g) { OnbGender.MALE -> CircleGender.MALE; OnbGender.FEMALE -> CircleGender.FEMALE; else -> CircleGender.UNSET }
            runCatching {
                val u = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(u).collection("settings").document("safety")
                    .set(mapOf("gender" to g.code), SetOptions.merge())
            }
        }
        primaryLanguage(d.language)?.let { l ->
            runCatching { viewModel.updateLanguage(com.noorpro.app.ui.theme.AppLanguage.valueOf(l.appUi)) }
            com.noorpro.app.ui.screens.NoorAiChatStore.setLanguage(ctx, l.noorAi)
        }
        val kids = KidsStore(ctx)
        val child = d.dob?.let { AgeRules.isChild(it, today) } == true
        kids.setAccountChild(child)
        if (child) { kids.setChildName(d.fullName.trim().substringBefore(' ')); kids.setActive(true) }
    }

    fun onNext() {
        val d = ui.draft
        OnboardingRules.stepProblem(ui.step, d, today)?.let { ui = ui.copy(error = it); return }
        when (ui.step) {
            OnbStep.USERNAME -> {
                if (d.username == d.reservedUsername || backendNotReady) { go(next(ui.step)); return }
                ui = ui.copy(saving = true, error = null)
                scope.launch {
                    runCatching { repo.reserve(d.username, d.reservedUsername) }
                        .onSuccess { ui = ui.copy(draft = ui.draft.copy(reservedUsername = d.username)); go(next(OnbStep.USERNAME)) }
                        .onFailure { e ->
                            when {
                                e is UsernameTakenException || e.cause is UsernameTakenException ->
                                    ui = ui.copy(saving = false, nameCheck = NameCheck.TAKEN, error = "@${d.username} was just taken. Try another.")
                                e.permissionDenied() -> { backendNotReady = true; go(next(OnbStep.USERNAME)) }
                                else -> ui = ui.copy(saving = false, error = "Couldn't reach the server. Check your connection and try again.")
                            }
                        }
                }
            }
            OnbStep.LANGUAGE -> {
                ui = ui.copy(saving = true, error = null)
                scope.launch {
                    val r = runCatching { repo.save(d, today) }
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                    when {
                        r.isSuccess && !backendNotReady -> { applyLocal(d); prefs(ctx).edit().putBoolean("done_$uid", true).apply(); go(OnbStep.DONE) }
                        r.isSuccess || r.exceptionOrNull()?.permissionDenied() == true -> {
                            // Server rules for usernames/onboarding aren't live yet: keep the answers on this device and ask again later.
                            applyLocal(d); prefs(ctx).edit().putLong("snooze_$uid", System.currentTimeMillis() + SNOOZE_MS).apply(); go(OnbStep.DONE)
                        }
                        else -> ui = ui.copy(saving = false, error = "Couldn't save. Check your connection and try again.")
                    }
                }
            }
            else -> go(next(ui.step))
        }
    }

    BackHandler {
        val i = OnboardingRules.STEPS.indexOf(ui.step)
        if (ui.step != OnbStep.DONE && i > OnboardingRules.STEPS.indexOf(ui.firstStep)) go(OnboardingRules.STEPS[i - 1])
        // Required steps can't be skipped; back on the first step stays here.
    }

    OnboardingContent(ui, OnboardingActions(
        onDraft = { nd -> ui = ui.copy(draft = nd, error = null) },
        onNext = ::onNext,
        onBack = { val i = OnboardingRules.STEPS.indexOf(ui.step); if (i > 0) go(OnboardingRules.STEPS[i - 1]) },
        onFinish = { onClose(); viewModel.navigateTo(DeenScreen.DASHBOARD) },
        onApplyScholar = { onClose(); viewModel.navigateTo(DeenScreen.SCHOLAR_APPLY) },
    ))
}

private fun defaultLanguage(vm: DeenViewModel): String = when (runCatching { vm.appLanguage.value.name }.getOrNull()) {
    "UR" -> "ur"; "AR" -> "ar"; else -> "en"
}
