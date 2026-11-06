package com.anadcurukovic.berylux.feature.today

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.TodayRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderLink
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** Today: the day's cases. The avatar leads to the profile. */
fun EntryProviderScope<NavKey>.todayEntries(
    onOpenProfile: () -> Unit,
    onNewChallenge: () -> Unit,
) {
    entry<TodayRoute> {
        PlaceholderScreen(
            title = "Today",
            links = listOf(
                PlaceholderLink("Profile", onOpenProfile),
                PlaceholderLink("New challenge", onNewChallenge),
            ),
        )
    }
}
