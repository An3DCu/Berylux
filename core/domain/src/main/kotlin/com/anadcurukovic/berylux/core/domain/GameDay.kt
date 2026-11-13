package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.UserSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A game day starts at a chosen hour on the phone's clock, 4 AM by default, so a case closed at
 * 1 AM still counts for the evening before.
 *
 * The hour is read on the local clock, not as a fixed offset from UTC. On the nights the clocks
 * change, the day still starts at the chosen local hour, and that game day is 23 or 25 hours long.
 */
object GameDay {
    const val DEFAULT_START_HOUR = UserSettings.DEFAULT_DAY_START_HOUR

    fun of(instant: Instant, zone: ZoneId, startHour: Int = DEFAULT_START_HOUR): LocalDate {
        require(startHour in UserSettings.DAY_START_HOURS) { "startHour must be 0 to 23, was $startHour" }
        return instant.atZone(zone).toLocalDateTime().minusHours(startHour.toLong()).toLocalDate()
    }

    /**
     * When the game day in force at [instant] ends: the start hour on the next calendar day. On the
     * night the clocks go forward, a start hour that doesn't exist moves on by the gap.
     */
    fun endOf(instant: Instant, zone: ZoneId, startHour: Int = DEFAULT_START_HOUR): Instant =
        of(instant, zone, startHour).plusDays(1).atTime(startHour, 0).atZone(zone).toInstant()
}
