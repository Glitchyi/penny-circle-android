package com.spendingbuddy.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Horizontal pane transitions for the app's NavHost.
 * [direction] returns +/-2 for top-level tabs (shared-axis travel) and +/-1 for pushed panes.
 */
object PaneMotion {
    private const val TAB_AXIS = 2
    private const val PANE_AXIS = 1
    private const val DURATION_MS = 300

    private val tabs = listOf("today", "activity", "circle")
    private val settingsChildren = setOf("limits", "presets", "autodetect")
    private val knownRoutes = tabs.toSet() + settingsChildren + setOf(
        "settings", "import", "onboarding", "circle/create", "circle/invite", "circle/join", "circle/settings"
    )

    /**
     * Relative navigation direction. Positive values move toward the logical end; negative values move back.
     * Returns 0 for the same route or an unknown route pair.
     */
    fun direction(fromRoute: String?, toRoute: String?): Int {
        val from = normalize(fromRoute) ?: return 0
        val to = normalize(toRoute) ?: return 0
        if (from == to || from !in knownRoutes || to !in knownRoutes) return 0

        val fromTab = tabIndex(from)
        val toTab = tabIndex(to)
        if (fromTab != null && toTab != null) {
            if (fromTab == toTab) return 0
            return if (toTab > fromTab) TAB_AXIS else -TAB_AXIS
        }

        val fromTabRoot = tabRoot(from)
        val toTabRoot = tabRoot(to)
        if (fromTabRoot != null && toTabRoot != null && fromTabRoot != toTabRoot) {
            return if (tabs.indexOf(toTabRoot) > tabs.indexOf(fromTabRoot)) TAB_AXIS else -TAB_AXIS
        }

        if (from == "onboarding") return PANE_AXIS
        if (to == "onboarding") return -PANE_AXIS

        if (parentRoute(to) == from) return PANE_AXIS
        if (parentRoute(from) == to) return -PANE_AXIS
        if (paneRoot(from) == paneRoot(to)) {
            val fromDepth = paneDepth(from)
            val toDepth = paneDepth(to)
            return when {
                toDepth > fromDepth -> PANE_AXIS
                toDepth < fromDepth -> -PANE_AXIS
                else -> PANE_AXIS
            }
        }

        // Moving from a content root to Settings/onboarding is a push; returning to a tab is a back move.
        if (fromTabRoot != null && toTabRoot == null) return PANE_AXIS
        if (fromTabRoot == null && toTabRoot != null) return -PANE_AXIS
        return PANE_AXIS
    }

    /** Forward push: new panes enter from the logical end; tabs travel together along the horizontal axis. */
    fun enter(direction: Int, reducedMotion: Boolean): EnterTransition {
        if (reducedMotion || direction == 0) return EnterTransition.None
        val sign = direction.sign()
        return if (direction.isTabAxis()) {
            slideInHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { width -> width * sign }
        } else {
            slideInHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { width -> width * sign * 90 / 100 }
        }
    }

    /** Forward push: keep the outgoing pane visible briefly under the incoming pane. */
    fun exit(direction: Int, reducedMotion: Boolean): ExitTransition {
        if (reducedMotion || direction == 0) return ExitTransition.None
        val sign = direction.sign()
        return if (direction.isTabAxis()) {
            slideOutHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { width -> -width * sign }
        } else {
            slideOutHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { width -> -width * sign * 15 / 100 }
        }
    }

    /** Pop: bring the previous pane back from its short parallax offset, or reverse the tab shared axis. */
    fun popEnter(direction: Int, reducedMotion: Boolean): EnterTransition {
        if (reducedMotion || direction == 0) return EnterTransition.None
        val sign = direction.sign()
        return if (direction.isTabAxis()) {
            enter(direction, reducedMotion = false)
        } else {
            slideInHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { width -> width * sign * 15 / 100 }
        }
    }

    /** Pop: move the current pane away along the reverse of its push direction. */
    fun popExit(direction: Int, reducedMotion: Boolean): ExitTransition {
        if (reducedMotion || direction == 0) return ExitTransition.None
        val sign = direction.sign()
        return if (direction.isTabAxis()) {
            exit(direction, reducedMotion = false)
        } else {
            slideOutHorizontally(tween(DURATION_MS, easing = FastOutSlowInEasing)) { width -> -width * sign * 90 / 100 }
        }
    }

    private fun normalize(route: String?): String? = route?.substringBefore('?')?.takeIf { it.isNotBlank() }
    private fun Int.sign(): Int = if (this > 0) 1 else -1
    private fun Int.isTabAxis(): Boolean = kotlin.math.abs(this) == TAB_AXIS

    private fun tabIndex(route: String): Int? = tabs.indexOf(route).takeIf { it >= 0 }

    private fun tabRoot(route: String): String? = when {
        route == "today" || route == "import" -> "today"
        route == "activity" -> "activity"
        route == "circle" || route.startsWith("circle/") -> "circle"
        else -> null
    }

    private fun paneRoot(route: String): String = when {
        route in settingsChildren || route == "settings" -> "settings"
        route == "import" -> "today"
        route == "onboarding" -> "onboarding"
        route.startsWith("circle/") -> "circle"
        else -> tabRoot(route) ?: route
    }

    private fun parentRoute(route: String): String? = when {
        route in settingsChildren -> "settings"
        route == "import" -> "today"
        route.startsWith("circle/") -> "circle"
        else -> null
    }

    private fun paneDepth(route: String): Int = if (parentRoute(route) == null) 0 else 1
}
