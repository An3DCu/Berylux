package com.anadcurukovic.berylux.core.domain

import app.cash.turbine.test
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.UserSettings
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

class SigilTest {
    @Test
    fun `the same seed always gives the same sigil`() {
        assertThat(Sigil.from(1234)).isEqualTo(Sigil.from(1234))
    }

    @Test
    fun `every row is a mirror image`() {
        (0L until 50L).map(Sigil::from).forEach { sigil ->
            for (row in 0 until Sigil.SIZE) {
                assertThat(sigil.isOn(row, 0)).isEqualTo(sigil.isOn(row, 4))
                assertThat(sigil.isOn(row, 1)).isEqualTo(sigil.isOn(row, 3))
            }
        }
    }

    @Test
    fun `a sigil is never nearly empty or nearly full`() {
        (0L until 200L).map(Sigil::from).forEach { assertThat(it.cells.count { on -> on }).isIn(6..19) }
    }

    @Test
    fun `different seeds give different sigils`() {
        assertThat((0L until 50L).map(Sigil::from).toSet().size).isGreaterThan(40)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameClockTest {
    // 14:00 UTC is 15:00 in Sarajevo in November.
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val settings = FakeSettingsRepository()
    private val clock = GameClock(time, settings)

    @Test
    fun `today is the game day on the phone's clock`() = runTest {
        assertThat(clock.today()).isEqualTo(LocalDate.of(2026, 11, 6))
    }

    @Test
    fun `a new day comes through when 4 AM passes`() = runTest {
        clock.days(tick = 1.minutes).test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 11, 6))
            time.instant = Instant.parse("2026-11-07T02:59:00Z") // 03:59 local: still the 6th
            testScheduler.advanceTimeBy(1.minutes.inWholeMilliseconds + 1)
            expectNoEvents()
            time.instant = Instant.parse("2026-11-07T03:00:00Z") // 04:00 local
            testScheduler.advanceTimeBy(1.minutes.inWholeMilliseconds)
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 11, 7))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moving the start hour can change the day at once`() = runTest {
        time.instant = Instant.parse("2026-11-07T04:30:00Z") // 05:30 local
        clock.days().test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 11, 7))
            settings.setDayStartHour(6)
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 11, 6))
            cancelAndIgnoreRemainingEvents()
        }
    }
}

class NameRulesTest {
    @Test
    fun `names are trimmed and inner spaces squeezed`() {
        assertThat(NameRules.clean("  Sam   Spade ")).isEqualTo("Sam Spade")
    }

    @Test
    fun `an empty name or one over 20 characters is refused`() {
        assertThat(NameRules.problem("   ")).isEqualTo(NameProblem.EMPTY)
        assertThat(NameRules.problem("a".repeat(21))).isEqualTo(NameProblem.TOO_LONG)
        assertThat(NameRules.problem("a".repeat(20))).isNull()
        assertThat(NameRules.problem("  " + "a".repeat(20) + "  ")).isNull()
    }

    @Test
    fun `the guest is stored with the cleaned name and the chosen seed`() = runTest {
        val profiles = FakeProfileRepository()
        val created = CreateGuestUseCase(profiles)("  Sam   Spade ", pickSeed = 99)
        assertThat(created.name).isEqualTo("Sam Spade")
        assertThat(profiles.profile.first()!!.pickSeed).isEqualTo(99)
    }

    @Test
    fun `a bad name never reaches the repository`() = runTest {
        val profiles = FakeProfileRepository()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { CreateGuestUseCase(profiles)(" ", pickSeed = 1) }
        }
        assertThat(profiles.profile.first()).isNull()
    }
}

class AcceptCaseUseCaseTest {
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val today = LocalDate.of(2026, 11, 6)
    private val challenges = FakeChallengeRepository(
        categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
        challenges = (1L..8L).map { Challenge(it, 1, "Challenge $it", "", Difficulty.EASY) },
    )
    private val cases = FakeCaseRepository(challenges)
    private val profiles = FakeProfileRepository()
    private val accept = AcceptCaseUseCase(
        profiles = profiles,
        cases = cases,
        clock = GameClock(time, FakeSettingsRepository(UserSettings())),
        time = time,
    )

    @Test
    fun `without a profile nothing is taken`() = runTest {
        assertThat(accept(1, isDaily = false)).isEqualTo(AcceptResult.NoProfile)
    }

    @Test
    fun `a case is taken for today's game day`() = runTest {
        val profile = profiles.createGuest("Sam", pickSeed = 1)
        val result = accept(3, isDaily = true)
        assertThat(result).isInstanceOf(AcceptResult.Accepted::class.java)
        val case = cases.caseFiles(profile.id).first().single().case
        assertThat(case.acceptedOn).isEqualTo(today)
        assertThat(case.isDaily).isTrue()
    }

    @Test
    fun `the same challenge can't be taken twice in a day, unless it was dropped`() = runTest {
        profiles.createGuest("Sam", pickSeed = 1)
        val first = accept(3, isDaily = false) as AcceptResult.Accepted
        assertThat(accept(3, isDaily = false)).isEqualTo(AcceptResult.AlreadyTaken)
        cases.drop(first.caseId)
        assertThat(accept(3, isDaily = false)).isInstanceOf(AcceptResult.Accepted::class.java)
    }

    @Test
    fun `the sixth case of the day is refused`() = runTest {
        profiles.createGuest("Sam", pickSeed = 1)
        (1L..5L).forEach { assertThat(accept(it, isDaily = false)).isInstanceOf(AcceptResult.Accepted::class.java) }
        assertThat(accept(6, isDaily = false)).isEqualTo(AcceptResult.DayFull)
    }
}
