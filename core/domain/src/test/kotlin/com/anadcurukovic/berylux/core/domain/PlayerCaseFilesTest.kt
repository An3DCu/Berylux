package com.anadcurukovic.berylux.core.domain

import app.cash.turbine.test
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PlayerCaseFilesTest {
    private val challenges = FakeChallengeRepository(
        categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
        challenges = (1L..3L).map { Challenge(it, 1, "Body $it", "", Difficulty.EASY) },
    )
    private val profiles = FakeProfileRepository()
    private val cases = FakeCaseRepository(challenges, profiles)
    private val day = LocalDate.of(2026, 11, 12)

    @Test
    fun `nothing comes before there's a player, then every case on any day`() = runTest {
        PlayerCaseFiles(profiles, cases)().test {
            expectNoEvents()

            val profile = profiles.createGuest(name = "Sam", pickSeed = 42)
            assertThat(awaitItem()).isEmpty()

            cases.accept(profile.id, 1, isDaily = true, at = Instant.EPOCH, on = day.minusDays(1))
            assertThat(awaitItem().map { it.challenge.id }).containsExactly(1L)

            cases.accept(profile.id, 2, isDaily = false, at = Instant.EPOCH, on = day)
            assertThat(awaitItem().map { it.challenge.id }).containsExactly(1L, 2L)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
