package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.Challenge
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/** When the morning docket, the daily reminder, comes next, and which day it speaks for. */
object MorningDocket {
    /**
     * The next [time] on the phone's clock after [now]. On the night the clocks go forward, a time
     * that doesn't exist moves on by the gap, the way the clock itself does.
     */
    fun nextAt(now: Instant, zone: ZoneId, time: LocalTime): Instant {
        val today = now.atZone(zone).toLocalDate()
        val todays = ZonedDateTime.of(today, time, zone).toInstant()
        return if (todays.isAfter(now)) todays else ZonedDateTime.of(today.plusDays(1), time, zone).toInstant()
    }

    /**
     * The day a reminder sent at [at] names: its calendar day. After the day's start hour that's the
     * game day in force; before it, it's the game day about to start, not the one ending.
     */
    fun dayFor(at: Instant, zone: ZoneId): LocalDate = at.atZone(zone).toLocalDate()
}

/** The daily case the morning docket names, or null before there's a player or a challenge. */
class MorningDocketCaseUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val challenges: ChallengeRepository,
    private val time: TimeSource,
) {
    suspend operator fun invoke(): Challenge? {
        val profile = profiles.profile.first() ?: return null
        val day = MorningDocket.dayFor(time.now(), time.zone())
        return DailyPicker.pick(
            challenges = challenges.challenges.first(),
            categories = challenges.categories.first(),
            day = day,
            profileSeed = profile.pickSeed,
        ).daily
    }
}
