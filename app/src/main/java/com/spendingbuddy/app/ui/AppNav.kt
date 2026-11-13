package com.spendingbuddy.app.ui

import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.IMPULSE
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.spendingbuddy.app.ThemeStore
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.Destinations
import com.spendingbuddy.app.Expense
import com.spendingbuddy.app.INDIA_ZONE
import com.spendingbuddy.app.MainActivity
import com.spendingbuddy.app.Visibility
import com.spendingbuddy.app.p2p.PeerStatus
import com.spendingbuddy.app.p2p.PeerSyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private const val TODAY = "today"
private const val ACTIVITY = "activity"
private const val CIRCLE = "circle"
private const val CIRCLE_CREATE = "circle/create"
private const val CIRCLE_INVITE = "circle/invite"
private const val CIRCLE_JOIN = "circle/join"
private const val CIRCLE_SETTINGS = "circle/settings"
private const val SETTINGS = "settings"
private const val PRESETS = "presets"
private const val LIMITS = "limits"
private const val IMPORT = "import"
private const val ONBOARDING = "onboarding"

private class TopLevel(val route: String, val label: String, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(TODAY, "Today", PennyIcons.Today),
    TopLevel(ACTIVITY, "Activity", PennyIcons.Activity),
    TopLevel(CIRCLE, "Circle", PennyIcons.Circle)
)

/** The one place that reads per-member reachability from the manager's status. */
private fun reachableIds(status: PeerStatus): Set<String> =
    status.reachable.filterValues { it }.keys

@Composable
fun SpendingBuddyApp() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val repo = remember { BuddyRepository.get(context) }
    val peerManager = remember { PeerSyncManager.get(context) }
    val state by repo.state.collectAsState()
    // Keep the synchronous copy used for the first frame (MainActivity.onCreate) in step with app state.
    LaunchedEffect(state.theme) {
        ThemeStore.save(context, state.theme)
        activity?.applySplashTheme(state.theme)
    }
    val peerStatus by peerManager.status.collectAsState()
    val loadProblem by repo.loadProblem.collectAsState()
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val actions = remember { ManagerCircleActions(peerManager, scope, snackbar) { code -> activity?.shareInvite(code) } }
    val now = remember { ZonedDateTime.now(INDIA_ZONE) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var finishDate by rememberSaveable { mutableStateOf<String?>(null) }
    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    var onboardingName by rememberSaveable { mutableStateOf("") }
    var onboardingDaily by rememberSaveable { mutableStateOf("500") }
    var onboardingWeekly by rememberSaveable { mutableStateOf("3500") }

    val saveExpense: (Expense) -> Unit = { expense ->
        val previousCheckIns = state.checkIns.filter { it.date == expense.date }
        val isNew = state.expenses.none { it.id == expense.id }
        try {
            repo.saveExpense(expense)
            addOpen = false
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(
                    "Purchase saved",
                    actionLabel = if (isNew) "Undo" else null,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) repo.undoSave(expense.id, previousCheckIns)
            }
        } catch (e: Exception) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(e.message?.takeIf { it.isNotBlank() } ?: "Couldn't save that purchase. Try again.")
            }
        }
    }

    var openPurchaseId by rememberSaveable { mutableStateOf<String?>(null) }
    val openPurchase: (Expense) -> Unit = { openPurchaseId = it.id }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val bottomBar: @Composable () -> Unit = { TopLevelBar(currentRoute, haptics = state.haptics) { nav.goTopLevel(it) } }

    LaunchedEffect(activity) {
        activity?.onUiReady()
        activity?.destination?.collect { requested ->
            when (requested) {
                Destinations.ACTIVITY -> nav.goTopLevel(ACTIVITY)
                Destinations.CIRCLE, Destinations.BUDDY -> nav.goTopLevel(CIRCLE)
                Destinations.SETTINGS -> nav.navigate(SETTINGS)
                Destinations.IMPORT -> if (nav.currentDestination?.route != IMPORT) nav.navigate(IMPORT)
                Destinations.ADD -> { addOpen = true }
                else -> Unit
            }
            if (requested != null) activity?.clearDestination()
        }
    }
    LaunchedEffect(activity) {
        activity?.messages?.collect { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) }
    }
    LaunchedEffect(peerStatus.lastError) {
        peerStatus.lastError?.let { snackbar.showSnackbar("Sync problem: $it", duration = SnackbarDuration.Short) }
    }

    SpendingBuddyTheme(AppTheme.fromId(state.theme)) {
        loadProblem?.let { reason ->
            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = { Text("Your saved data couldn't be opened") },
                text = {
                    Text(
                        "$reason\n\nA copy of the file was kept on this phone and nothing has been overwritten. " +
                            "Until you choose, new changes are not saved. Starting fresh begins with empty data; restoring loads a backup file you exported earlier."
                    )
                },
                confirmButton = { TextButton(onClick = { repo.acknowledgeLoadProblem() }) { Text("Start fresh") } },
                dismissButton = {
                    TextButton(onClick = { repo.acknowledgeLoadProblem(); activity?.restoreBackup() }) { Text("Restore a backup") }
                }
            )
        }
        state.expenses.find { it.id == openPurchaseId }?.let { expense ->
            val impulse = expense.reason == IMPULSE
            AlertDialog(
                onDismissRequest = { openPurchaseId = null },
                title = { Text(expense.merchant.ifBlank { expense.category }) },
                text = {
                    Text(
                        "${rupees(expense.amountPaise)} · ${expense.category} · ${if (impulse) "Impulse" else expense.reason}\n" +
                            "${expense.date}${if (expense.kind != "expense") " · ${expense.kind}" else ""}\n\n" +
                            "Deleting re-opens that day's check-in. You can undo right after."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val previousCheckIns = state.checkIns.filter { it.date == expense.date }
                        openPurchaseId = null
                        val removed = repo.deleteExpense(expense.id)
                        if (removed != null) scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar("Purchase deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                            if (result == SnackbarResult.ActionPerformed) repo.undoDelete(removed, previousCheckIns)
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { openPurchaseId = null }) { Text("Close") } }
            )
        }
        // One drifting background for the whole app, in its own layer so its animation does not redraw the
        // screens. Screens use transparent scaffolds and inherit the content colour from here.
        Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().graphicsLayer().auroraBackground())
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        NavHost(navController = nav, startDestination = if (state.onboardingDone) TODAY else ONBOARDING) {
            composable(TODAY) {
                TodayScreen(
                    state = state,
                    now = now,
                    bottomBar = bottomBar,
                    onAdd = { addOpen = true },
                    onFinish = { finishDate = it },
                    onSettings = { nav.navigate(SETTINGS) },
                    onFinishYesterday = { finishDate = now.toLocalDate().minusDays(1).toString() },
                    snackbar = snackbar,
                    onPurchase = openPurchase
                )
                if (addOpen) {
                    AddSheet(
                        state,
                        onDismiss = { addOpen = false },
                        onSave = saveExpense,
                        onSavePreset = { name, cat, why -> repo.savePreset(name, cat, why) }
                    )
                }
                finishDate?.let { date ->
                    FinishSheet(
                        state,
                        date,
                        onConfirm = { repo.completeDay(date); finishDate = null },
                        onAdd = { finishDate = null; addOpen = true },
                        onDismiss = { finishDate = null }
                    )
                }
            }
            composable(ACTIVITY) {
                ActivityScreen(state, now, bottomBar, onAdd = { addOpen = true }, snackbar = snackbar, onPurchase = openPurchase)
                if (addOpen) {
                    AddSheet(
                        state,
                        onDismiss = { addOpen = false },
                        onSave = saveExpense,
                        onSavePreset = { name, cat, why -> repo.savePreset(name, cat, why) }
                    )
                }
            }
            composable(CIRCLE) {
                CircleScreen(
                    state = state,
                    joinProgress = peerStatus.joinProgress,
                    reachableIds = reachableIds(peerStatus),
                    actions = actions,
                    snackbar = snackbar,
                    bottomBar = bottomBar,
                    onCreate = { nav.navigate(CIRCLE_CREATE) },
                    onJoin = { nav.navigate(CIRCLE_JOIN) },
                    onInvite = { nav.navigate(CIRCLE_INVITE) },
                    onCircleSettings = { nav.navigate(CIRCLE_SETTINGS) }
                )
            }
            composable(CIRCLE_CREATE) {
                CircleCreateScreen(
                    actions = actions,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onCreated = {
                        nav.navigate(CIRCLE_INVITE) { popUpTo(CIRCLE_CREATE) { inclusive = true } }
                    }
                )
            }
            composable(CIRCLE_INVITE) {
                CircleInviteScreen(state, actions, snackbar, onBack = { nav.popBackStack() }, peerStatus = peerStatus, makeQr = activity?.let { a -> { text -> a.makeQr(text) } })
            }
            composable(CIRCLE_JOIN) {
                CircleJoinScreen(
                    joinProgress = peerStatus.joinProgress,
                    inCircle = state.group != null,
                    actions = actions,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onJoined = { nav.popBackStack(CIRCLE, inclusive = false) },
                    onScan = activity?.let { a -> { onCode -> a.scanInvite(onCode) } }
                )
            }
            composable(CIRCLE_SETTINGS) {
                CircleSettingsScreen(
                    state = state,
                    actions = actions,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onLeft = { nav.popBackStack(CIRCLE, inclusive = false) }
                )
            }
            composable(SETTINGS) {
                SettingsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onName = repo::updateName,
                    onTheme = { repo.setTheme(it) },
                    onLimits = { nav.navigate(LIMITS) },
                    onCircle = if (state.group != null) ({ nav.navigate(CIRCLE_SETTINGS) }) else null,
                    onPresets = { nav.navigate(PRESETS) },
                    onAmountInput = repo::setAmountInput,
                    onHaptics = repo::setHaptics,
                    onReminders = repo::setReminders,
                    onAlerts = repo::setFriendAlerts,
                    onReachable = peerManager::setStayReachable,
                    onExport = { activity?.exportBackup() },
                    onRestore = { activity?.restoreBackup() },
                    onBattery = { activity?.openBatterySettings() },
                    snackbar = snackbar
                )
            }
            composable(PRESETS) {
                PresetsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onSave = { id, name, cat, why -> if (id == null) repo.savePreset(name, cat, why) else repo.updatePreset(id, name, cat, why) },
                    onSetDefault = repo::setDefaultPreset,
                    onDelete = { id ->
                        val removed = repo.deletePreset(id)
                        if (removed != null) scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val r = snackbar.showSnackbar("Deleted \"${removed.name}\"", actionLabel = "Undo", duration = SnackbarDuration.Long)
                            if (r == SnackbarResult.ActionPerformed) repo.restorePreset(removed)
                        }
                    },
                    snackbar = snackbar
                )
            }
            composable(LIMITS) {
                LimitsScreen(state, onBack = { nav.popBackStack() }) { daily, weekly ->
                    repo.setBudgets(daily, weekly)
                    nav.popBackStack()
                }
            }
            composable(IMPORT) {
                ImportScreen(
                    state,
                    onBack = { nav.popBackStack() },
                    onAddMore = { activity?.pickMoreScreenshots() },
                    onRetry = repo::retryImportImage,
                    onRemove = repo::deleteImportImage,
                    onUpdate = repo::updateCandidate,
                    onSave = {
                        val problem = repo.saveImport()
                        if (problem == null) nav.popBackStack() else scope.launch { snackbar.showSnackbar(problem) }
                    },
                    onDiscard = { repo.discardImport(); nav.popBackStack() },
                    snackbar = snackbar
                )
            }
            composable(ONBOARDING) {
                val finish = { nav.navigate(TODAY) { popUpTo(ONBOARDING) { inclusive = true } } }
                OnboardingScreen(
                    step = onboardingStep,
                    theme = state.theme,
                    onTheme = { repo.setTheme(it) },
                    name = onboardingName,
                    daily = onboardingDaily,
                    weekly = onboardingWeekly,
                    onName = { onboardingName = it },
                    onDaily = { onboardingDaily = it },
                    onWeekly = { onboardingWeekly = it },
                    onNext = { if (onboardingStep < 3) onboardingStep += 1 },
                    onSkip = {
                        completeOnboarding(repo, onboardingName, onboardingDaily, onboardingWeekly)
                        finish()
                    },
                    onReminders = {
                        completeOnboarding(repo, onboardingName, onboardingDaily, onboardingWeekly)
                        activity?.requestNotifications()
                        finish()
                    }
                )
            }
        }
        }
        }
    }
}

private fun NavHostController.goTopLevel(route: String) {
    navigate(route) {
        popUpTo(TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Penny Circle tab bar shared by the three top-level destinations, with a sliding indicator. */
@Composable
private fun TopLevelBar(currentRoute: String?, haptics: Boolean = true, onSelect: (String) -> Unit) {
    val tokens = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    val view = LocalView.current
    val primary = MaterialTheme.colorScheme.primary
    val selectedIndex = topLevel.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val hasSelection = topLevel.any { it.route == currentRoute }
    Column(Modifier.background(tokens.bg.copy(alpha = if (tokens.dark) 0.84f else 0.90f)).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.border.copy(alpha = 0.6f)))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val itemWidth = maxWidth / topLevel.size
            val pillWidth = 64.dp
            // The pill slides between tabs with a little overshoot.
            val indicatorX by animateDpAsState(
                targetValue = itemWidth * selectedIndex + (itemWidth - pillWidth) / 2,
                animationSpec = if (reduced) snap() else PennyMotion.spatial(),
                label = "tabIndicator"
            )
            if (hasSelection) {
                Box(
                    Modifier
                        .offset { IntOffset(indicatorX.roundToPx(), 8.dp.roundToPx()) }
                        .size(pillWidth, 32.dp)
                        .background(
                            Brush.horizontalGradient(listOf(tokens.accent.copy(alpha = 0.30f), tokens.accent2.copy(alpha = 0.22f))),
                            CircleShape
                        )
                )
            }
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                topLevel.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val tint by animateColorAsState(
                        if (isSelected) (if (tokens.dark) Color.White else primary) else tokens.muted,
                        animationSpec = if (reduced) snap<Color>() else PennyMotion.effects(),
                        label = "tabTint"
                    )
                    val labelColor by animateColorAsState(
                        if (isSelected) tokens.text else tokens.muted,
                        animationSpec = if (reduced) snap<Color>() else PennyMotion.effects(),
                        label = "tabLabel"
                    )
                    // The chosen icon pops, then settles slightly larger than the others.
                    val iconScale = animateFloatAsState(
                        if (isSelected) 1.1f else 1f,
                        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.38f, stiffness = 480f),
                        label = "tabIcon"
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 64.dp)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = {
                                if (!isSelected) {
                                    onSelect(item.route)
                                    if (haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                }
                            })
                            .padding(top = 8.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                item.icon,
                                null,
                                tint = tint,
                                modifier = Modifier.graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = labelColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun completeOnboarding(repo: BuddyRepository, name: String, daily: String, weekly: String) {
    val safeName = if (name.isBlank()) "You" else name
    repo.completeOnboarding(safeName, (daily.toLongOrNull() ?: 500L) * 100L, (weekly.toLongOrNull() ?: 3500L) * 100L)
}

/**
 * Runs circle actions on the manager. Each key can only run once at a time (double taps are ignored),
 * and a failure becomes a calm snackbar instead of being lost.
 */
@Stable
private class ManagerCircleActions(
    private val manager: PeerSyncManager,
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
    private val shareCode: (String) -> Unit
) : CircleActions {
    private var busy by mutableStateOf(emptySet<String>())

    override fun isBusy(key: String) = key in busy

    override fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    /** [block] may throw, or return a failed [Result]; both are reported. */
    private fun <T> run(key: String, failure: String, onSuccess: (T) -> Unit = {}, block: suspend () -> T) {
        if (key in busy) return
        busy = busy + key
        scope.launch {
            val outcome = try {
                val value = block()
                if (value is Result<*>) value.getOrThrow()
                Result.success(value)
            } catch (e: CancellationException) {
                busy = busy - key
                throw e
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            busy = busy - key
            outcome.fold(
                onSuccess = onSuccess,
                onFailure = { e ->
                    val detail = e.message?.takeIf { it.isNotBlank() }
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(if (detail != null) "$failure $detail" else "$failure Try again in a moment.")
                }
            )
        }
    }

    override fun createCircle(name: String, onDone: () -> Unit) =
        run("create", "Couldn't create the circle.", { onDone() }) { manager.createCircle(name) }

    override fun issueCode(rotate: Boolean, onCode: (String) -> Unit) =
        run("code", "Couldn't make a code.", onCode) { manager.issueJoinCode(rotate = rotate) }

    override fun join(code: String) =
        run("join", "Couldn't send the request.", { }) { manager.joinWithCode(code.trim()) }

    override fun approve(requestId: String) =
        run("approve:$requestId", "Couldn't approve the request.", { notify("Approved. They will see the circle when both phones connect.") }) {
            manager.approveJoin(requestId)
        }

    override fun decline(requestId: String) =
        run("decline:$requestId", "Couldn't decline the request.", { notify("Request declined.") }) {
            manager.declineJoin(requestId)
        }

    override fun leave(successorId: String?, onDone: () -> Unit) =
        run("leave", "Couldn't leave the circle.", { onDone() }) { manager.leaveCircle(successorId) }

    override fun transferAdmin(memberId: String, onDone: () -> Unit) =
        run("transfer", "Couldn't transfer the admin role.", { onDone(); notify("Admin role transferred.") }) {
            manager.transferAdmin(memberId)
        }

    override fun setVisibility(visibility: Visibility) =
        run("visibility", "Couldn't change what you share.", { notify("Sharing updated from now on.") }) {
            manager.setVisibility(visibility)
        }

    override fun nudge(memberId: String, name: String) =
        run("nudge:$memberId", "Couldn't send the nudge.", { notify("Nudge queued. $name gets it when their phone connects.") }) {
            manager.nudge(memberId)
        }

    override fun suggest(targetId: String, dailyPaise: Long, weeklyPaise: Long, name: String, onDone: () -> Unit) =
        run("suggest", "Couldn't send the suggestion.", { onDone(); notify("Suggestion queued. $name decides whether to accept.") }) {
            manager.suggestBudget(targetId, dailyPaise, weeklyPaise)
        }

    override fun respond(suggestionId: String, accepted: Boolean) =
        run("respond:$suggestionId", "Couldn't send your answer.", { notify(if (accepted) "Limits updated." else "Kept your limits.") }) {
            manager.respondToSuggestion(suggestionId, accepted)
        }

    override fun share(code: String) = shareCode(code)
}
