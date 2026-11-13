package com.anadcurukovic.berylux.feature.tracker

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.CasesRoute

/**
 * Cases: today's cases to close or drop, and the earlier days. An empty day leads to Today.
 * [closeRequest] is a case to ask "Close it?" for, from the home screen widget, until
 * [onCloseRequestSeen].
 */
fun EntryProviderScope<NavKey>.casesEntries(
    onOpenToday: () -> Unit,
    closeRequest: () -> Long? = { null },
    onCloseRequestSeen: () -> Unit = {},
) {
    entry<CasesRoute> {
        CasesScreen(
            onOpenToday = onOpenToday,
            closeRequest = closeRequest(),
            onCloseRequestSeen = onCloseRequestSeen,
        )
    }
}
