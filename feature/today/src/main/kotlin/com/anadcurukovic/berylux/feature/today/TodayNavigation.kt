package com.anadcurukovic.berylux.feature.today

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.TodayRoute

/** Today: the day's cases. The sigil leads to the profile. */
fun EntryProviderScope<NavKey>.todayEntries(onOpenProfile: () -> Unit) {
    entry<TodayRoute> {
        TodayScreen(onOpenProfile = onOpenProfile)
    }
}
