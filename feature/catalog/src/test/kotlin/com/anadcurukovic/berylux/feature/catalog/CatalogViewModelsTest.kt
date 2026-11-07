package com.anadcurukovic.berylux.feature.catalog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.anadcurukovic.berylux.core.domain.AcceptCaseUseCase
import com.anadcurukovic.berylux.core.domain.DailyPicker
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.LuxRules
import com.anadcurukovic.berylux.core.domain.TodaysCases
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
import com.anadcurukovic.berylux.core.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 6)
    private val categoryList = listOf(
        Category(MY_CASES_CATEGORY_ID, "My cases", "", CategoryKind.CUSTOM),
        Category(2, "Mind", "", CategoryKind.REGULAR),
        Category(1, "Body", "", CategoryKind.REGULAR),
        Category(7, "Long shots", "", CategoryKind.BONUS),
    )
    private val challengeList =
        (1L..3L).map { Challenge(it, 1, "Body easy $it", "", Difficulty.EASY) } +
            (4L..6L).map { Challenge(it, 1, "Body hard $it", "", Difficulty.HARD) } +
            (7L..8L).map { Challenge(it, 2, "Mind $it", "", Difficulty.MEDIUM) } +
            Challenge(9, 7, "Long shot", "", Difficulty.HARD) +
            Challenge(10, MY_CASES_CATEGORY_ID, "Read a poem", "", Difficulty.EASY, isCustom = true)
    private val challenges = FakeChallengeRepository(categoryList, challengeList)
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 0, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(challenges, profiles)

    // 14:00 UTC is 15:00 in Sarajevo: the game day is 6 November.
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val clock = GameClock(time, FakeSettingsRepository())

    /** The same daily case Today shows for this profile and day. */
    private val dailyId = DailyPicker.pick(challengeList, categoryList, today, 42).daily!!.id

    private fun category(id: Long, saved: SavedStateHandle = SavedStateHandle()) = CategoryViewModel(
        categoryId = id,
        challenges = challenges,
        todaysCases = TodaysCases(profiles, cases, clock),
        acceptCase = AcceptCaseUseCase(profiles, cases, clock, time),
        savedState = saved,
    )

    private fun <T> TestScope.collecting(flow: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
        return flow
    }

    private fun content(vm: CategoryViewModel) = vm.uiState.value as CategoryUiState.Content

    @Test
    fun `the catalog lists the built-in categories by id, My cases last, with their counts`() = runTest {
        val state = collecting(CatalogViewModel(challenges).uiState).value as CatalogUiState.Content
        assertThat(state.categories.map { it.category.name })
            .containsExactly("Body", "Mind", "Long shots", "My cases").inOrder()
        assertThat(state.categories.map { it.count }).containsExactly(6, 2, 1, 1).inOrder()
    }

    @Test
    fun `a category shows its challenges with their Lux, and the daily case only on Today`() = runTest {
        val dailyCategory = challengeList.single { it.id == dailyId }.categoryId
        val vm = category(dailyCategory)
        collecting(vm.uiState)
        val items = content(vm).challenges
        assertThat(items.single { it.challenge.id == dailyId }.offer).isEqualTo(Offer.ON_TODAY)
        assertThat(items.filter { it.challenge.id != dailyId }.map { it.offer }.toSet()).containsExactly(Offer.TAKE)
        items.forEach { assertThat(it.lux).isEqualTo(LuxRules.base(it.challenge.difficulty)) }
    }

    @Test
    fun `the difficulty filter narrows the list and survives the process being killed`() = runTest {
        val saved = SavedStateHandle()
        val vm = category(1, saved)
        collecting(vm.uiState)
        vm.onDifficulty(Difficulty.HARD)
        assertThat(content(vm).challenges.map { it.challenge.id }).containsExactly(4L, 5L, 6L)

        val restored = category(1, saved)
        collecting(restored.uiState)
        assertThat(content(restored).difficulty).isEqualTo(Difficulty.HARD)
        assertThat(content(restored).challenges).hasSize(3)
    }

    @Test
    fun `taking a challenge makes it a case for today and marks it taken`() = runTest {
        val vm = category(2)
        collecting(vm.uiState)
        val mind = content(vm).challenges.first { it.offer == Offer.TAKE }
        vm.messageFlow.test {
            vm.onTake(mind)
            assertThat(awaitItem()).isEqualTo(CategoryMessage.Taken(mind.challenge.title))
        }
        assertThat(content(vm).challenges.single { it.challenge.id == mind.challenge.id }.offer).isEqualTo(Offer.TAKEN)
    }

    @Test
    fun `the sixth case of the day is refused`() = runTest {
        (1L..6L).filter { it != dailyId }.take(5).forEach {
            cases.accept(1, it, isDaily = false, at = time.now(), on = today)
        }
        val vm = category(2)
        collecting(vm.uiState)
        vm.messageFlow.test {
            vm.onTake(content(vm).challenges.first { it.offer == Offer.TAKE })
            assertThat(awaitItem()).isEqualTo(CategoryMessage.DayFull)
        }
    }

    @Test
    fun `My cases has no filter and its challenges can be taken`() = runTest {
        val saved = SavedStateHandle().apply { set("difficulty", Difficulty.HARD.name) }
        val vm = category(MY_CASES_CATEGORY_ID, saved)
        collecting(vm.uiState)
        assertThat(content(vm).isMyCases).isTrue()
        assertThat(content(vm).difficulty).isNull()
        assertThat(content(vm).challenges.single().offer).isEqualTo(Offer.TAKE)
        assertThat(content(vm).challenges.single().lux).isEqualTo(0)
    }

    @Test
    fun `a category that isn't there says so`() = runTest {
        val vm = category(99)
        collecting(vm.uiState)
        assertThat(vm.uiState.value).isEqualTo(CategoryUiState.NotFound)
    }
}
