package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Picks a new hour for the game day to start. Today keeps its end: the new hour takes over when
 * today ends at the hour in force now. Nothing goes cold early and no day comes back. Picking the
 * hour in force now again cancels a waiting change.
 */
class ChangeDayStartHourUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val time: TimeSource,
) {
    suspend operator fun invoke(hour: Int) {
        val now = time.now()
        val zone = time.zone()
        val current = settings.settings.first().startHourAt(now)
        if (hour == current) {
            settings.scheduleDayStartHour(current, next = null, from = null)
            return
        }
        val today = GameDay.of(now, zone, current)
        val endOfToday = today.plusDays(1).atTime(current, 0).atZone(zone).toInstant()
        settings.scheduleDayStartHour(current, next = hour, from = endOfToday)
    }
}
