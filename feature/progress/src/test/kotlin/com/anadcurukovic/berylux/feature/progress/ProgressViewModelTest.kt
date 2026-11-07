package com.anadcurukovic.berylux.feature.progress

import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.model.Achievement
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.GemRank
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.testing.FakeAchievementRepository
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.anadcurukovic.berylux.core.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 6)
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val challenges = FakeChallengeRepository(
        categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
        challenges = (1L..3L).map { Challenge(it, 1, "Body $it", "", Difficulty.EASY) },
    )
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 640, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(challenges, profiles)
    private val achievements = FakeAchievementRepository()
    private val settings = FakeSettingsRepository()

    private val viewModel by lazy {
        ProgressViewModel(profiles, cases, achievements, GameClock(time, settings), settings, time)
    }

    private fun TestScope.keepCollecting() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun content() = viewModel.uiState.value as ProgressUiState.Content

    @Test
    fun `progress shows the rank, the streak and all 17 achievements`() = runTest {
        listOf(today.minusDays(1), today).forEachIndexed { index, day ->
            val id = cases.accept(1, index + 1L, isDaily = false, at = time.now(), on = day)
            cases.close(id, time.now(), day, luxEarned = 0)
        }
        achievements.unlock(1, Achievement.FIRST_CLOSED.key, time.now())
        keepCollecting()

        assertThat(content().rank.rank).isEqualTo(GemRank.HELIODOR)
        assertThat(content().streak.current).isEqualTo(2)
        assertThat(content().streak.doneToday).isTrue()
        assertThat(content().achievements).hasSize(17)
        assertThat(content().unlockedCount).isEqualTo(1)
        assertThat(content().achievements.first { it.achievement == Achievement.FIRST_CLOSED }.unlockedAt)
            .isEqualTo(time.now())
    }

    @Test
    fun `unlocks since the last look are new on this visit, and the look is recorded`() = runTest {
        settings.markAchievementsSeen(Instant.parse("2026-11-06T10:00:00Z"))
        achievements.unlock(1, Achievement.FIRST_CLOSED.key, Instant.parse("2026-11-06T09:00:00Z"))
        achievements.unlock(1, Achievement.CLOSED_10.key, Instant.parse("2026-11-06T12:00:00Z"))
        keepCollecting()

        // Nothing is marked until the screen is shown.
        assertThat(content().achievements.none { it.isNew }).isTrue()
        viewModel.onShown()
        val new = content().achievements.filter { it.isNew }.map { it.achievement }
        assertThat(new).containsExactly(Achievement.CLOSED_10)
        assertThat(settings.achievementsSeenAt.first()).isEqualTo(time.now())
    }

    @Test
    fun `a locked achievement has no date`() = runTest {
        keepCollecting()
        assertThat(content().achievements.all { it.unlockedAt == null && !it.isNew }).isTrue()
    }
}
