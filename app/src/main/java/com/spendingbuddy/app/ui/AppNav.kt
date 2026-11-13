package com.spendingbuddy.app.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.spendingbuddy.app.R
import com.spendingbuddy.app.RecoveryKind
import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.IMPULSE
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.rememberUpdatedState
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
import com.spendingbuddy.app.notificationTopLevelRoute
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

private class TopLevel(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(TODAY, R.string.shell_today, PennyIcons.Today),
    TopLevel(ACTIVITY, R.string.shell_activity, PennyIcons.Activity),
    TopLevel(CIRCLE, R.string.shell_circle, PennyIcons.Circle)
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
    val recoveryOperation by repo.recoveryOperation.collectAsState()
    val restoreInProgress = recoveryOperation?.kind == RecoveryKind.Restore
    val recoveryInProgress = recoveryOperation != null
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val actions = remember { ManagerCircleActions(context.applicationContext, peerManager, scope, snackbar) { code -> activity?.shareInvite(code) } }
    val now by produceState(ZonedDateTime.now(INDIA_ZONE)) {
        while (true) { delay(30_000); value = ZonedDateTime.now(INDIA_ZONE) }
    }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var finishDate by rememberSaveable { mutableStateOf<String?>(null) }
    var dailyLimitDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var weeklyLimitDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var circleCreateDraft by rememberSaveable { mutableStateOf("") }
    var circleJoinDraft by rememberSaveable { mutableStateOf("") }
    val purchaseDrafts = rememberSaveableStateHolder()
    val localActionMutex = remember { Mutex() }
    var lastLocalFailure by remember { mutableStateOf<String?>(null) }
    var localBusyKeys by remember { mutableStateOf(emptySet<String>()) }
    var loadRecoveryError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmFreshStart by rememberSaveable { mutableStateOf(false) }
    var recoveryBusy by remember { mutableStateOf(false) }
    var recoveryJob by remember { mutableStateOf<Job?>(null) }
    fun safeLocalAction(key: String? = null, onSuccess: () -> Unit = {}, operation: suspend () -> Unit) {
        if (key != null && key in localBusyKeys) return
        if (key != null) localBusyKeys = localBusyKeys + key
        scope.launch {
            var ownsKey = key != null
            try {
                // One queue preserves name keystroke order and preference changes across IO dispatches.
                val problem = localActionMutex.withLock {
                    localOperationError(context.getString(R.string.shell_settings_save_error)) { operation(); null }
                }
                // Snackbar lifetime must not keep a failed command locked against an immediate retry.
                if (key != null) localBusyKeys = localBusyKeys - key
                ownsKey = false
                if (problem == null) { lastLocalFailure = null; onSuccess() }
                else if (lastLocalFailure != problem || snackbar.currentSnackbarData == null) {
                    lastLocalFailure = problem
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(problem)
                }
            } finally {
                if (ownsKey && key != null) localBusyKeys = localBusyKeys - key
            }
        }
    }
    var accessTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { accessTick++; onPauseOrDispose { } }
    val detectionAccess = remember(accessTick) {
        AutoDetectAccess(
            PurchaseDetector.notificationAccessGranted(context), PurchaseDetector.smsGranted(context),
            NotificationManagerCompat.from(context).areNotificationsEnabled(), DetectionNotifications.liveUpdatesAllowed(context)
        )
    }

    val saveExpense: suspend (Expense) -> String? = { expense ->
        val before = repo.state.value
        val previousCheckIns = before.checkIns.filter { it.date == expense.date }
        val isNew = before.expenses.none { it.id == expense.id }
        val problem = localOperationError(context.getString(R.string.shell_save_purchase_error)) { repo.saveExpense(expense); null }
        if (problem == null) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(
                    context.getString(R.string.shell_purchase_saved),
                    actionLabel = if (isNew) context.getString(R.string.shell_undo) else null,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    localOperationError(context.getString(R.string.shell_undo_purchase_error)) { repo.undoSave(expense.id, previousCheckIns); null }
                        ?.let { snackbar.showSnackbar(it) }
                }
            }
        }
        problem
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
            notificationTopLevelRoute(requested)?.let { nav.goTopLevel(it) }
            when (requested) {
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
        activity?.messages?.collect {
            if (repo.loadProblem.value != null) loadRecoveryError = it
            else snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(peerStatus.lastError) {
        peerStatus.lastError?.let { snackbar.showSnackbar(context.getString(R.string.shell_sync_error, it), duration = SnackbarDuration.Short) }
    }
    LaunchedEffect(recoveryOperation) {
        if (restoreInProgress && repo.loadProblem.value == null) {
            val progress = context.getString(R.string.backup_restoring)
            snackbar.currentSnackbarData?.dismiss()
            try { snackbar.showSnackbar(progress, duration = SnackbarDuration.Indefinite) }
            finally { if (snackbar.currentSnackbarData?.visuals?.message == progress) snackbar.currentSnackbarData?.dismiss() }
        }
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
        var paneWidthPx by remember { mutableFloatStateOf(0f) }
        var paneGestureGeneration by remember { mutableIntStateOf(0) }
        LaunchedEffect(reducedMotion, layoutSign) {
            paneSettleJob?.cancel()
            paneDragOffset = 0f
            gestureSourceRoute = null
            gestureTargetRoute = null
        }
        LaunchedEffect(currentRoute, paneSwipeEnabled) {
            if (!paneSwipeEnabled || (gestureSourceRoute != null && currentRoute != gestureSourceRoute)) {
                // Keep the full-width preview until the new NavHost entry has composed its first frame.
                if (currentRoute == gestureTargetRoute) withFrameNanos { }
                paneSettleJob?.cancel()
                paneDragOffset = 0f
                gestureSourceRoute = null
                gestureTargetRoute = null
            }
        }
        loadProblem?.let { reason ->
            val closeSafely: () -> Unit = { recoveryJob?.cancel(); activity?.closeForRecovery() }
            if (!confirmFreshStart) {
            AlertDialog(
                onDismissRequest = closeSafely,
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
                title = { Text(context.getString(R.string.shell_load_problem_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.shell_load_problem_body, reason))
                        if (restoreInProgress) Text(stringResource(R.string.backup_restoring), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        ErrorText(loadRecoveryError)
                        TextButton(
                            enabled = !recoveryInProgress,
                            onClick = { if (repo.recoveryOperation.value == null) { loadRecoveryError = null; confirmFreshStart = true } },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text(stringResource(R.string.shell_start_fresh)) }
                    }
                },
                confirmButton = { TextButton(enabled = !recoveryInProgress, onClick = { if (repo.recoveryOperation.value == null) { loadRecoveryError = null; activity?.restoreBackup() } }) { Text(context.getString(R.string.shell_restore_backup)) } },
                dismissButton = {
                    TextButton(onClick = closeSafely) { Text(stringResource(R.string.shell_close_app)) }
                }
            )
            } else {
                AlertDialog(
                    onDismissRequest = { if (recoveryBusy || repo.recoveryOperation.value != null) closeSafely() else confirmFreshStart = false },
                    title = { Text(stringResource(R.string.shell_start_fresh_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.shell_start_fresh_body))
                            if (restoreInProgress) Text(stringResource(R.string.backup_restoring), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            ErrorText(loadRecoveryError)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !recoveryBusy && !recoveryInProgress,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                if (!recoveryBusy) {
                                    val operation = repo.beginFreshStart() ?: return@TextButton
                                    recoveryBusy = true
                                    loadRecoveryError = null
                                    val job = scope.launch {
                                    try {
                                        loadRecoveryError = localOperationError(context.getString(R.string.shell_start_fresh_error)) { repo.startFreshAfterLoadProblem(operation); null }
                                        if (loadRecoveryError == null) confirmFreshStart = false
                                    } finally { recoveryBusy = false }
                                    }
                                    recoveryJob = job
                                    job.invokeOnCompletion { repo.finishRecovery(operation) }
                                }
                            }
                        ) { Text(stringResource(if (recoveryBusy) R.string.shell_starting_fresh else R.string.shell_start_fresh)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { if (recoveryBusy || repo.recoveryOperation.value != null) closeSafely() else confirmFreshStart = false }) {
                            Text(stringResource(if (recoveryBusy || recoveryInProgress) R.string.shell_close_app else R.string.shell_keep_existing_file))
                        }
                    }
                )
            }
        }
        state.expenses.find { it.id == openPurchaseId }?.let { expense ->
            PurchaseDetailsSheet(
                expense = expense,
                onDismiss = { openPurchaseId = null },
                onEdit = { openPurchaseId = null; editPurchaseId = expense.id },
                onToggleImpulse = { impulse ->
                    localOperationError(context.getString(R.string.shell_update_purchase_error)) { repo.setImpulse(expense.id, impulse); null }
                },
                onDelete = {
                        val previousCheckIns = repo.state.value.checkIns.filter { it.date == expense.date }
                        var removed: Expense? = null
                        val problem = localOperationError(context.getString(R.string.shell_delete_purchase_error)) { removed = repo.deleteExpense(expense.id); null }
                        val deleted = removed
                        if (problem == null) openPurchaseId = null
                        if (problem == null && deleted != null) scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar(context.getString(R.string.shell_purchase_deleted), actionLabel = context.getString(R.string.shell_undo), duration = SnackbarDuration.Long)
                            if (result == SnackbarResult.ActionPerformed) {
                                localOperationError(context.getString(R.string.shell_restore_purchase_error)) { repo.undoDelete(deleted, previousCheckIns); null }
                                    ?.let { snackbar.showSnackbar(it) }
                            }
                        }
                        problem
                }
            )
        }
        state.expenses.find { it.id == editPurchaseId }?.let { expense ->
            val draftKey = "purchase:edit:${expense.id}"
            purchaseDrafts.SaveableStateProvider(draftKey) {
            AddSheet(
                state,
                initial = expense,
                onDismiss = { editPurchaseId = null },
                onSave = { saveExpense(it).also { problem -> if (problem == null) purchaseDrafts.removeState(draftKey) } },
                onSavePreset = { name, cat, why -> localOperationError(context.getString(R.string.shell_save_preset_error)) { repo.savePreset(name, cat, why) } }
            )
            }
        }
        if (addOpen) {
            purchaseDrafts.SaveableStateProvider("purchase:add") {
                AddSheet(
                    state,
                    onDismiss = { addOpen = false },
                    onSave = { saveExpense(it).also { problem -> if (problem == null) purchaseDrafts.removeState("purchase:add") } },
                    onSavePreset = { name, cat, why -> localOperationError(context.getString(R.string.shell_save_preset_error)) { repo.savePreset(name, cat, why) } }
                )
            }
        }
        finishDate?.let { date ->
            FinishSheet(
                state,
                date,
                onConfirm = { context.finishDayMessage(localOperationError(context.getString(R.string.shell_finish_day_error)) { repo.completeDay(date) }) },
                onAdd = { finishDate = null; addOpen = true },
                onDismiss = { finishDate = null }
            )
        }
        // Preview and committed panes use the same screens and callbacks; no data work starts here.
        val topLevelContent: @Composable (String) -> Unit = { route ->
            when (route) {
                TODAY -> {
                TodayScreen(
                    state = state,
                    now = now,
                    bottomBar = bottomBar,
                    onAdd = { addOpen = true },
                    onFinish = { finishDate = it },
                    onFinishNow = { date ->
                        scope.launch {
                            context.finishDayMessage(localOperationError(context.getString(R.string.shell_finish_day_error)) { repo.completeDay(date) })
                                ?.let { snackbar.showSnackbar(it) }
                        }
                    },
                    onSettings = { nav.navigate(SETTINGS) },
                    onFinishYesterday = { finishDate = now.toLocalDate().minusDays(1).toString() },
                    snackbar = snackbar,
                    onPurchase = openPurchase,
                    autoAccessReady = detectionAccess.sms || detectionAccess.notificationAccess,
                    auto = AutoActions(
                        setUp = { nav.navigate(AUTO_DETECT) },
                        notNow = { safeLocalAction { repo.dismissAutoDetectOffer() } },
                        undo = { id -> safeLocalAction { repo.undoAuto(id); DetectionNotifications.cancelAdded(context, id) } },
                        keep = { id -> safeLocalAction { repo.keepAuto(id); DetectionNotifications.cancelAdded(context, id) } },
                        addPending = { id -> safeLocalAction { repo.addPending(id); DetectionNotifications.cancelAsk(context, id) } },
                        skipPending = { id -> safeLocalAction { repo.skipPending(id); DetectionNotifications.cancelAsk(context, id) } }
                    )
                )
                }
                ACTIVITY -> {
                ActivityScreen(state, now, bottomBar, onAdd = { addOpen = true }, snackbar = snackbar, onPurchase = openPurchase)
                }
                CIRCLE -> {
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
                    onCircleSettings = { nav.navigate(CIRCLE_SETTINGS) },
                    memberLinks = peerStatus.members
                )
                }
                else -> Unit
            }
        }
        val latestTopLevelContent by rememberUpdatedState(topLevelContent)
        // This owner outlives every NavHost entry. Previews and taps must restore the same per-tab
        // values even when the destination has not yet entered the navigation back stack.
        val topLevelStateHolder = rememberSaveableStateHolder()
        val topLevelPanes = remember {
            topLevel.associate { item ->
                item.route to movableContentOf {
                    topLevelStateHolder.SaveableStateProvider(item.route) { latestTopLevelContent(item.route) }
                }
            }
        }
        // One drifting background for the whole app, in its own layer so its animation does not redraw the
        // screens. Screens use transparent scaffolds and inherit the content colour from here.
        Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().graphicsLayer().auroraBackground())
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.weight(1f).clipToBounds().onSizeChanged { measured ->
                if (paneWidthPx != measured.width.toFloat()) {
                    paneSettleJob?.cancel()
                    paneDragOffset = 0f
                    gestureSourceRoute = null
                    gestureTargetRoute = null
                    paneWidthPx = measured.width.toFloat()
                }
            }.pointerInput(currentRoute, paneSwipeEnabled, layoutSign, state.haptics, reducedMotion, paneWidthPx, paneGestureGeneration) {
                if (!paneSwipeEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // One gesture owns a complete settle. A second down must not strand its offset.
                    if (gestureSourceRoute != null) return@awaitEachGesture
                    val widthPx = size.width.toFloat()
                    val safeBottom = size.height - bottomExclusionPx
                    if (down.position.x < edgeBandPx || down.position.x > widthPx - edgeBandPx || down.position.y > safeBottom) {
                        return@awaitEachGesture
                    }

                    var dragX = 0f
                    val dragStarted = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        change.consume()
                        dragX = overSlop
                    }
                    if (dragStarted == null) return@awaitEachGesture
                    gestureSourceRoute = currentRoute
                    paneWidthPx = widthPx
                    fun updatePreview() {
                        val neighbor = topLevelSwipePreviewIndex(topLevelIndex, dragX, layoutSign.toFloat())
                        gestureTargetRoute = neighbor?.let { topLevel[it].route }
                        paneDragOffset = if (reducedMotion || neighbor == null) 0f else resistedPaneOffset(dragX, widthPx)
                    }
                    var handedToSettle = false
                    try {
                    updatePreview()
                    val dragCompleted = horizontalDrag(down.id) { change ->
                        if (!change.isConsumed) {
                            dragX += change.position.x - change.previousPosition.x
                            updatePreview()
                            change.consume()
                        }
                    }
                    val destinationIndex = if (dragCompleted) {
                        topLevelSwipeTargetIndex(topLevelIndex, dragX, widthPx, layoutSign.toFloat())
                    } else null
                    val destination = destinationIndex?.let { topLevel[it].route }
                    handedToSettle = true
                    paneSettleJob = scope.launch {
                        var committed = false
                        try {
                        val animation = Animatable(paneDragOffset)
                        val settleTarget = if (destination == null || reducedMotion) 0f else kotlin.math.sign(dragX) * widthPx
                        if (reducedMotion) animation.snapTo(0f)
                        else if (destination != null) {
                            animation.animateTo(settleTarget, tween(300, easing = FastOutSlowInEasing)) { paneDragOffset = value }
                        } else animation.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f)) { paneDragOffset = value }
                        if (destination != null) {
                            gestureTargetRoute = destination
                            committed = true
                            nav.goTopLevel(destination)
                            if (state.haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        }
                        } finally {
                            // A committed handoff is cleared after its NavHost entry composes; every other
                            // completion/cancellation restores the source immediately.
                            if (!committed) {
                                paneDragOffset = 0f
                                gestureSourceRoute = null
                                gestureTargetRoute = null
                            }
                        }
                    }
                    } finally {
                        if (!handedToSettle) {
                            paneDragOffset = 0f
                            gestureSourceRoute = null
                            gestureTargetRoute = null
                        }
                    }
                }
            }
        ) {
        NavHost(
            navController = nav,
            startDestination = if (state.onboardingDone) TODAY else ONBOARDING,
            enterTransition = {
                if (gestureSourceRoute == initialState.destination.route && gestureTargetRoute == targetState.destination.route) EnterTransition.None
                else PaneMotion.enter(PaneMotion.direction(initialState.destination.route, targetState.destination.route) * layoutSign, reducedMotion)
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
            pane(TODAY, Modifier.absoluteOffset { IntOffset(if (gestureSourceRoute == TODAY) paneDragOffset.roundToInt() else 0, 0) }) {
                if (gestureTargetRoute != TODAY || reducedMotion) topLevelPanes.getValue(TODAY)()
            }
            pane(ACTIVITY, Modifier.absoluteOffset { IntOffset(if (gestureSourceRoute == ACTIVITY) paneDragOffset.roundToInt() else 0, 0) }) {
                if (gestureTargetRoute != ACTIVITY || reducedMotion) topLevelPanes.getValue(ACTIVITY)()
            }
            pane(CIRCLE, Modifier.absoluteOffset { IntOffset(if (gestureSourceRoute == CIRCLE) paneDragOffset.roundToInt() else 0, 0) }) {
                if (gestureTargetRoute != CIRCLE || reducedMotion) topLevelPanes.getValue(CIRCLE)()
            }
            pane(CIRCLE_CREATE) {
                CircleCreateScreen(
                    actions = actions,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onCreated = {
                        circleCreateDraft = ""
                        if (nav.currentDestination?.route == CIRCLE_CREATE) {
                            nav.navigate(CIRCLE_INVITE) { popUpTo(CIRCLE_CREATE) { inclusive = true } }
                        }
                    },
                    draftName = circleCreateDraft,
                    onDraftNameChange = { circleCreateDraft = it }
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
                    onJoined = { circleJoinDraft = ""; if (nav.currentDestination?.route == CIRCLE_JOIN) nav.popBackStack(CIRCLE, inclusive = false) },
                    onScan = activity?.let { a -> { onCode -> a.scanInvite(onCode) } },
                    draftCode = circleJoinDraft,
                    onDraftCodeChange = { circleJoinDraft = it }
                )
            }
            pane(CIRCLE_SETTINGS) {
                CircleSettingsScreen(
                    state = state,
                    actions = actions,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onLeft = { if (nav.currentDestination?.route == CIRCLE_SETTINGS) nav.popBackStack(CIRCLE, inclusive = false) }
                )
            }
            pane(SETTINGS) {
                SettingsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onName = { name -> safeLocalAction { repo.updateName(name) } },
                    onTheme = { theme -> safeLocalAction { repo.setTheme(theme) } },
                    onLimits = { nav.navigate(LIMITS) },
                    onAutoDetect = { nav.navigate(AUTO_DETECT) },
                    onCircle = if (state.group != null) ({ nav.navigate(CIRCLE_SETTINGS) }) else null,
                    onPresets = { nav.navigate(PRESETS) },
                    onAmountInput = { mode -> safeLocalAction { repo.setAmountInput(mode) } },
                    onHaptics = { enabled -> safeLocalAction { repo.setHaptics(enabled) } },
                    onReminders = { enabled -> safeLocalAction { repo.setReminders(enabled) } },
                    onFinishCountdown = { enabled -> safeLocalAction { repo.setFinishCountdown(enabled) } },
                    onPaymentUpdateMinutes = { minutes -> safeLocalAction { repo.setPaymentUpdateMinutes(minutes) } },
                    onFinishCountdownLeadMinutes = { minutes -> safeLocalAction { repo.setFinishCountdownLeadMinutes(minutes) } },
                    onAlerts = { enabled -> safeLocalAction { repo.setFriendAlerts(enabled) } },
                    onReachable = { enabled -> safeLocalAction { peerManager.setStayReachableAwait(enabled) } },
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
                    onSave = { id, name, cat, why -> localOperationError(context.getString(R.string.shell_save_preset_error)) { if (id == null) repo.savePreset(name, cat, why) else repo.updatePreset(id, name, cat, why) } },
                    onSetDefault = { id -> localOperationError(context.getString(R.string.shell_default_preset_error)) { repo.setDefaultPreset(id); null } },
                    onDelete = { id -> localOperationError(context.getString(R.string.shell_delete_preset_error)) { repo.deletePreset(id); null } },
                    onUndoDelete = { preset ->
                        localOperationError(context.getString(R.string.shell_restore_preset_error)) {
                            repo.restorePreset(preset)
                            if (repo.state.value.presets.any { it.id == preset.id }) null else context.getString(R.string.shell_restore_preset_conflict)
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
                    onEnabled = { enabled -> localOperationError(context.getString(R.string.shell_settings_save_error)) { repo.setAutoDetect(enabled); null } },
                    onNotificationAccess = { activity?.openNotificationAccess() },
                    onSms = { if (access.sms) activity?.openAppSettings() else activity?.requestSms { accessTick++ } },
                    onNotifications = { if (access.canNotify) activity?.openLiveUpdateSettings() else activity?.requestNotifications { accessTick++ } },
                    onLiveUpdates = { activity?.openLiveUpdateSettings() },
                    onSaveRule = { merchant, category, reason -> localOperationError(context.getString(R.string.shell_save_rule_error)) { repo.saveMerchantRule(merchant, category, reason); null } },
                    onDeleteRule = { key -> localOperationError(context.getString(R.string.shell_delete_rule_error)) { repo.removeMerchantRule(key); null } },
                    snackbar = snackbar
                )
            }
            pane(LIMITS) {
                LimitsScreen(
                    state,
                    onBack = { nav.popBackStack() },
                    dailyDraft = dailyLimitDraft,
                    weeklyDraft = weeklyLimitDraft,
                    onDraftChange = { daily, weekly -> dailyLimitDraft = daily; weeklyLimitDraft = weekly },
                    onSave = { daily, weekly ->
                        val problem = localOperationError(context.getString(R.string.shell_save_limits_error)) { repo.setBudgets(daily, weekly); null }
                        if (problem == null) {
                            dailyLimitDraft = null
                            weeklyLimitDraft = null
                            nav.popBackStack()
                        }
                        problem
                    }
                )
            }
            pane(IMPORT) {
                purchaseDrafts.SaveableStateProvider("import:review") {
                ImportScreen(
                    state,
                    onBack = { nav.popBackStack() },
                    onAddMore = { activity?.pickMoreScreenshots() },
                    onRetry = repo::retryImportImage,
                    onRemove = { id -> localOperationError(context.getString(R.string.shell_remove_screenshot_error)) { repo.deleteImportImage(id); null } },
                    onUpdate = { candidate -> localOperationError(context.getString(R.string.shell_save_correction_error)) { repo.updateCandidate(candidate); null } },
                    onSave = {
                        val problem = localOperationError(context.getString(R.string.shell_save_import_error)) { repo.saveImport() }
                        if (problem == null) {
                            purchaseDrafts.removeState("import:review")
                            nav.popBackStack()
                        }
                        problem
                    },
                    onDiscard = {
                        val problem = localOperationError(context.getString(R.string.shell_discard_import_error)) { repo.discardImport(); null }
                        if (problem == null) {
                            purchaseDrafts.removeState("import:review")
                            nav.popBackStack()
                        }
                        problem
                    },
                    onAddByHand = { nav.goTopLevel(TODAY); addOpen = true },
                    snackbar = snackbar
                )
                }
            }
            pane(ONBOARDING) {
                val finish = { nav.navigate(TODAY) { popUpTo(ONBOARDING) { inclusive = true } } }
                OnboardingFlow(
                    state = state,
                    access = detectionAccess,
                    onTheme = { theme -> safeLocalAction { repo.setTheme(theme) } },
                    onNotificationAccess = { activity?.openNotificationAccess() },
                    onSms = { activity?.requestSms { accessTick++ } },
                    onNotifications = { if (detectionAccess.canNotify) activity?.openLiveUpdateSettings() else activity?.requestNotifications { accessTick++ } },
                    onComplete = { name, daily, weekly, automatic, reminders ->
                        safeLocalAction(key = "onboardingComplete", onSuccess = { if (nav.currentDestination?.route == ONBOARDING) finish() }) {
                            repo.completeOnboarding(name, daily, weekly)
                            repo.setAutoDetect(automatic)
                            repo.setReminders(reminders)
                        }
                    },
                    snackbar = snackbar
                )
            }
        }
        val previewRoute = gestureTargetRoute
        if (previewRoute != null && paneDragOffset != 0f && !reducedMotion) {
            Box(
                Modifier.fillMaxSize()
                    .absoluteOffset { IntOffset(panePreviewOffset(paneDragOffset, paneWidthPx).roundToInt(), 0) }
                    .auroraBackground(animate = false)
                    .clearAndSetSemantics { }
            ) {
                // A preview is already the destination, rather than a disposable screenshot/copy.
                // Moving this content into its NavHost entry preserves its week and scroll state.
                CompositionLocalProvider(LocalReducedMotion provides true) { topLevelPanes.getValue(previewRoute)() }
            }
        }
        if (gestureSourceRoute != null) {
            // New touches during settling cannot activate controls in the moving preview or start a
            // second navigation. The pointer chain of the gesture already in progress is retained.
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            })
        }
        }
        AnimatedVisibility(
            visible = topLevel.any { it.route == currentRoute },
            enter = if (reducedMotion) EnterTransition.None else slideInVertically(tween(220)) { it } + expandVertically(tween(220)),
            exit = if (reducedMotion) ExitTransition.None else slideOutVertically(tween(220)) { it } + shrinkVertically(tween(220))
        ) {
            TopLevelBar(currentRoute, haptics = state.haptics) {
                paneGestureGeneration++
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
                Modifier.fillMaxWidth().height(rowHeight).glass(shape, shadow = true)
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
                    val tint by pennyAnimatedColor(
                        if (isSelected) tokens.accent else tokens.muted,
                        animationSpec = if (reduced) snap<Color>() else PennyMotion.effects(),
                        label = "tabTint"
                    )
                    val labelColor by pennyAnimatedColor(
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
                            stringResource(item.labelRes),
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
    private val context: Context,
    private val manager: PeerSyncManager,
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
    private val shareCode: (String) -> Unit
) : CircleActions {
    private var busy by mutableStateOf(emptySet<String>())
    private var failures by mutableStateOf(emptyMap<String, String>())

    override fun isBusy(key: String) = key in busy
    override fun failure(key: String): String? = failures[key]
    override fun clearFailure(key: String) { failures = failures - key }

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
        clearFailure(key)
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
                    val detail = manager.userFacingError(e).takeIf { it.isNotBlank() }
                    val message = if (detail != null) context.getString(R.string.shell_action_error_detail, failure, detail)
                        else context.getString(R.string.shell_action_error_retry, failure)
                    failures = failures + (key to message)
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(message)
                }
            )
        }
    }

    override fun createCircle(name: String, onDone: () -> Unit) =
        run("create", context.getString(R.string.shell_create_circle_error), { onDone() }) { manager.createCircle(name) }

    override fun issueCode(rotate: Boolean, onCode: (String) -> Unit) =
        run("code", context.getString(R.string.shell_make_code_error), onCode) { manager.issueJoinCode(rotate = rotate) }

    override fun join(code: String) =
        run("join", context.getString(R.string.shell_join_circle_error), { }) { manager.joinWithCode(code.trim()) }

    override fun cancelJoin(onDone: () -> Unit) =
        run("cancelJoin", context.getString(R.string.shell_cancel_join_error), { clearFailure("join"); onDone() }) {
            withContext(Dispatchers.IO) { manager.cancelJoin() }
        }

    override fun checkJoin() =
        run("join", context.getString(R.string.shell_check_join_error)) { manager.checkPendingJoin() }

    override fun retryMember(memberId: String) =
        run("retry:$memberId", context.getString(R.string.shell_retry_member_error)) { manager.retryMember(memberId) }

    override fun approve(requestId: String) =
        run("approve:$requestId", context.getString(R.string.shell_approve_join_error), { notify(context.getString(R.string.shell_approve_join_success)) }) {
            manager.approveJoin(requestId)
        }

    override fun decline(requestId: String) =
        run("decline:$requestId", context.getString(R.string.shell_decline_join_error), { notify(context.getString(R.string.shell_decline_join_success)) }) {
            manager.declineJoin(requestId)
        }

    override fun leave(successorId: String?, onDone: () -> Unit) =
        run("leave", context.getString(R.string.shell_leave_circle_error), { onDone() }) { manager.leaveCircle(successorId) }

    override fun transferAdmin(memberId: String, onDone: () -> Unit) =
        run("transfer", context.getString(R.string.shell_transfer_admin_error), { onDone(); notify(context.getString(R.string.shell_transfer_admin_success)) }) {
            manager.transferAdmin(memberId)
        }

    override fun setVisibility(visibility: Visibility) =
        run("visibility", context.getString(R.string.shell_visibility_error), { notify(context.getString(R.string.shell_visibility_success)) }) {
            manager.setVisibility(visibility)
        }

    override fun nudge(memberId: String, name: String) =
        run("nudge:$memberId", context.getString(R.string.shell_send_nudge_error), { notify(context.getString(R.string.shell_send_nudge_success, name)) }) {
            manager.nudge(memberId)
        }

    override fun suggest(targetId: String, dailyPaise: Long, weeklyPaise: Long, name: String, onDone: () -> Unit) =
        run("suggest", context.getString(R.string.shell_send_suggestion_error), { onDone(); notify(context.getString(R.string.shell_send_suggestion_success, name)) }) {
            manager.suggestBudget(targetId, dailyPaise, weeklyPaise)
        }

    override fun respond(suggestionId: String, accepted: Boolean) =
        run("respond:$suggestionId", context.getString(R.string.shell_respond_error), { notify(if (accepted) context.getString(R.string.shell_limits_updated) else context.getString(R.string.shell_limits_kept)) }) {
            manager.respondToSuggestion(suggestionId, accepted)
        }

    override fun share(code: String) = shareCode(code)
}
