package com.anadcurukovic.berylux.feature.profile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.NameProblem
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.anadcurukovic.berylux.core.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 120, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(FakeChallengeRepository(), profiles)
    private val clock = GameClock(FakeTimeSource(), FakeSettingsRepository())

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        ProfileViewModel(profiles, cases, clock, saved)

    private fun TestScope.collecting(vm: ProfileViewModel): ProfileViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun content(vm: ProfileViewModel) = vm.uiState.value as ProfileUiState.Content

    private suspend fun stored() = profiles.profile.first()!!

    @Test
    fun `the file opens with the stored name and sigil`() = runTest {
        val vm = collecting(viewModel())
        assertThat(vm.name).isEqualTo("Sam")
        assertThat(content(vm).sigilSeed).isEqualTo(42)
    }

    @Test
    fun `a new sigil and name are filed together, and the pick seed stays`() = runTest {
        val vm = collecting(viewModel())
        vm.onNameChange("  Sam   Spade ")
        vm.onDrawAnother()
        val drawn = content(vm).sigilSeed
        assertThat(drawn).isNotEqualTo(42)

        vm.eventFlow.test {
            vm.onSave()
            assertThat(awaitItem()).isEqualTo(ProfileEvent.Done)
        }
        assertThat(stored().name).isEqualTo("Sam Spade")
        assertThat(stored().sigilSeed).isEqualTo(drawn)
        assertThat(stored().pickSeed).isEqualTo(42)
    }

    @Test
    fun `an empty name isn't filed and says why`() = runTest {
        val vm = collecting(viewModel())
        vm.onNameChange("   ")
        vm.onSave()
        assertThat(content(vm).problem).isEqualTo(NameProblem.EMPTY)
        assertThat(stored().name).isEqualTo("Sam")

        vm.onNameChange("Sam")
        assertThat(content(vm).problem).isNull()
    }

    @Test
    fun `a drawn sigil and a typed name survive the process being killed`() = runTest {
        val saved = SavedStateHandle()
        collecting(viewModel(saved)).apply {
            onNameChange("Spade")
            onDrawAnother()
        }
        val drawn = saved.get<Long>("sigil_seed")

        val restored = collecting(viewModel(saved))
        assertThat(restored.name).isEqualTo("Spade")
        assertThat(content(restored).sigilSeed).isEqualTo(drawn)
    }
}
