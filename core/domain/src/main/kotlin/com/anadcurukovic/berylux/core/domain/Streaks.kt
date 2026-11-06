package com.anadcurukovic.berylux.core.domain

import java.time.LocalDate

/**
 * @property current days in the running streak. Days saved by a freeze don't add to it.
 * @property longest the longest streak so far, the running one included.
 * @property freezesBanked freezes ready to save the next missed day.
 * @property frozenDays the missed days a freeze saved in the running streak.
 * @property doneToday whether today already counts. Until today is over, a streak isn't broken.
 */
data class StreakState(
    val current: Int,
    val longest: Int,
    val freezesBanked: Int,
    val frozenDays: List<LocalDate>,
    val doneToday: Boolean,
)

/**
 * A game day counts when at least one case was closed on it. Every [FREEZE_EVERY] counted days in
 * a row earn a freeze, with at most [MAX_FREEZES] banked. A freeze saves one missed day; a missed
 * day without one ends the streak.
 */
object Streaks {
    const val FREEZE_EVERY = 7
    const val MAX_FREEZES = 1

    fun calculate(countedDays: Collection<LocalDate>, today: LocalDate): StreakState {
        val days = countedDays.filter { !it.isAfter(today) }.toSortedSet()
        if (days.isEmpty()) {
            return StreakState(current = 0, longest = 0, freezesBanked = 0, frozenDays = emptyList(), doneToday = false)
        }

        var current = 0
        var longest = 0
        var banked = 0
        val frozen = mutableListOf<LocalDate>()

        var day = days.first()
        while (!day.isAfter(today)) {
            when {
                day in days -> {
                    current++
                    longest = maxOf(longest, current)
                    if (current % FREEZE_EVERY == 0) banked = minOf(banked + 1, MAX_FREEZES)
                }
                // Today isn't over yet, so not having closed a case today breaks nothing.
                day == today -> Unit
                current > 0 && banked > 0 -> {
                    banked--
                    frozen += day
                }
                else -> {
                    current = 0
                    banked = 0
                    frozen.clear()
                }
            }
            day = day.plusDays(1)
        }

        return StreakState(
            current = current,
            longest = longest,
            freezesBanked = banked,
            frozenDays = frozen.toList(),
            doneToday = today in days,
        )
    }
}
