package com.spendingbuddy.app.ui

import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.IMPULSE
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavGraphBuilder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.detect.DetectionNotifications
import com.spendingbuddy.app.detect.PurchaseDetector
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spendingbuddy.app.Destinations
import com.spendingbuddy.app.Expense
import com.spendingbuddy.app.INDIA_ZONE
import com.spendingbuddy.app.MainActivity
import com.spendingbuddy.app.Visibility
import com.spendingbuddy.app.p2p.PeerStatus
import com.spendingbuddy.app.p2p.PeerSyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import java.time.ZonedDateTime
import java.time.LocalDate
import kotlin.math.roundToInt

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
private const val AUTO_DETECT = "autodetect"
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
    val now by produceState(ZonedDateTime.now(INDIA_ZONE)) {
        while (true) { delay(30_000); value = ZonedDateTime.now(INDIA_ZONE) }
    }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var finishDate by rememberSaveable { mutableStateOf<String?>(null) }
    var accessTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { accessTick++; onPauseOrDispose { } }
    val detectionAccess = remember(accessTick) {
        AutoDetectAccess(
            PurchaseDetector.notificationAccessGranted(context), PurchaseDetector.smsGranted(context),
            NotificationManagerCompat.from(context).areNotificationsEnabled(), DetectionNotifications.liveUpdatesAllowed(context)
        )
    }

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
    var editPurchaseId by rememberSaveable { mutableStateOf<String?>(null) }
    val openPurchase: (Expense) -> Unit = { openPurchaseId = it.id }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // One persistent navigation bar: the content moves between tabs while its anchor stays put.
    val bottomBar: @Composable () -> Unit = {}

    LaunchedEffect(activity) {
        activity?.onUiReady()
        activity?.destination?.collect { requested ->
            when (requested) {
                Destinations.ACTIVITY -> nav.goTopLevel(ACTIVITY)
                Destinations.CIRCLE, Destinations.BUDDY -> nav.goTopLevel(CIRCLE)
                Destinations.SETTINGS -> nav.navigate(SETTINGS)
                Destinations.IMPORT -> if (nav.currentDestination?.route != IMPORT) nav.navigate(IMPORT)
                Destinations.ADD -> { addOpen = true }
                Destinations.AUTO_DETECT -> if (nav.currentDestination?.route != AUTO_DETECT) nav.navigate(AUTO_DETECT)
                null -> Unit
                else -> when {
                    requested.startsWith(Destinations.FINISH_PREFIX) -> {
                        val date = requested.removePrefix(Destinations.FINISH_PREFIX)
                        val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                        if (parsed != null && parsed <= LocalDate.now(INDIA_ZONE)) {
                            nav.goTopLevel(TODAY)
                            finishDate = date
                        }
                    }
                    requested.startsWith(Destinations.EDIT_PREFIX) -> editPurchaseId = requested.removePrefix(Destinations.EDIT_PREFIX)
                }
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
        val reducedMotion = LocalReducedMotion.current
        val layoutSign = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
        val topLevelIndex = topLevel.indexOfFirst { it.route == currentRoute }
        val paneSwipeEnabled = topLevelIndex >= 0 && !addOpen && finishDate == null && openPurchaseId == null && editPurchaseId == null
        val edgeBandPx = with(LocalDensity.current) { 32.dp.toPx() }
        val bottomExclusionPx = with(LocalDensity.current) { 96.dp.toPx() }
        val view = LocalView.current
        var paneDragOffset by remember { mutableFloatStateOf(0f) }
        var paneSettleJob by remember { mutableStateOf<Job?>(null) }
        var gestureSourceRoute by remember { mutableStateOf<String?>(null) }
        var gestureTargetRoute by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(currentRoute) {
            if (gestureSourceRoute == null && gestureTargetRoute == null) {
                paneSettleJob?.cancel()
                paneDragOffset = 0f
            }
        }
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
            PurchaseDetailsSheet(
                expense = expense,
                onDismiss = { openPurchaseId = null },
                onEdit = { openPurchaseId = null; editPurchaseId = expense.id },
                onToggleImpulse = { repo.setImpulse(expense.id, expense.reason != IMPULSE); openPurchaseId = null },
                onDelete = {
                        val previousCheckIns = state.checkIns.filter { it.date == expense.date }
                        openPurchaseId = null
                        val removed = repo.deleteExpense(expense.id)
                        if (removed != null) scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar("Purchase deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                            if (result == SnackbarResult.ActionPerformed) repo.undoDelete(removed, previousCheckIns)
                        }
                }
            )
        }
        state.expenses.find { it.id == editPurchaseId }?.let { expense ->
            AddSheet(
                state,
                initial = expense,
                onDismiss = { editPurchaseId = null },
                onSave = { saveExpense(it); editPurchaseId = null },
                onSavePreset = { name, cat, why -> repo.savePreset(name, cat, why) }
            )
        }
        // One drifting background for the whole app, in its own layer so its animation does not redraw the
        // screens. Screens use transparent scaffolds and inherit the content colour from here.
        Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().graphicsLayer().auroraBackground())
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.weight(1f).pointerInput(currentRoute, paneSwipeEnabled, layoutSign, state.haptics, reducedMotion) {
                if (!paneSwipeEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val widthPx = size.width.toFloat()
                    val safeBottom = size.height - bottomExclusionPx
                    if (down.position.x < edgeBandPx || down.position.x > widthPx - edgeBandPx || down.position.y > safeBottom) {
                        return@awaitEachGesture
                    }

                    paneSettleJob?.cancel()
                    paneSettleJob = null
                    var dragX = 0f
                    val dragStarted = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        change.consume()
                        dragX = overSlop
                    }
                    if (dragStarted == null) return@awaitEachGesture
                    gestureSourceRoute = currentRoute
                    if (!reducedMotion) paneDragOffset = resistedPaneOffset(dragX, widthPx)

                    val dragCompleted = horizontalDrag(down.id) { change ->
                        if (!change.isConsumed) {
                            dragX += change.position.x - change.previousPosition.x
                            if (!reducedMotion) paneDragOffset = resistedPaneOffset(dragX, widthPx)
                            change.consume()
                        }
                    }
                    val destinationIndex = if (dragCompleted) {
                        topLevelSwipeTargetIndex(topLevelIndex, dragX, widthPx, layoutSign.toFloat())
                    } else null
                    val destination = destinationIndex?.let { topLevel[it].route }
                    if (destination != null) {
                        gestureTargetRoute = destination
                        nav.goTopLevel(destination)
                        if (state.haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    }
                    paneSettleJob = scope.launch {
                        val animation = Animatable(paneDragOffset)
                        val settleTarget = if (destination == null || reducedMotion) 0f else kotlin.math.sign(dragX) * widthPx
                        if (reducedMotion) animation.snapTo(0f)
                        else if (destination != null) {
                            animation.animateTo(settleTarget, tween(300, easing = FastOutSlowInEasing)) { paneDragOffset = value }
                        } else animation.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f)) { paneDragOffset = value }
                        paneDragOffset = 0f
                        gestureSourceRoute = null
                        gestureTargetRoute = null
                    }
                }
            }
        ) {
        NavHost(
            navController = nav,
            startDestination = if (state.onboardingDone) TODAY else ONBOARDING,
            enterTransition = {
                PaneMotion.enter(PaneMotion.direction(initialState.destination.route, targetState.destination.route) * layoutSign, reducedMotion)
            },
            exitTransition = {
                if (gestureTargetRoute != null && gestureSourceRoute == initialState.destination.route) ExitTransition.None
                else PaneMotion.exit(PaneMotion.direction(initialState.destination.route, targetState.destination.route) * layoutSign, reducedMotion)
            },
            popEnterTransition = {
                if (gestureTargetRoute == targetState.destination.route || gestureTargetRoute == initialState.destination.route) EnterTransition.None
                else PaneMotion.popEnter(PaneMotion.direction(initialState.destination.route, targetState.destination.route) * layoutSign, reducedMotion)
            },
            popExitTransition = {
                if (gestureTargetRoute == targetState.destination.route || gestureTargetRoute == initialState.destination.route) ExitTransition.None
                else PaneMotion.popExit(PaneMotion.direction(initialState.destination.route, targetState.destination.route) * layoutSign, reducedMotion)
            },
            modifier = Modifier
        ) {
            pane(TODAY, Modifier.offset { IntOffset(if (gestureSourceRoute == TODAY) paneDragOffset.roundToInt() else 0, 0) }) {
                TodayScreen(
                    state = state,
                    now = now,
                    bottomBar = bottomBar,
                    onAdd = { addOpen = true },
                    onFinish = { finishDate = it },
                    onFinishNow = { repo.completeDay(it) },
                    onSettings = { nav.navigate(SETTINGS) },
                    onFinishYesterday = { finishDate = now.toLocalDate().minusDays(1).toString() },
                    snackbar = snackbar,
                    onPurchase = openPurchase,
                    autoAccessReady = detectionAccess.sms || detectionAccess.notificationAccess,
                    auto = AutoActions(
                        setUp = { nav.navigate(AUTO_DETECT) },
                        notNow = { repo.dismissAutoDetectOffer() },
                        undo = { id -> repo.undoAuto(id); DetectionNotifications.cancelAdded(context, id) },
                        keep = { id -> repo.keepAuto(id); DetectionNotifications.cancelAdded(context, id) },
                        addPending = { id -> repo.addPending(id); DetectionNotifications.cancelAsk(context, id) },
                        skipPending = { id -> repo.skipPending(id); DetectionNotifications.cancelAsk(context, id) }
                    )
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
            pane(ACTIVITY, Modifier.offset { IntOffset(if (gestureSourceRoute == ACTIVITY) paneDragOffset.roundToInt() else 0, 0) }) {
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
            pane(CIRCLE, Modifier.offset { IntOffset(if (gestureSourceRoute == CIRCLE) paneDragOffset.roundToInt() else 0, 0) }) {
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
            pane(CIRCLE_CREATE) {
                CircleCreateScreen(
                    actions = actions,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onCreated = {
                        nav.navigate(CIRCLE_INVITE) { popUpTo(CIRCLE_CREATE) { inclusive = true } }
                    }
                )
            }
            pane(CIRCLE_INVITE) {
                CircleInviteScreen(state, actions, snackbar, onBack = { nav.popBackStack() }, peerStatus = peerStatus, makeQr = activity?.let { a -> { text -> a.makeQr(text) } })
            }
            pane(CIRCLE_JOIN) {
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
            pane(CIRCLE_SETTINGS) {
                CircleSettingsScreen(
                    state = state,
                    actions = actions,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onLeft = { nav.popBackStack(CIRCLE, inclusive = false) }
                )
            }
            pane(SETTINGS) {
                SettingsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onName = repo::updateName,
                    onTheme = { repo.setTheme(it) },
                    onLimits = { nav.navigate(LIMITS) },
                    onAutoDetect = { nav.navigate(AUTO_DETECT) },
                    onCircle = if (state.group != null) ({ nav.navigate(CIRCLE_SETTINGS) }) else null,
                    onPresets = { nav.navigate(PRESETS) },
                    onAmountInput = repo::setAmountInput,
                    onHaptics = repo::setHaptics,
                    onReminders = repo::setReminders,
                    onFinishCountdown = repo::setFinishCountdown,
                    onPaymentUpdateMinutes = repo::setPaymentUpdateMinutes,
                    onFinishCountdownLeadMinutes = repo::setFinishCountdownLeadMinutes,
                    onAlerts = repo::setFriendAlerts,
                    onReachable = peerManager::setStayReachable,
                    onExport = { activity?.exportBackup() },
                    onRestore = { activity?.restoreBackup() },
                    onBattery = { activity?.openBatterySettings() },
                    snackbar = snackbar
                )
            }
            pane(PRESETS) {
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
            pane(AUTO_DETECT) {
                val access = detectionAccess
                AutoDetectScreen(
                    state = state,
                    access = access,
                    onBack = { nav.popBackStack() },
                    onEnabled = repo::setAutoDetect,
                    onNotificationAccess = { activity?.openNotificationAccess() },
                    onSms = { if (access.sms) activity?.openAppSettings() else activity?.requestSms { accessTick++ } },
                    onNotifications = { if (access.canNotify) activity?.openLiveUpdateSettings() else activity?.requestNotifications { accessTick++ } },
                    onLiveUpdates = { activity?.openLiveUpdateSettings() },
                    onSaveRule = repo::saveMerchantRule,
                    onDeleteRule = repo::removeMerchantRule,
                    snackbar = snackbar
                )
            }
            pane(LIMITS) {
                LimitsScreen(state, onBack = { nav.popBackStack() }) { daily, weekly ->
                    repo.setBudgets(daily, weekly)
                    nav.popBackStack()
                }
            }
            pane(IMPORT) {
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
            pane(ONBOARDING) {
                val finish = { nav.navigate(TODAY) { popUpTo(ONBOARDING) { inclusive = true } } }
                OnboardingFlow(
                    state = state,
                    access = detectionAccess,
                    onTheme = { repo.setTheme(it) },
                    onNotificationAccess = { activity?.openNotificationAccess() },
                    onSms = { activity?.requestSms { accessTick++ } },
                    onNotifications = { activity?.requestNotifications { accessTick++ } },
                    onComplete = { name, daily, weekly, automatic, reminders ->
                        repo.completeOnboarding(name, daily, weekly)
                        repo.setAutoDetect(automatic)
                        repo.setReminders(reminders)
                        finish()
                    }
                )
            }
        }
        }
        AnimatedVisibility(
            visible = topLevel.any { it.route == currentRoute },
            enter = if (reducedMotion) EnterTransition.None else slideInVertically(tween(220)) { it } + expandVertically(tween(220)),
            exit = if (reducedMotion) ExitTransition.None else slideOutVertically(tween(220)) { it } + shrinkVertically(tween(220))
        ) {
            TopLevelBar(currentRoute, haptics = state.haptics) {
                paneSettleJob?.cancel()
                paneDragOffset = 0f
                gestureSourceRoute = null
                gestureTargetRoute = null
                nav.goTopLevel(it)
            }
        }
        }
        }
        }
    }
}

/** Each moving pane paints its own opaque canvas so text never overlaps the outgoing pane. */
private fun NavGraphBuilder.pane(route: String, contentModifier: Modifier = Modifier, content: @Composable () -> Unit) {
    composable(route) {
        Box(Modifier.fillMaxSize().then(contentModifier).auroraBackground(animate = false)) { content() }
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
    val largeText = LocalDensity.current.fontScale >= 1.4f
    val selectedIndex = topLevel.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val hasSelection = topLevel.any { it.route == currentRoute }
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 5.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val itemWidth = maxWidth / topLevel.size
            val rowHeight = if (largeText) 96.dp else 68.dp
            val indicatorX by animateDpAsState(
                targetValue = itemWidth * selectedIndex + 4.dp,
                animationSpec = if (reduced) snap() else spring(dampingRatio = 0.84f, stiffness = 520f),
                label = "tabIndicator"
            )
            Box(
                Modifier.fillMaxWidth().height(rowHeight).glass(shape)
            ) {
            if (hasSelection) Box(
                Modifier.align(Alignment.CenterStart)
                    .offset(x = indicatorX, y = 0.dp)
                    .padding(vertical = 4.dp)
                    .width(itemWidth - 8.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(tokens.accent.copy(alpha = if (tokens.dark) 0.18f else 0.10f))
                    .border(1.dp, tokens.accent.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            )
            Row(Modifier.fillMaxSize().selectableGroup()) {
                topLevel.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val tint by animateColorAsState(
                        if (isSelected) tokens.accent else tokens.muted,
                        animationSpec = if (reduced) snap<Color>() else PennyMotion.effects(),
                        label = "tabTint"
                    )
                    val labelColor by animateColorAsState(
                        if (isSelected) tokens.text else tokens.muted,
                        animationSpec = if (reduced) snap<Color>() else PennyMotion.effects(),
                        label = "tabLabel"
                    )
                    val iconScale = animateFloatAsState(
                        if (isSelected) 1.04f else 1f,
                        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.76f, stiffness = 560f),
                        label = "tabIcon"
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = {
                                if (!isSelected) {
                                    onSelect(item.route)
                                    if (haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                }
                            })
                            .padding(horizontal = if (largeText) 2.dp else 4.dp, vertical = if (largeText) 8.dp else 6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.height(if (largeText) 30.dp else 26.dp), contentAlignment = Alignment.Center) {
                            Icon(item.icon, null, tint = tint, modifier = Modifier.size(if (largeText) 24.dp else 22.dp).graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value })
                        }
                        Spacer(Modifier.height(if (largeText) 4.dp else 3.dp))
                        Text(
                            item.label,
                            style = if (largeText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                            color = labelColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = if (largeText) 2 else 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            }
        }
    }
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
