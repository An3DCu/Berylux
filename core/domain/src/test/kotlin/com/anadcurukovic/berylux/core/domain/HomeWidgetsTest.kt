package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Noon in Sarajevo on 12 November 2026, with the day starting at 4 AM: the game day is the 12th. */
class HomeWidgetsTest {
    private val categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR))
    private val list = (1L..6L).map { Challenge(it, 1, "Body $it", "", Difficulty.MEDIUM) }
    private val challenges = FakeChallengeRepository(categories = categories, challenges = list)
    private val profiles = FakeProfileRepository()
    private val cases = FakeCaseRepository(challenges, profiles)
    private val time = FakeTimeSource(Instant.parse("2026-11-12T11:00:00Z"))
    private val widgets = HomeWidgetsUseCase(profiles, challenges, cases, GameClock(time, FakeSettingsRepository()))
    private val day = LocalDate.of(2026, 11, 12)

    // Sam's daily case on the 12th, picked the way Today picks it.
    private val daily = DailyPicker.pick(list, categories, day, profileSeed = 42).daily!!

    // A Medium case pays 25; the daily pays 1.5 times that, 37.5, rounded up to 38.
    private fun onDesk(standing: DailyStanding, caseId: Long? = null) =
        DailyOnDesk(daily, "Body", standing, lux = 38, caseId = caseId)

    @Test
    fun `before there's a player there's no streak and no case`() = runTest {
        assertThat(widgets().first()).isEqualTo(HomeWidgetsState(streak = 0, daily = null))
    }

    @Test
    fun `not taken, it shows what the daily case pays`() = runTest {
        profiles.createGuest(name = "Sam", pickSeed = 42)
        assertThat(widgets().first().daily).isEqualTo(onDesk(DailyStanding.NOT_TAKEN))
    }

    @Test
    fun `taken, it carries the case to close`() = runTest {
        val sam = profiles.createGuest(name = "Sam", pickSeed = 42)
        val id = cases.accept(sam.id, daily.id, isDaily = true, at = time.instant, on = day)
        assertThat(widgets().first().daily).isEqualTo(onDesk(DailyStanding.OPEN, caseId = id))
    }

    @Test
    fun `closed, it shows what the case earned`() = runTest {
        val sam = profiles.createGuest(name = "Sam", pickSeed = 42)
        val id = cases.accept(sam.id, daily.id, isDaily = true, at = time.instant, on = day)
        cases.close(id, at = time.instant, on = day, luxEarned = 38)
        assertThat(widgets().first().daily).isEqualTo(onDesk(DailyStanding.CLOSED, caseId = id))
    }

    @Test
    fun `dropped, it can be taken again`() = runTest {
        val sam = profiles.createGuest(name = "Sam", pickSeed = 42)
        val id = cases.accept(sam.id, daily.id, isDaily = true, at = time.instant, on = day)
        cases.drop(id, penalty = LuxRules.DROP_PENALTY)
        assertThat(widgets().first().daily).isEqualTo(onDesk(DailyStanding.NOT_TAKEN))
    }

    @Test
    fun `the streak counts the days before today until today is closed`() = runTest {
        val sam = profiles.createGuest(name = "Sam", pickSeed = 42)
        for (earlier in listOf(day.minusDays(2), day.minusDays(1))) {
            val id = cases.accept(sam.id, 1, isDaily = false, at = Instant.EPOCH, on = earlier)
            cases.close(id, at = Instant.EPOCH, on = earlier, luxEarned = 25)
        }
        // The 10th and the 11th closed, nothing yet on the 12th: day 2.
        assertThat(widgets().first().streak).isEqualTo(2)

        val today = cases.accept(sam.id, 2, isDaily = false, at = time.instant, on = day)
        cases.close(today, at = time.instant, on = day, luxEarned = 25)
        assertThat(widgets().first().streak).isEqualTo(3)
    }

    @Test
    fun `today ends at the start hour in force now, not at one still waiting`() = runTest {
        val settings = FakeSettingsRepository()
        val clock = GameClock(time, settings)
        // 4 AM on the 13th in Sarajevo (UTC+1) is 03:00 UTC.
        assertThat(clock.endOfToday()).isEqualTo(Instant.parse("2026-11-13T03:00:00Z"))

        // Picked 6 at noon: today still ends at 4, and the new hour starts with the 13th.
        ChangeDayStartHourUseCase(settings, time)(6)
        assertThat(clock.endOfToday()).isEqualTo(Instant.parse("2026-11-13T03:00:00Z"))
        // At 5 AM local on the 13th the 6 o'clock hour is in force, and 5 AM is before 6: still the
        // 12th's game day, which now ends at 6 AM on the 13th, 05:00 UTC.
        time.instant = Instant.parse("2026-11-13T04:00:00Z")
        assertThat(clock.endOfToday()).isEqualTo(Instant.parse("2026-11-13T05:00:00Z"))
        // At 7 AM it's the 13th, which ends at 6 AM on the 14th.
        time.instant = Instant.parse("2026-11-13T06:00:00Z")
        assertThat(clock.endOfToday()).isEqualTo(Instant.parse("2026-11-14T05:00:00Z"))
    }
}
