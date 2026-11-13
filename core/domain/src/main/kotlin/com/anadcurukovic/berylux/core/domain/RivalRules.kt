package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.Rival
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * The bots' Lux. Each day's number comes from the player's seed, the bot and the day. It's the
 * same on every launch and every device, and two players get different bots.
 */
object RivalRules {
    // Each bot's habit, in Lux for a whole game day.
    private val STEADY = 40..70
    private val SPRINT_BURST = 120..200
    private val SPRINT_REST = 0..20
    private val SLACK = 10..40
    private val NIGHT = 30..90
    private val GRIND = 70..110

    private const val SPRINT_PERCENT = 30
    private const val SLACK_SKIP_PERCENT = 50
    private const val GRIND_REST_ONE_IN = 7
    private const val PERCENT = 100

    /** The night owl earns nothing in the first half of the game day, then catches up by its end. */
    private const val NIGHT_START = 0.5

    private const val PRIME = 1_000_003L

    /** A bot's Lux for a whole game day. */
    fun luxOn(rival: Rival, day: LocalDate, seed: Long): Int {
        val random = SplitMix64(seed xor (day.toEpochDay() * PRIME) xor (rival.ordinal + 1L) * PRIME * PRIME)
        fun IntRange.pick() = first + random.nextInt(last - first + 1)
        return when (rival) {
            Rival.LEDGER -> STEADY.pick()
            Rival.FLASHBULB -> if (random.nextInt(PERCENT) < SPRINT_PERCENT) SPRINT_BURST.pick() else SPRINT_REST.pick()
            Rival.SNOOZE -> if (random.nextInt(PERCENT) < SLACK_SKIP_PERCENT) 0 else SLACK.pick()
            Rival.NIGHTJAR -> NIGHT.pick()
            Rival.GRINDSTONE -> if (random.nextInt(GRIND_REST_ONE_IN) == 0) 0 else GRIND.pick()
        }
    }

    /** How much of a day's Lux a bot has by [dayFraction] (0 at the start hour, 1 at the next). */
    fun luxSoFar(rival: Rival, day: LocalDate, seed: Long, dayFraction: Double): Int {
        val share = when (rival) {
            Rival.NIGHTJAR -> ((dayFraction - NIGHT_START) / (1 - NIGHT_START)).coerceIn(0.0, 1.0)
            else -> dayFraction.coerceIn(0.0, 1.0)
        }
        return (luxOn(rival, day, seed) * share).toInt()
    }

    /** The Monday that starts the week of [day]. */
    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** How far into its game day [now] is, from 0 to 1. */
    fun dayFraction(now: Instant, zone: ZoneId, day: LocalDate, startHour: Int): Double {
        val start = day.atTime(startHour, 0).atZone(zone).toInstant()
        val end = day.plusDays(1).atTime(startHour, 0).atZone(zone).toInstant()
        return Duration.between(start, now).toMillis().toDouble() / Duration.between(start, end).toMillis()
    }
}

/** One row of the board. [rival] is null for the player. */
data class BoardRow(val rival: Rival?, val lux: Int) {
    val isYou: Boolean get() = rival == null
}

/**
 * @property rows this week, highest Lux first. A tie puts the player above the bot.
 * @property lastWeekPlace the player's place last week, from 1; null in the week they started.
 */
data class RivalBoard(
    val weekStart: LocalDate,
    val rows: List<BoardRow>,
    val lastWeekPlace: Int?,
) {
    val place: Int get() = rows.indexOfFirst { it.isYou } + 1
}

/** The rivals board for this week, moving as the day goes on. */
class RivalBoardUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val cases: CaseRepository,
    private val settings: SettingsRepository,
    private val time: TimeSource,
) {
    /** Recomputed once every [tick]. The bots' Lux grows through the day. */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(tick: kotlin.time.Duration = 1.minutes): Flow<RivalBoard> {
        val ticks = flow {
            while (true) {
                emit(time.now())
                delay(tick)
            }
        }
        return profiles.profile.filterNotNull().flatMapLatest { profile ->
            combine(cases.caseFiles(profile.id), settings.settings, ticks) { files, userSettings, now ->
                board(files, profile, now, userSettings.startHourAt(now))
            }
        }
    }

    /** Both sides count from the day the player started. A first week is a fair one. */
    private fun board(files: List<CaseFile>, profile: Profile, now: Instant, startHour: Int): RivalBoard {
        val zone = time.zone()
        val seed = profile.pickSeed
        val startDay = GameDay.of(profile.createdAt, zone, startHour)
        val today = GameDay.of(now, zone, startHour)
        val fraction = RivalRules.dayFraction(now, zone, today, startHour)
        val thisWeek = RivalRules.weekStart(today)
        val lastWeek = thisWeek.minusWeeks(1)

        fun youIn(from: LocalDate, to: LocalDate) = files
            .filter { it.case.status == CaseStatus.CLOSED }
            .filter { day -> day.case.closedOn?.let { !it.isBefore(from) && !it.isAfter(to) } == true }
            .sumOf { it.case.luxEarned }

        fun botIn(rival: Rival, from: LocalDate, to: LocalDate) =
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.sumOf { day ->
                if (day == today) {
                    RivalRules.luxSoFar(rival, day, seed, fraction)
                } else {
                    RivalRules.luxOn(rival, day, seed)
                }
            }

        fun rank(rows: List<BoardRow>) = rows.sortedWith(compareByDescending<BoardRow> { it.lux }.thenBy { !it.isYou })

        fun rows(from: LocalDate, to: LocalDate) = rank(
            listOf(BoardRow(null, youIn(from, to))) + Rival.entries.map { BoardRow(it, botIn(it, from, to)) },
        )

        val lastWeekEnd = thisWeek.minusDays(1)
        val lastWeekPlace = if (startDay.isAfter(lastWeekEnd)) {
            null
        } else {
            rows(maxOf(lastWeek, startDay), lastWeekEnd).indexOfFirst { it.isYou } + 1
        }
        return RivalBoard(thisWeek, rows(maxOf(thisWeek, startDay), today), lastWeekPlace)
    }
}
