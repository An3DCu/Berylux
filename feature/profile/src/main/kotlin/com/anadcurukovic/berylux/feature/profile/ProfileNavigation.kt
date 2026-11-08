package com.anadcurukovic.berylux.feature.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.ProfileRoute

/** The player's personnel file. Settings open from here; filing it goes back. */
fun EntryProviderScope<NavKey>.profileEntries(onOpenSettings: () -> Unit, onDone: () -> Unit) {
    entry<ProfileRoute> {
        ProfileScreen(onOpenSettings = onOpenSettings, onDone = onDone)
    }
}
