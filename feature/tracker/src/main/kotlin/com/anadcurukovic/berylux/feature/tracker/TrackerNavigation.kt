package com.anadcurukovic.berylux.feature.tracker

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.CasesRoute

/** Cases: today's cases to close or drop, and the earlier days. An empty day leads to Today. */
fun EntryProviderScope<NavKey>.casesEntries(onOpenToday: () -> Unit) {
    entry<CasesRoute> {
        CasesScreen(onOpenToday = onOpenToday)
    }
}
