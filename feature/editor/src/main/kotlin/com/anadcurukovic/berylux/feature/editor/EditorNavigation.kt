package com.anadcurukovic.berylux.feature.editor

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.EditorRoute

/** The editor for custom challenges. [onDone] leaves it after a save or a delete. */
fun EntryProviderScope<NavKey>.editorEntries(onDone: () -> Unit) {
    entry<EditorRoute> { key ->
        EditorScreen(challengeId = key.challengeId, onDone = onDone)
    }
}
