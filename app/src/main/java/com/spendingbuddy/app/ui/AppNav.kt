package com.spendingbuddy.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spendingbuddy.app.*
import com.spendingbuddy.app.p2p.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private const val TODAY = "today"
private const val ACTIVITY = "activity"
private const val BUDDY = "buddy"
private const val SETTINGS = "settings"
private const val LIMITS = "limits"
private const val IMPORT = "import"
private const val ONBOARDING = "onboarding"
private const val INVITE = "invite"
private const val JOIN = "join"

@Composable
fun SpendingBuddyApp() {
    val context = LocalContext.current
    val activity = context as? com.spendingbuddy.app.MainActivity
    val repo = remember { BuddyRepository.get(context) }
    val peerManager = remember { PeerSyncManager.get(context) }
    val state by repo.state.collectAsState()
    val peerStatus by peerManager.status.collectAsState()
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val now = remember { ZonedDateTime.now(INDIA_ZONE) }
    var addOpen by remember { mutableStateOf(false) }
    var finishDate by remember { mutableStateOf<String?>(null) }
    var invite by remember { mutableStateOf<InviteInfo?>(null) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var joinError by remember { mutableStateOf<String?>(null) }
    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    var onboardingName by rememberSaveable { mutableStateOf("") }
    var onboardingDaily by rememberSaveable { mutableStateOf("500") }
    var onboardingWeekly by rememberSaveable { mutableStateOf("3500") }

    LaunchedEffect(Unit) {
        activity?.onUiReady()
        when (activity?.consumeDestination()) {
            Destinations.ACTIVITY -> nav.navigate(ACTIVITY)
            Destinations.BUDDY -> nav.navigate(BUDDY)
            Destinations.SETTINGS -> nav.navigate(SETTINGS)
            Destinations.IMPORT -> nav.navigate(IMPORT)
            Destinations.ADD -> { nav.navigate(TODAY); addOpen = true }
            Destinations.TODAY, null -> Unit
        }
    }
    SpendingBuddyTheme {
        NavHost(navController = nav, startDestination = if (state.onboardingDone) TODAY else ONBOARDING) {
            composable(TODAY) {
                TodayScreen(state, peerStatus, now, { addOpen = true }, { finishDate = it }, { nav.navigate(SETTINGS) }, { nav.navigate(ACTIVITY) }, { nav.navigate(BUDDY) }, { finishDate = now.toLocalDate().minusDays(1).toString() }, {})
                if (addOpen) AddSheet(state, onDismiss = { addOpen = false }, onSave = { expense -> runCatching { repo.saveExpense(expense) }.onSuccess { addOpen = false } }, onPreset = repo::addPreset)
                finishDate?.let { date -> FinishSheet(state, date, { repo.completeDay(date); finishDate = null }, { finishDate = null; addOpen = true }, { finishDate = null }) }
            }
            composable(ACTIVITY) {
                ActivityScreen(state, now, { addOpen = true }, { nav.popBackStack() })
                if (addOpen) AddSheet(state, onDismiss = { addOpen = false }, onSave = { repo.saveExpense(it); addOpen = false }, onPreset = repo::addPreset)
            }
            composable(BUDDY) {
                BuddyScreen(
                    state = state, peer = peerStatus, now = now,
                    onInvite = { scope.launch { peerManager.createInvite().onSuccess { info -> invite = info; qr = activity?.makeQr(info.code); nav.navigate(INVITE) } } },
                    onJoin = { nav.navigate(JOIN) }, onNudge = peerManager::sendNudge, onSuggest = {},
                    onRespond = peerManager::respondToSuggestion, onSync = peerManager::syncNow,
                    onUnlink = peerManager::unlink, onReachable = peerManager::setStayReachable,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(SETTINGS) {
                SettingsScreen(state, { nav.popBackStack() }, repo::updateName, { nav.navigate(LIMITS) }, {}, repo::setAmountInput, repo::setHaptics, repo::setReminders, repo::setFriendAlerts, peerManager::setStayReachable, { activity?.exportBackup() }, { activity?.restoreBackup() }, { activity?.openBatterySettings() })
            }
            composable(LIMITS) { LimitsScreen(state, { nav.popBackStack() }) { daily, weekly -> repo.setBudgets(daily, weekly); nav.popBackStack() } }
            composable(IMPORT) {
                ImportScreen(state, { nav.popBackStack() }, { activity?.pickMoreScreenshots() }, repo::retryImportImage, repo::deleteImportImage, repo::updateCandidate, { repo.saveImport(); nav.popBackStack() }, { repo.discardImport(); nav.popBackStack() })
            }
            composable(INVITE) { InviteScreen(invite, qr, { peerManager.cancelInvite(); nav.popBackStack() }, { activity?.shareInvite(invite?.code.orEmpty()) }, { peerManager.cancelInvite(); nav.popBackStack() }) }
            composable(JOIN) {
                JoinScreen(
                    onBack = { nav.popBackStack() },
                    onScan = { activity?.scanInvite { code -> scope.launch { peerManager.joinWithCode(code).onSuccess { nav.popBackStack(); nav.popBackStack() }.onFailure { joinError = it.message } } } },
                    onJoin = { code -> scope.launch { peerManager.joinWithCode(code).onSuccess { nav.popBackStack(); nav.popBackStack() }.onFailure { joinError = it.message } } }
                )
                joinError?.let { ErrorText(it) }
            }
            composable(ONBOARDING) {
                OnboardingScreen(
                    onboardingStep, onboardingName, onboardingDaily, onboardingWeekly,
                    { onboardingName = it }, { onboardingDaily = it }, { onboardingWeekly = it },
                    onNext = { if (onboardingStep < 2) onboardingStep += 1 },
                    onSkip = { completeOnboarding(repo, onboardingName, onboardingDaily, onboardingWeekly); nav.navigate(TODAY) { popUpTo(ONBOARDING) { inclusive = true } } },
                    onReminders = { completeOnboarding(repo, onboardingName, onboardingDaily, onboardingWeekly); activity?.requestNotifications(); nav.navigate(TODAY) { popUpTo(ONBOARDING) { inclusive = true } } }
                )
            }
        }
    }
}

private fun completeOnboarding(repo: BuddyRepository, name: String, daily: String, weekly: String) {
    val safeName = if (name.isBlank()) "You" else name
    repo.completeOnboarding(safeName, (daily.toLongOrNull() ?: 500L) * 100L, (weekly.toLongOrNull() ?: 3500L) * 100L)
}
