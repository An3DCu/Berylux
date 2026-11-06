package com.anadcurukovic.berylux.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Every navigation event goes through here. Screens get callbacks, never the navigator itself,
 * so they stay previewable and testable.
 *
 * @param keepTabHistory when true, switching tabs keeps each tab's own back stack. When false,
 * a tab always opens at its root.
 */
class Navigator(
    val state: NavigationState,
    private val keepTabHistory: Boolean = true,
) {
    /** A top-level route switches tabs; anything else opens on top of the current tab. */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            if (!keepTabHistory && route != state.topLevelRoute) resetToRoot(route)
            state.topLevelRoute = route
        } else {
            state.backStacks.getValue(state.topLevelRoute).add(route)
        }
    }

    /**
     * Back pops the current tab. At a tab's root it goes to the start tab. At the start tab's
     * root it returns false, and the system closes the app.
     */
    fun goBack(): Boolean {
        val stack = state.backStacks.getValue(state.topLevelRoute)
        return when {
            stack.size > 1 -> {
                stack.removeAt(stack.lastIndex)
                true
            }
            state.topLevelRoute != state.startRoute -> {
                if (!keepTabHistory) resetToRoot(state.topLevelRoute)
                state.topLevelRoute = state.startRoute
                true
            }
            else -> false
        }
    }

    private fun resetToRoot(route: NavKey) {
        val stack = state.backStacks.getValue(route)
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}
