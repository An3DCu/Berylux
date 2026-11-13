package com.anadcurukovic.berylux.widget

import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * While the app runs, redraws the widgets whenever a case is taken, closed or dropped, a player
 * starts, or the day's start hour changes; the end of the day is booked again with it.
 */
class WidgetSync @Inject constructor(
    private val profiles: ProfileRepository,
    private val cases: CaseRepository,
    private val settings: SettingsRepository,
    private val updates: WidgetUpdates,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val changes: Flow<Any?> = profiles.profile.flatMapLatest { profile ->
        if (profile == null) {
            flowOf(null)
        } else {
            combine(cases.caseFiles(profile.id), settings.settings) { files, userSettings -> files to userSettings }
        }
    }

    suspend fun run() {
        changes.collect {
            if (updates.anyPlaced()) {
                updates.drawAll()
                updates.bookEndOfDay()
            } else {
                updates.cancel()
            }
        }
    }
}
