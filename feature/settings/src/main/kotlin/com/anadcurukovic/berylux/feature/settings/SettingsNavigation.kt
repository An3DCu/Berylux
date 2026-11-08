package com.anadcurukovic.berylux.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.SettingsRoute

/** Settings: the hour the day starts, the theme, and the grain and scanlines. */
fun EntryProviderScope<NavKey>.settingsEntries() {
    entry<SettingsRoute> {
        SettingsScreen()
    }
}
