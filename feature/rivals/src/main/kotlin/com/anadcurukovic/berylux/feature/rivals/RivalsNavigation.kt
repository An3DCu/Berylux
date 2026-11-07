package com.anadcurukovic.berylux.feature.rivals

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.RivalsRoute

/** Rivals: this week's board with the five bots. The online leaderboard comes in the cloud round. */
fun EntryProviderScope<NavKey>.rivalsEntries() {
    entry<RivalsRoute> {
        RivalsScreen()
    }
}
