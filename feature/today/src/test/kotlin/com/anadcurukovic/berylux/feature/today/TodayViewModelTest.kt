package com.anadcurukovic.berylux.feature.today

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.anadcurukovic.berylux.core.domain.AcceptCaseUseCase
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.GemRank
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.anadcurukovic.berylux.core.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val categories = listOf(
        Category(1, "Body", "", CategoryKind.REGULAR),
        Category(2, "Mind", "", CategoryKind.REGULAR),
        Category(7, "Long shots", "", CategoryKind.BONUS),
    )
    private val challenges = FakeChallengeRepository(
        categories = categories,
        challenges = (1L..8L).map { Challenge(it, 1, "Body $it", "", Difficulty.EASY) } +
            (9L..16L).map { Challenge(it, 2, "Mind $it", "", Difficulty.MEDIUM) } +
            (17L..20L).map { Challenge(it, 7, "Long shot $it", "", Difficulty.HARD) },
    )
    private val cases = FakeCaseRepository(challenges)
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 600, pickSeed = 42, createdAt = Instant.EPOCH),
    )

    // 14:00 UTC is 15:00 in Sarajevo: the game day is 6 November.
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val clock = GameClock(time, FakeSettingsRepository())

    // Built on first use inside a test, after the rule has swapped in the test dispatcher.
    private val viewModel by lazy {
        TodayViewModel(
            profiles = profiles,
            challenges = challenges,
            cases = cases,
            clock = clock,
            acceptCase = AcceptCaseUseCase(profiles, cases, clock, time),
            savedState = SavedStateHandle(),
        )
    }

    private fun TestScope.keepCollecting() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun content() = viewModel.uiState.value as TodayUiState.Content

    @Test
    fun `today shows the profile, a daily case, a bonus case and four picks`() = runTest {
        keepCollecting()
        val state = content()
        assertThat(state.name).isEqualTo("Sam")
        assertThat(state.rank.rank).isEqualTo(GemRank.HELIODOR)
        assertThat(state.used).isEqualTo(0)
        assertThat(state.daily!!.isDaily).isTrue()
        assertThat(state.bonus!!.challenge.categoryId).isEqualTo(7)
        assertThat(state.bonus!!.lux).isEqualTo(50)
        assertThat(state.picks).hasSize(4)
        assertThat(state.filters.map { it.name }).containsExactly("Body", "Mind").inOrder()
    }

    @Test
    fun `the daily case shows its one and a half times Lux`() = runTest {
        keepCollecting()
        val daily = content().daily!!
        val expected = if (daily.challenge.difficulty == Difficulty.EASY) 15 else 38
        assertThat(daily.lux).isEqualTo(expected)
    }

    @Test
    fun `taking a pick marks it taken, uses a slot and says so`() = runTest {
        keepCollecting()
        val pick = content().picks.first()
        viewModel.messageFlow.test {
            viewModel.onTake(pick)
            assertThat(awaitItem()).isEqualTo(TodayMessage.Taken(pick.challenge.title))
        }
        assertThat(content().used).isEqualTo(1)
        assertThat(content().picks.first { it.challenge.id == pick.challenge.id }.taken).isTrue()
    }

    @Test
    fun `the sixth case of the day is refused with a message`() = runTest {
        keepCollecting()
        val offered = with(content()) { listOfNotNull(daily, bonus) + picks }
        viewModel.messageFlow.test {
            offered.take(5).forEach {
                viewModel.onTake(it)
                assertThat(awaitItem()).isInstanceOf(TodayMessage.Taken::class.java)
            }
            viewModel.onTake(offered[5])
            assertThat(awaitItem()).isEqualTo(TodayMessage.DayFull)
        }
        assertThat(content().dayFull).isTrue()
    }

    @Test
    fun `a filter changes the picks, not the daily or bonus case`() = runTest {
        keepCollecting()
        val before = content()
        viewModel.onFilter(2)
        val after = content()
        assertThat(after.selectedFilter).isEqualTo(2)
        assertThat(after.picks.all { it.challenge.categoryId == 2L }).isTrue()
        assertThat(after.daily).isEqualTo(before.daily)
        assertThat(after.bonus).isEqualTo(before.bonus)
    }

    @Test
    fun `at 4 AM a new day starts with all five slots free`() = runTest {
        keepCollecting()
        viewModel.onTake(content().picks.first())
        assertThat(content().used).isEqualTo(1)

        time.instant = Instant.parse("2026-11-07T03:00:00Z") // 04:00 in Sarajevo
        main.dispatcher.scheduler.advanceTimeBy(60_001)
        assertThat(content().used).isEqualTo(0)
        assertThat(content().picks.none { it.taken }).isTrue()
    }
}
