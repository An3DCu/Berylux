package com.anadcurukovic.berylux.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * One back stack per top-level route, plus which one is showing. Back always ends on the start
 * route before leaving the app ("exit through home").
 *
 * This class only holds the state. [Navigator] changes it.
 */
class NavigationState(
    val startRoute: NavKey,
    val topLevelRoutes: List<NavKey>,
    private val selectedIndex: MutableState<Int>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    init {
        require(startRoute in topLevelRoutes) { "the start route must be a top-level route" }
    }

    var topLevelRoute: NavKey
        get() = topLevelRoutes[selectedIndex.value]
        set(value) {
            selectedIndex.value = topLevelRoutes.indexOf(value).also {
                require(it >= 0) { "$value isn't a top-level route" }
            }
        }

    /** The stacks on screen: the start route's, then the selected one's if it's another. */
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) listOf(startRoute) else listOf(startRoute, topLevelRoute)

    /** The key on top, the screen the user sees. */
    val currentKey: NavKey
        get() = backStacks.getValue(topLevelRoute).last()

    /**
     * Turns the stacks in use into entries, each stack with its own saved state and ViewModels.
     * Each entry carries the position of its tab, read back with [tab].
     */
    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decorated = backStacks.mapValues { (route, stack) ->
            val tab = topLevelRoutes.indexOf(route)
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = { key -> entryProvider(key).inTab(key, tab) },
            )
        }
        return stacksInUse.flatMap { decorated[it].orEmpty() }
    }
}

/** The position of the tab this entry belongs to, left to right; null for an entry from elsewhere. */
val NavEntry<*>.tab: Int?
    get() = metadata[TAB] as? Int

private fun NavEntry<NavKey>.inTab(key: NavKey, tab: Int): NavEntry<NavKey> =
    NavEntry(key, contentKey, metadata + (TAB to tab)) { Content() }

private const val TAB = "berylux.tab"

/** Creates the state once and keeps it through rotation and process death. */
@Composable
fun rememberNavigationState(startRoute: NavKey, topLevelRoutes: List<NavKey>): NavigationState {
    val selected = rememberSaveable { mutableIntStateOf(topLevelRoutes.indexOf(startRoute)) }
    val backStacks = topLevelRoutes.associateWith { rememberNavBackStack(it) }
    return remember(startRoute, topLevelRoutes) {
        NavigationState(startRoute, topLevelRoutes, selected, backStacks)
    }
}
