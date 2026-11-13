package com.anadcurukovic.berylux.reminder

import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Keeps WorkManager in step with the setting: one docket booked while it's on, none while it's off. */
class ReminderSync @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend fun run() {
        settings.settings
            .map { it.reminderOn to it.reminderTime }
            .distinctUntilChanged()
            .collect { (on, at) -> if (on) scheduler.schedule(at) else scheduler.cancel() }
    }
}
