package com.anadcurukovic.berylux.feature.progress

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.ProgressRoute

/** Progress: rank, streak, achievements and the way to the rivals. */
fun EntryProviderScope<NavKey>.progressEntries(onOpenRivals: () -> Unit) {
    entry<ProgressRoute> {
        ProgressScreen(onOpenRivals = onOpenRivals)
    }
}
