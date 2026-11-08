package com.anadcurukovic.berylux.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.anadcurukovic.berylux.R
import com.anadcurukovic.berylux.core.navigation.CategoryRoute
import com.anadcurukovic.berylux.core.navigation.EditorRoute
import com.anadcurukovic.berylux.core.navigation.Navigator
import com.anadcurukovic.berylux.core.navigation.ProfileRoute
import com.anadcurukovic.berylux.core.navigation.RivalsRoute
import com.anadcurukovic.berylux.core.navigation.SettingsRoute
import com.anadcurukovic.berylux.core.navigation.TodayRoute
import com.anadcurukovic.berylux.core.navigation.rememberNavigationState
import com.anadcurukovic.berylux.core.ui.titleRes
import com.anadcurukovic.berylux.feature.catalog.catalogEntries
import com.anadcurukovic.berylux.feature.editor.editorEntries
import com.anadcurukovic.berylux.feature.profile.profileEntries
import com.anadcurukovic.berylux.feature.profile.welcome.WelcomeScreen
import com.anadcurukovic.berylux.feature.progress.progressEntries
import com.anadcurukovic.berylux.feature.rivals.rivalsEntries
import com.anadcurukovic.berylux.feature.settings.settingsEntries
import com.anadcurukovic.berylux.feature.today.todayEntries
import com.anadcurukovic.berylux.feature.tracker.casesEntries

/**
 * The whole app: the tabs and the screens. It's the only place that knows every feature;
 * the features only know the route keys and get callbacks for where they lead.
 */
@Composable
fun BeryluxApp(modifier: Modifier = Modifier, appViewModel: AppViewModel = hiltViewModel()) {
    val appState by appViewModel.state.collectAsStateWithLifecycle()
    when (appState) {
        // The first read of the database takes a moment; the window background shows meanwhile.
        AppState.Loading -> Unit
        AppState.NeedsProfile -> WelcomeScreen(modifier = modifier)
        AppState.Ready -> BeryluxTabs(appViewModel, modifier = modifier)
    }
}

/** The tabs and the screens behind them. */
@Composable
private fun BeryluxTabs(appViewModel: AppViewModel, modifier: Modifier = Modifier) {
    val hasNewAchievements by appViewModel.hasNewAchievements.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(appViewModel, resources) {
        appViewModel.unlockFlow.collect { achievement ->
            snackbar.showSnackbar(resources.getString(R.string.unlocked, resources.getString(achievement.titleRes())))
        }
    }

    val tabs = TopLevelTab.entries
    val state = rememberNavigationState(startRoute = TodayRoute, topLevelRoutes = tabs.map { it.route })
    val navigator = remember(state) { Navigator(state) }

    val entryProvider = entryProvider {
        todayEntries(onOpenProfile = { navigator.navigate(ProfileRoute) })
        casesEntries(onOpenToday = { navigator.navigate(TodayRoute) })
        catalogEntries(
            onOpenCategory = { navigator.navigate(CategoryRoute(it)) },
            onNewChallenge = { navigator.navigate(EditorRoute()) },
            onEditChallenge = { navigator.navigate(EditorRoute(it)) },
        )
        editorEntries(onDone = { navigator.goBack() })
        progressEntries(onOpenRivals = { navigator.navigate(RivalsRoute) })
        rivalsEntries()
        profileEntries(onOpenSettings = { navigator.navigate(SettingsRoute) }, onDone = { navigator.goBack() })
        settingsEntries()
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            tabs.forEach { tab ->
                item(
                    selected = tab.route == state.topLevelRoute,
                    onClick = { navigator.navigate(tab.route) },
                    icon = {
                        BadgedBox(badge = { if (tab == TopLevelTab.PROGRESS && hasNewAchievements) Badge() }) {
                            Icon(painterResource(tab.icon), contentDescription = null)
                        }
                    },
                    label = { Text(tab.label) },
                )
            }
        },
    ) {
        // Unlock messages drop in at the top of whatever screen is open; a screen's own
        // messages keep the bottom, so both can show at once.
        Box {
            NavDisplay(
                entries = state.toDecoratedEntries(entryProvider),
                onBack = { navigator.goBack() },
            )
            // The screens draw edge to edge. Without this strip, a scrolled list shows through
            // the clock and the status icons.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background),
            )
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars),
            )
        }
    }
}
