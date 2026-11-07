package com.anadcurukovic.berylux.feature.profile.welcome

import androidx.lifecycle.SavedStateHandle
import com.anadcurukovic.berylux.core.domain.CreateGuestUseCase
import com.anadcurukovic.berylux.core.domain.NameProblem
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class WelcomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val profiles = FakeProfileRepository()

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        WelcomeViewModel(CreateGuestUseCase(profiles), saved)

    @Test
    fun `an empty field isn't an error until the player tries to start`() {
        val state = viewModel().state.value
        assertThat(state.name).isEmpty()
        assertThat(state.problem).isNull()
    }

    @Test
    fun `starting without a name shows the problem and makes no profile`() = runTest {
        val vm = viewModel()
        vm.onStart()
        assertThat(vm.state.value.problem).isEqualTo(NameProblem.EMPTY)
        assertThat(profiles.profile.first()).isNull()
    }

    @Test
    fun `typing clears the problem`() {
        val vm = viewModel()
        vm.onStart()
        vm.onNameChange("S")
        assertThat(vm.state.value.problem).isNull()
    }

    @Test
    fun `drawing another sigil changes the seed`() {
        val vm = viewModel()
        val first = vm.state.value.seed
        vm.onDrawAnother()
        assertThat(vm.state.value.seed).isNotEqualTo(first)
    }

    @Test
    fun `starting makes the profile with the cleaned name and the sigil on screen`() = runTest {
        val vm = viewModel()
        vm.onNameChange("  Sam   Spade ")
        val seed = vm.state.value.seed
        vm.onStart()
        val profile = profiles.profile.first()!!
        assertThat(profile.name).isEqualTo("Sam Spade")
        assertThat(profile.pickSeed).isEqualTo(seed)
    }

    @Test
    fun `the name and the sigil survive the process being killed`() {
        val saved = SavedStateHandle()
        val before = viewModel(saved).apply { onNameChange("Sam") }.state.value
        val after = viewModel(saved).state.value
        assertThat(after.name).isEqualTo("Sam")
        assertThat(after.seed).isEqualTo(before.seed)
    }
}
