package com.anadcurukovic.berylux.feature.progress

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.ProgressRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderLink
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** Progress: rank, streak, achievements and the way to the rivals. */
fun EntryProviderScope<NavKey>.progressEntries(onOpenRivals: () -> Unit) {
    entry<ProgressRoute> {
        PlaceholderScreen(
            title = "Progress",
            links = listOf(PlaceholderLink("Rivals", onOpenRivals)),
        )
    }
}
