package com.anadcurukovic.berylux.core.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavigatorTest {
    private val tabs = listOf(TodayRoute, CasesRoute, CatalogRoute, ProgressRoute)

    private fun navigator(keepTabHistory: Boolean = true): Navigator {
        val state = NavigationState(
            startRoute = TodayRoute,
            topLevelRoutes = tabs,
            selectedIndex = mutableIntStateOf(0),
            backStacks = tabs.associateWith { NavBackStack<NavKey>(it) },
        )
        return Navigator(state, keepTabHistory)
    }

    private fun Navigator.stack(route: NavKey) = state.backStacks.getValue(route).toList()

    @Test
    fun `the app opens on Today`() {
        val nav = navigator()
        assertThat(nav.state.currentKey).isEqualTo(TodayRoute)
        assertThat(nav.state.stacksInUse).containsExactly(TodayRoute)
    }

    @Test
    fun `a screen opens on top of the current tab`() {
        val nav = navigator()
        nav.navigate(CatalogRoute)
        nav.navigate(CategoryRoute(3))
        assertThat(nav.stack(CatalogRoute)).containsExactly(CatalogRoute, CategoryRoute(3)).inOrder()
        assertThat(nav.state.currentKey).isEqualTo(CategoryRoute(3))
    }

    @Test
    fun `Today stays underneath any other tab, so the app is left through Today`() {
        val nav = navigator()
        nav.navigate(ProgressRoute)
        assertThat(nav.state.stacksInUse).containsExactly(TodayRoute, ProgressRoute).inOrder()
    }

    @Test
    fun `back walks down the tab, then to Today, then out of the app`() {
        val nav = navigator()
        nav.navigate(CatalogRoute)
        nav.navigate(CategoryRoute(3))
        nav.navigate(EditorRoute())

        assertThat(nav.goBack()).isTrue()
        assertThat(nav.state.currentKey).isEqualTo(CategoryRoute(3))
        assertThat(nav.goBack()).isTrue()
        assertThat(nav.state.currentKey).isEqualTo(CatalogRoute)
        assertThat(nav.goBack()).isTrue()
        assertThat(nav.state.currentKey).isEqualTo(TodayRoute)
        assertThat(nav.goBack()).isFalse()
    }

    @Test
    fun `each tab remembers where you were`() {
        val nav = navigator(keepTabHistory = true)
        nav.navigate(CatalogRoute)
        nav.navigate(CategoryRoute(2))
        nav.navigate(CasesRoute)
        nav.navigate(CatalogRoute)
        assertThat(nav.state.currentKey).isEqualTo(CategoryRoute(2))
    }

    @Test
    fun `without tab history a tab opens at its root`() {
        val nav = navigator(keepTabHistory = false)
        nav.navigate(CatalogRoute)
        nav.navigate(CategoryRoute(2))
        nav.navigate(CasesRoute)
        nav.navigate(CatalogRoute)
        assertThat(nav.state.currentKey).isEqualTo(CatalogRoute)
    }

    @Test
    fun `tapping the tab you're on keeps your place`() {
        val nav = navigator()
        nav.navigate(CatalogRoute)
        nav.navigate(CategoryRoute(2))
        nav.navigate(CatalogRoute)
        assertThat(nav.state.currentKey).isEqualTo(CategoryRoute(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `the start route has to be a tab`() {
        NavigationState(
            startRoute = SettingsRoute,
            topLevelRoutes = tabs,
            selectedIndex = mutableIntStateOf(0),
            backStacks = tabs.associateWith { NavBackStack<NavKey>(it) },
        )
    }
}
