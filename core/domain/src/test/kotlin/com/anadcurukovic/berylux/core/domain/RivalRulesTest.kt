package com.anadcurukovic.berylux.core.domain

import app.cash.turbine.test
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.Rival
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

class RivalRulesTest {
    private val days = (0L until 200L).map { LocalDate.of(2026, 1, 1).plusDays(it) }

    private fun lux(rival: Rival, seed: Long = 42) = days.map { RivalRules.luxOn(rival, it, seed) }

    @Test
    fun `the same seed and day always give the same Lux, another seed other Lux`() {
        Rival.entries.forEach { assertThat(lux(it)).isEqualTo(lux(it)) }
        assertThat(lux(Rival.LEDGER, seed = 42)).isNotEqualTo(lux(Rival.LEDGER, seed = 43))
    }

    @Test
    fun `each bot keeps to its habit`() {
        assertThat(lux(Rival.LEDGER).all { it in 40..70 }).isTrue()
        assertThat(lux(Rival.NIGHTJAR).all { it in 30..90 }).isTrue()

        val sprints = lux(Rival.FLASHBULB)
        assertThat(sprints.all { it in 0..20 || it in 120..200 }).isTrue()
        assertThat(sprints.count { it >= 120 }).isIn(30..90) // about 30% of 200 days

        val slacks = lux(Rival.SNOOZE)
        assertThat(slacks.all { it == 0 || it in 10..40 }).isTrue()
        assertThat(slacks.count { it == 0 }).isIn(70..130) // about half

        val grinds = lux(Rival.GRINDSTONE)
        assertThat(grinds.all { it == 0 || it in 70..110 }).isTrue()
        assertThat(grinds.count { it == 0 }).isIn(15..45) // about one day in seven
    }

    @Test
    fun `during the day bots grow toward their total, the night owl only after the middle`() {
        val day = days.first()
        val total = RivalRules.luxOn(Rival.LEDGER, day, 42)
        assertThat(RivalRules.luxSoFar(Rival.LEDGER, day, 42, 0.0)).isEqualTo(0)
        assertThat(RivalRules.luxSoFar(Rival.LEDGER, day, 42, 0.5)).isEqualTo(total / 2)
        assertThat(RivalRules.luxSoFar(Rival.LEDGER, day, 42, 1.0)).isEqualTo(total)

        val owl = RivalRules.luxOn(Rival.NIGHTJAR, day, 42)
        assertThat(RivalRules.luxSoFar(Rival.NIGHTJAR, day, 42, 0.5)).isEqualTo(0)
        assertThat(RivalRules.luxSoFar(Rival.NIGHTJAR, day, 42, 0.75)).isEqualTo(owl / 2)
        assertThat(RivalRules.luxSoFar(Rival.NIGHTJAR, day, 42, 1.0)).isEqualTo(owl)
    }

    @Test
    fun `weeks start on Monday`() {
        assertThat(RivalRules.weekStart(LocalDate.of(2026, 11, 6))).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(RivalRules.weekStart(LocalDate.of(2026, 11, 2))).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(RivalRules.weekStart(LocalDate.of(2026, 11, 8))).isEqualTo(LocalDate.of(2026, 11, 2))
    }

    @Test
    fun `the day fraction runs from the start hour to the next one`() {
        val zone = ZoneId.of("Europe/Sarajevo")
        val day = LocalDate.of(2026, 11, 6)
        // 16:00 local is 12 hours after 04:00.
        assertThat(RivalRules.dayFraction(Instant.parse("2026-11-06T15:00:00Z"), zone, day, 4)).isWithin(1e-9).of(0.5)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RivalBoardUseCaseTest {
    private val zone = ZoneId.of("Europe/Sarajevo")

    // Friday 6 November 2026, 15:00 local: the week started Monday the 2nd.
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"), zone)
    private val today = LocalDate.of(2026, 11, 6)
    private val challenges = FakeChallengeRepository(
        categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
        challenges = (1L..3L).map { Challenge(it, 1, "Body $it", "", Difficulty.HARD) },
    )

    private fun board(startedOn: Instant): Pair<RivalBoardUseCase, FakeCaseRepository> {
        val profiles = FakeProfileRepository(Profile(1, "Sam", lux = 0, pickSeed = 42, createdAt = startedOn))
        val cases = FakeCaseRepository(challenges, profiles)
        return RivalBoardUseCase(profiles, cases, FakeSettingsRepository(), time) to cases
    }

    @Test
    fun `the board has you and the five bots, highest first, with your Lux from this week`() = runTest {
        val (useCase, cases) = board(startedOn = Instant.parse("2026-01-01T12:00:00Z"))
        val thisWeek = cases.accept(1, 1, isDaily = false, at = time.now(), on = today)
        cases.close(thisWeek, time.now(), today, luxEarned = 50)
        val lastWeek = cases.accept(1, 2, isDaily = false, at = time.now(), on = today.minusDays(7))
        cases.close(lastWeek, time.now(), today.minusDays(7), luxEarned = 50)

        useCase().test {
            val result = awaitItem()
            assertThat(result.weekStart).isEqualTo(LocalDate.of(2026, 11, 2))
            assertThat(result.rows).hasSize(6)
            assertThat(result.rows.single { it.isYou }.lux).isEqualTo(50)
            assertThat(result.rows.map { it.lux }).isInOrder(Comparator.reverseOrder<Int>())
            assertThat(result.lastWeekPlace).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bots count only from the day you started, and there's no last week yet`() = runTest {
        // Started this morning: the bots have only today's share so far.
        val (useCase, _) = board(startedOn = Instant.parse("2026-11-06T06:00:00Z"))
        useCase().test {
            val result = awaitItem()
            assertThat(result.lastWeekPlace).isNull()
            val fraction = RivalRules.dayFraction(time.now(), zone, today, 4)
            result.rows.filter { !it.isYou }.forEach { row ->
                assertThat(row.lux).isEqualTo(RivalRules.luxSoFar(row.rival!!, today, 42, fraction))
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the bots' Lux grows as the minutes pass`() = runTest {
        val (useCase, _) = board(startedOn = Instant.parse("2026-11-06T03:00:00Z"))
        useCase(tick = 1.minutes).test {
            val before = awaitItem().rows.filter { !it.isYou }.sumOf { it.lux }
            time.instant = Instant.parse("2026-11-06T20:00:00Z") // 21:00 local
            testScheduler.advanceTimeBy(1.minutes.inWholeMilliseconds + 1)
            val after = awaitItem().rows.filter { !it.isYou }.sumOf { it.lux }
            assertThat(after).isGreaterThan(before)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
