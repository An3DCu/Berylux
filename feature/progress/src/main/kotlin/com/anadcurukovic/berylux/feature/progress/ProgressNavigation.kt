package com.anadcurukovic.berylux.feature.progress

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.FileRoomRoute
import com.anadcurukovic.berylux.core.navigation.ProgressRoute
import com.anadcurukovic.berylux.feature.progress.fileroom.FileRoomScreen

/** Progress: rank, streak, achievements, and the ways to the rivals and the file room. */
fun EntryProviderScope<NavKey>.progressEntries(onOpenRivals: () -> Unit, onOpenFileRoom: () -> Unit) {
    entry<ProgressRoute> {
        ProgressScreen(onOpenRivals = onOpenRivals, onOpenFileRoom = onOpenFileRoom)
    }
    entry<FileRoomRoute> {
        FileRoomScreen()
    }
}
