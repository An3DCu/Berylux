package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import javax.inject.Inject

/** The Lux closed cases earned in the week starting on [weekStart], a Monday. */
data class WeekLux(val weekStart: LocalDate, val lux: Int)

/** How a day shows on the streak calendar. */
enum class DayMark {
    /** At least one case closed: the day counts. */
    CLOSED,

    /** Missed, and saved by a freeze. */
    SAVED,

    /** Missed, or still to come. */
    NONE,
}

data class CalendarDay(val day: LocalDate, val mark: DayMark, val isToday: Boolean)

/** How many cases were closed in one category. */
data class CategoryCount(val name: String, val closed: Int)

/**
 * The file room's three panels. [weeks] are the last [CaseHistory.WEEKS], oldest first, this week
 * last. [calendar] is [CaseHistory.CALENDAR_WEEKS] weeks from Monday, this week last, days to come
 * included. [categories] has only categories with a closed case, most first.
 */
data class CaseHistoryState(
    val weeks: List<WeekLux>,
    val calendar: List<CalendarDay>,
    val categories: List<CategoryCount>,
) {
    val anyClosed: Boolean get() = categories.isNotEmpty()
    val luxThisWeek: Int get() = weeks.last().lux
    val luxInWeeks: Int get() = weeks.sumOf { it.lux }
    val bestWeek: Int get() = weeks.maxOf { it.lux }
    val closedDays: Int get() = calendar.count { it.mark == DayMark.CLOSED }
    val savedDays: Int get() = calendar.count { it.mark == DayMark.SAVED }

    /** The calendar's days up to today; the rest of this week is still to come. */
    val daysSoFar: Int get() = calendar.indexOfFirst { it.isToday } + 1
}

/** The numbers behind the file room, from every case the player has. */
object CaseHistory {
    const val WEEKS = 12
    const val CALENDAR_WEEKS = 5
    private const val DAYS_A_WEEK = 7L

    fun of(files: List<CaseFile>, categories: List<Category>, today: LocalDate): CaseHistoryState {
        val closed = files.map { it.case to it.challenge }.filter { (case, _) -> case.status == CaseStatus.CLOSED }
        val thisWeek = RivalRules.weekStart(today)

        val luxByWeek = closed
            .groupBy { (case, _) -> RivalRules.weekStart(requireNotNull(case.closedOn)) }
            .mapValues { (_, inWeek) -> inWeek.sumOf { (case, _) -> case.luxEarned } }
        val weeks = (WEEKS - 1 downTo 0).map { back ->
            val start = thisWeek.minusWeeks(back.toLong())
            WeekLux(start, luxByWeek[start] ?: 0)
        }

        val closedDays = closed.mapNotNull { (case, _) -> case.closedOn }.toSet()
        val saved = Streaks.calculate(closedDays, today).savedDays.toSet()
        val first = thisWeek.minusWeeks(CALENDAR_WEEKS - 1L)
        val calendar = (0 until CALENDAR_WEEKS * DAYS_A_WEEK).map { first.plusDays(it) }.map { day ->
            val mark = when (day) {
                in closedDays -> DayMark.CLOSED
                in saved -> DayMark.SAVED
                else -> DayMark.NONE
            }
            CalendarDay(day, mark, isToday = day == today)
        }

        val names = categories.associate { it.id to it.name }
        val byCategory = closed
            .groupingBy { (_, challenge) -> names[challenge.categoryId].orEmpty() }
            .eachCount()
            .map { (name, count) -> CategoryCount(name, count) }
            .sortedWith(compareByDescending<CategoryCount> { it.closed }.thenBy { it.name })

        return CaseHistoryState(weeks, calendar, byCategory)
    }
}

/** The file room as one flow: it follows every case change and moves on with the game day. */
class CaseHistoryUseCase @Inject constructor(
    private val playerCaseFiles: PlayerCaseFiles,
    private val challenges: ChallengeRepository,
    private val clock: GameClock,
) {
    operator fun invoke(): Flow<CaseHistoryState> =
        combine(playerCaseFiles(), challenges.categories, clock.days()) { files, categories, today ->
            CaseHistory.of(files, categories, today)
        }.distinctUntilChanged()
}
