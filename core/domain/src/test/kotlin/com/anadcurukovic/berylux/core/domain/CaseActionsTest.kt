package com.anadcurukovic.berylux.core.domain

import app.cash.turbine.test
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class CaseActionsTest {
    // 14:00 UTC is 15:00 in Sarajevo: the game day is 6 November.
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val today = LocalDate.of(2026, 11, 6)
    private val clock = GameClock(time, FakeSettingsRepository())
    private val challenges = FakeChallengeRepository(
        categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
        challenges = listOf(
            Challenge(1, 1, "Brisk walk", "", Difficulty.EASY),
            Challenge(2, 1, "Plank set", "", Difficulty.MEDIUM),
            Challenge(3, MY_CASES_CATEGORY_ID, "Read a poem", "", Difficulty.HARD, isCustom = true),
        ),
    )
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 100, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(challenges, profiles)
    private val actions = CaseActions(profiles, cases, clock, time)

    private suspend fun take(challengeId: Long, isDaily: Boolean = false, on: LocalDate = today): CaseFile {
        val id = cases.accept(1, challengeId, isDaily, at = time.now(), on = on)
        return cases.caseFiles(1).first().single { it.case.id == id }
    }

    private suspend fun lux() = profiles.profile.first()!!.lux

    private suspend fun status(file: CaseFile) =
        cases.caseFiles(1).first().single { it.case.id == file.case.id }.case.status

    @Test
    fun `closing pays the case's Lux, once`() = runTest {
        val file = take(2)
        assertThat(actions.close(file)).isEqualTo(CloseResult.Closed(25))
        assertThat(lux()).isEqualTo(125)
        assertThat(actions.close(file)).isEqualTo(CloseResult.NotOpen)
        assertThat(lux()).isEqualTo(125)
    }

    @Test
    fun `the daily case pays one and a half times, a custom case pays nothing`() = runTest {
        assertThat(actions.close(take(2, isDaily = true))).isEqualTo(CloseResult.Closed(38))
        assertThat(actions.close(take(3))).isEqualTo(CloseResult.Closed(0))
        assertThat(lux()).isEqualTo(138)
    }

    @Test
    fun `a closed case is closed on today's game day`() = runTest {
        val file = take(1)
        actions.close(file)
        val closed = cases.caseFiles(1).first().single().case
        assertThat(closed.closedOn).isEqualTo(today)
        assertThat(closed.luxEarned).isEqualTo(10)
    }

    @Test
    fun `dropping costs 5 Lux and frees the slot`() = runTest {
        val file = take(1)
        assertThat(actions.drop(file)).isEqualTo(DropResult.Dropped(5))
        assertThat(lux()).isEqualTo(95)
        assertThat(DailyCap.usedOn(today, cases.caseFiles(1).first().map { it.case })).isEqualTo(0)
        assertThat(actions.drop(file)).isEqualTo(DropResult.NotOpen)
        assertThat(lux()).isEqualTo(95)
    }

    @Test
    fun `dropping never takes Lux below 0 and says what it really cost`() = runTest {
        profiles.addLux(1, -97)
        assertThat(actions.drop(take(1))).isEqualTo(DropResult.Dropped(3))
        assertThat(lux()).isEqualTo(0)
    }

    @Test
    fun `a case from an earlier day can't be closed or dropped, it goes cold`() = runTest {
        val yesterdays = take(1, on = today.minusDays(1))
        val other = take(2, on = today.minusDays(1))
        assertThat(actions.close(yesterdays)).isEqualTo(CloseResult.WentCold)
        assertThat(status(yesterdays)).isEqualTo(CaseStatus.COLD)
        assertThat(status(other)).isEqualTo(CaseStatus.COLD)
        assertThat(actions.drop(other)).isEqualTo(DropResult.WentCold)
        assertThat(lux()).isEqualTo(100)
    }

    @Test
    fun `nothing happens without a profile`() = runTest {
        val file = take(1)
        val noProfile = FakeProfileRepository()
        val noActions = CaseActions(noProfile, cases, clock, time)
        assertThat(noActions.close(file)).isEqualTo(CloseResult.NoProfile)
        assertThat(noActions.drop(file)).isEqualTo(DropResult.NoProfile)
    }

    @Test
    fun `open cases go cold at the start and again when the day ends`() = runTest {
        take(1, on = today.minusDays(1))
        val todays = take(2)
        MarkColdCasesUseCase(profiles, cases, clock)().test {
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(status(todays)).isEqualTo(CaseStatus.OPEN)

            time.instant = Instant.parse("2026-11-07T03:00:00Z") // 04:00 local on the 7th
            testScheduler.advanceTimeBy(1.minutes.inWholeMilliseconds + 1)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(status(todays)).isEqualTo(CaseStatus.COLD)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the close that makes a seven-day streak brings the milestone, a second one that day doesn't`() = runTest {
        // Six days in a row closed before today.
        for (daysAgo in 6L downTo 1L) {
            val day = today.minusDays(daysAgo)
            val id = cases.accept(1, 1, isDaily = false, at = time.now(), on = day)
            cases.close(id, at = time.now(), on = day, luxEarned = 10)
        }
        assertThat(actions.close(take(1))).isEqualTo(CloseResult.Closed(10, Narration.Milestone(7)))
        assertThat(actions.close(take(2))).isEqualTo(CloseResult.Closed(25, Narration.Close))
    }

    @Test
    fun `the close that reaches a new rank brings the rank-up`() = runTest {
        // Heliodor starts at 500 Lux; a Medium case pays 25.
        val nearRank = FakeProfileRepository(
            Profile(id = 1, name = "Sam", lux = 490, pickSeed = 42, createdAt = Instant.EPOCH),
        )
        val nearCases = FakeCaseRepository(challenges, nearRank)
        val nearActions = CaseActions(nearRank, nearCases, clock, time)
        val id = nearCases.accept(1, 2, isDaily = false, at = time.now(), on = today)
        val file = nearCases.caseFiles(1).first().single { it.case.id == id }
        assertThat(nearActions.close(file)).isEqualTo(CloseResult.Closed(25, Narration.RankUp))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodaysCasesTest {
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val challenges = FakeChallengeRepository(
        challenges = listOf(Challenge(1, 1, "Brisk walk", "", Difficulty.EASY)),
    )
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 0, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(challenges, profiles)
    private val todaysCases = TodaysCases(profiles, cases, GameClock(time, FakeSettingsRepository()))

    @Test
    fun `today follows new cases and moves to the next day at 4 AM`() = runTest {
        val today = LocalDate.of(2026, 11, 6)
        todaysCases().test {
            assertThat(awaitItem().cases).isEmpty()
            cases.accept(1, 1, isDaily = false, at = time.now(), on = today)
            assertThat(awaitItem().cases).hasSize(1)

            time.instant = Instant.parse("2026-11-07T03:00:00Z") // 04:00 local on the 7th
            testScheduler.advanceTimeBy(1.minutes.inWholeMilliseconds + 1)
            val next = awaitItem()
            assertThat(next.day).isEqualTo(today.plusDays(1))
            assertThat(next.cases).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
