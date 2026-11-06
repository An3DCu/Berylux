package com.anadcurukovic.berylux.feature.editor

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.EditorRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** The editor for custom challenges. */
fun EntryProviderScope<NavKey>.editorEntries() {
    entry<EditorRoute> { key ->
        PlaceholderScreen(title = if (key.challengeId == null) "New challenge" else "Edit challenge")
    }
}
