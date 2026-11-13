package com.anadcurukovic.berylux.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class StreaksTest {
    private fun nov(day: Int): LocalDate = LocalDate.of(2026, 11, day)

    private fun novRange(first: Int, last: Int) = (first..last).map(::nov)

    @Test
    fun `no days, no streak`() {
        val state = Streaks.calculate(emptyList(), today = nov(10))
        assertThat(state).isEqualTo(
            StreakState(current = 0, longest = 0, freezesBanked = 0, frozenDays = emptyList(), doneToday = false),
        )
    }

    @Test
    fun `a case closed today starts a streak`() {
        val state = Streaks.calculate(listOf(nov(10)), today = nov(10))
        assertThat(state.current).isEqualTo(1)
        assertThat(state.doneToday).isTrue()
    }

    @Test
    fun `yesterday's streak is still alive until today is over`() {
        val state = Streaks.calculate(listOf(nov(9)), today = nov(10))
        assertThat(state.current).isEqualTo(1)
        assertThat(state.doneToday).isFalse()
    }

    @Test
    fun `a missed day without a freeze ends the streak`() {
        val state = Streaks.calculate(listOf(nov(8)), today = nov(10))
        assertThat(state.current).isEqualTo(0)
        assertThat(state.longest).isEqualTo(1)
    }

    @Test
    fun `seven days in a row earn a freeze`() {
        val state = Streaks.calculate(novRange(4, 10), today = nov(10))
        assertThat(state.current).isEqualTo(7)
        assertThat(state.freezesBanked).isEqualTo(1)
    }

    @Test
    fun `six days in a row earn nothing yet`() {
        val state = Streaks.calculate(novRange(1, 6) + nov(8), today = nov(8))
        // 7 November is missed with no freeze. 8 November starts over.
        assertThat(state.current).isEqualTo(1)
        assertThat(state.longest).isEqualTo(6)
        assertThat(state.freezesBanked).isEqualTo(0)
    }

    @Test
    fun `no more than one freeze is banked`() {
        val state = Streaks.calculate(novRange(1, 14), today = nov(14))
        assertThat(state.current).isEqualTo(14)
        assertThat(state.freezesBanked).isEqualTo(1)
    }

    @Test
    fun `a freeze saves one missed day, which doesn't add to the streak`() {
        // 1 to 7 November earn a freeze, it saves the 8th, the 9th makes eight days.
        val state = Streaks.calculate(novRange(1, 7) + nov(9), today = nov(9))
        assertThat(state.current).isEqualTo(8)
        assertThat(state.freezesBanked).isEqualTo(0)
        assertThat(state.frozenDays).containsExactly(nov(8))
    }

    @Test
    fun `two missed days are too many for one freeze`() {
        val state = Streaks.calculate(novRange(1, 7) + nov(10), today = nov(10))
        assertThat(state.current).isEqualTo(1)
        assertThat(state.longest).isEqualTo(7)
        assertThat(state.freezesBanked).isEqualTo(0)
        assertThat(state.frozenDays).isEmpty()
    }

    @Test
    fun `a saved day stays on record after the streak it saved has ended`() {
        // The 8th is saved; the 11th and 12th are missed with no freeze left.
        val state = Streaks.calculate(novRange(1, 7) + novRange(9, 10) + nov(13), today = nov(13))
        assertThat(state.current).isEqualTo(1)
        assertThat(state.frozenDays).isEmpty()
        assertThat(state.savedDays).containsExactly(nov(8))
        assertThat(state.freezesUsed).isEqualTo(1)
    }

    @Test
    fun `yesterday's miss uses the freeze even before today is done`() {
        val state = Streaks.calculate(novRange(1, 7), today = nov(9))
        assertThat(state.current).isEqualTo(7)
        assertThat(state.freezesBanked).isEqualTo(0)
        assertThat(state.frozenDays).containsExactly(nov(8))
        assertThat(state.doneToday).isFalse()
    }

    @Test
    fun `fourteen counted days earn the next freeze, saved days not included`() {
        // 1 to 7 counted, 8 saved, 9 to 15 counted: 14 counted days on the 15th.
        val state = Streaks.calculate(novRange(1, 7) + novRange(9, 15), today = nov(15))
        assertThat(state.current).isEqualTo(14)
        assertThat(state.freezesBanked).isEqualTo(1)
        assertThat(state.frozenDays).containsExactly(nov(8))
    }

    @Test
    fun `days after today are ignored and duplicates count once`() {
        val state = Streaks.calculate(listOf(nov(5), nov(5), nov(6)), today = nov(5))
        assertThat(state.current).isEqualTo(1)
        assertThat(state.longest).isEqualTo(1)
    }
}
