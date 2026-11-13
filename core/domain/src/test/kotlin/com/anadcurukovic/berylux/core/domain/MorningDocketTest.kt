package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Sarajevo is UTC+1 in winter and UTC+2 from the last Sunday of March. */
class MorningDocketTest {
    private val sarajevo = ZoneId.of("Europe/Sarajevo")
    private val nine = LocalTime.of(9, 0)

    @Test
    fun `before nine it comes the same morning`() {
        // 8:15 local on 12 November is 07:15 UTC; 9:00 local is 08:00 UTC.
        val next = MorningDocket.nextAt(Instant.parse("2026-11-12T07:15:00Z"), sarajevo, nine)
        assertThat(next).isEqualTo(Instant.parse("2026-11-12T08:00:00Z"))
    }

    @Test
    fun `at nine or after it comes the next morning`() {
        assertThat(MorningDocket.nextAt(Instant.parse("2026-11-12T08:00:00Z"), sarajevo, nine))
            .isEqualTo(Instant.parse("2026-11-13T08:00:00Z"))
        assertThat(MorningDocket.nextAt(Instant.parse("2026-11-12T19:30:00Z"), sarajevo, nine))
            .isEqualTo(Instant.parse("2026-11-13T08:00:00Z"))
    }

    @Test
    fun `it keeps to the phone's clock across the change to summer time`() {
        // Clocks go forward on Sunday 28 March 2027. 9:00 local that day is 07:00 UTC, not 08:00.
        val next = MorningDocket.nextAt(Instant.parse("2027-03-27T10:00:00Z"), sarajevo, nine)
        assertThat(next).isEqualTo(Instant.parse("2027-03-28T07:00:00Z"))
    }

    @Test
    fun `a time the clocks skip comes an hour later that night`() {
        // 2:00 to 3:00 doesn't exist on 28 March 2027; 2:30 becomes 3:30 summer time, 01:30 UTC.
        val next = MorningDocket.nextAt(Instant.parse("2027-03-27T22:00:00Z"), sarajevo, LocalTime.of(2, 30))
        assertThat(next).isEqualTo(Instant.parse("2027-03-28T01:30:00Z"))
    }

    @Test
    fun `before the day's start hour it names the day about to start`() {
        // 9 AM on 12 November, with the day starting at 10: the game day is still the 11th,
        // and the reminder names the 12th, the day about to start.
        val at = Instant.parse("2026-11-12T08:00:00Z")
        assertThat(GameDay.of(at, sarajevo, 10)).isEqualTo(LocalDate.of(2026, 11, 11))
        assertThat(MorningDocket.dayFor(at, sarajevo)).isEqualTo(LocalDate.of(2026, 11, 12))
    }

    @Test
    fun `after the start hour it names the game day in force`() {
        val at = Instant.parse("2026-11-12T08:00:00Z")
        assertThat(MorningDocket.dayFor(at, sarajevo)).isEqualTo(GameDay.of(at, sarajevo, 4))
    }

    @Test
    fun `it names the day's daily case, and nothing before there's a player`() = runTest {
        val challenges = FakeChallengeRepository(
            categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
            challenges = (1L..6L).map { Challenge(it, 1, "Body $it", "", Difficulty.EASY) },
        )
        val time = FakeTimeSource(Instant.parse("2026-11-12T08:00:00Z"), sarajevo)
        val profiles = FakeProfileRepository()
        val docket = MorningDocketCaseUseCase(profiles, challenges, time)
        assertThat(docket()).isNull()

        profiles.createGuest(name = "Sam", pickSeed = 42)
        val expected = DailyPicker.pick(
            challenges = (1L..6L).map { Challenge(it, 1, "Body $it", "", Difficulty.EASY) },
            categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
            day = LocalDate.of(2026, 11, 12),
            profileSeed = 42,
        ).daily
        assertThat(docket()).isEqualTo(expected)
    }
}
