package com.anadcurukovic.berylux.feature.rivals

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.RivalsRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** Rivals: the bot characters, and later the online leaderboard. */
fun EntryProviderScope<NavKey>.rivalsEntries() {
    entry<RivalsRoute> {
        PlaceholderScreen(title = "Rivals")
    }
}
