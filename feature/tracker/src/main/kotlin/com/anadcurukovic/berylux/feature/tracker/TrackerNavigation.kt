package com.anadcurukovic.berylux.feature.tracker

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.CasesRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** Cases: the cases taken on, open and closed. */
fun EntryProviderScope<NavKey>.casesEntries() {
    entry<CasesRoute> {
        PlaceholderScreen(title = "Cases")
    }
}
