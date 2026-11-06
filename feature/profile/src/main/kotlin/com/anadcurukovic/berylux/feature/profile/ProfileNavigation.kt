package com.anadcurukovic.berylux.feature.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.ProfileRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderLink
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** The player's profile. Settings open from here. */
fun EntryProviderScope<NavKey>.profileEntries(onOpenSettings: () -> Unit) {
    entry<ProfileRoute> {
        PlaceholderScreen(
            title = "Profile",
            links = listOf(PlaceholderLink("Settings", onOpenSettings)),
        )
    }
}
