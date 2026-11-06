package com.anadcurukovic.berylux.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.SettingsRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** Settings: day start hour, theme, effects. */
fun EntryProviderScope<NavKey>.settingsEntries() {
    entry<SettingsRoute> {
        PlaceholderScreen(title = "Settings")
    }
}
