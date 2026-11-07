package com.anadcurukovic.berylux.feature.editor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.anadcurukovic.berylux.core.domain.ChallengeProblem
import com.anadcurukovic.berylux.core.domain.SaveCustomChallengeUseCase
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
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
class EditorViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val challenges = FakeChallengeRepository(
        challenges = listOf(
            Challenge(1, 1, "Brisk walk", "", Difficulty.EASY),
            Challenge(2, MY_CASES_CATEGORY_ID, "Read a poem", "Out loud.", Difficulty.EASY, isCustom = true),
        ),
    )
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 0, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(challenges, profiles)

    private fun editor(id: Long?, saved: SavedStateHandle = SavedStateHandle()) = EditorViewModel(
        challengeId = id,
        challenges = challenges,
        profiles = profiles,
        cases = cases,
        save = SaveCustomChallengeUseCase(challenges),
        savedState = saved,
    )

    private fun TestScope.collecting(vm: EditorViewModel): EditorViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun form(vm: EditorViewModel) = vm.uiState.value as EditorUiState.Form

    @Test
    fun `a new form is empty and shows no problems before a try`() = runTest {
        val vm = collecting(editor(null))
        assertThat(form(vm).isNew).isTrue()
        assertThat(form(vm).title).isEmpty()
        assertThat(form(vm).problems).isEmpty()
    }

    @Test
    fun `saving a bad form shows its problems, and fixing them clears them`() = runTest {
        val vm = collecting(editor(null))
        vm.onSave()
        assertThat(form(vm).problems).containsExactly(ChallengeProblem.TITLE_EMPTY)
        vm.onTitleChange("Read a poem")
        assertThat(form(vm).problems).isEmpty()
        assertThat(challenges.challenges.first()).hasSize(2)
    }

    @Test
    fun `saving a new challenge puts it in My cases and leaves the editor`() = runTest {
        val vm = collecting(editor(null))
        vm.onTitleChange("Call grandma")
        vm.onDescriptionChange("Ten minutes at least.")
        vm.eventFlow.test {
            vm.onSave()
            assertThat(awaitItem()).isEqualTo(EditorEvent.Done)
        }
        val saved = challenges.challenges.first().single { it.title == "Call grandma" }
        assertThat(saved.isCustom).isTrue()
        assertThat(saved.categoryId).isEqualTo(MY_CASES_CATEGORY_ID)
    }

    @Test
    fun `editing fills the form from the challenge and saves the change`() = runTest {
        val vm = collecting(editor(2))
        assertThat(form(vm).isNew).isFalse()
        assertThat(form(vm).title).isEqualTo("Read a poem")
        assertThat(form(vm).description).isEqualTo("Out loud.")

        vm.onTitleChange("Read two poems")
        vm.eventFlow.test {
            vm.onSave()
            assertThat(awaitItem()).isEqualTo(EditorEvent.Done)
        }
        assertThat(challenges.challenge(2)!!.title).isEqualTo("Read two poems")
    }

    @Test
    fun `a built-in challenge can't be opened in the editor`() = runTest {
        val vm = collecting(editor(1))
        assertThat(vm.uiState.value).isEqualTo(EditorUiState.NotFound)
    }

    @Test
    fun `what was typed survives the process being killed, not overwritten by the stored one`() = runTest {
        val saved = SavedStateHandle()
        collecting(editor(2, saved)).onTitleChange("Read three poems")
        val restored = collecting(editor(2, saved))
        assertThat(form(restored).title).isEqualTo("Read three poems")
    }

    @Test
    fun `deleting asks first, counts the cases that go with it, then leaves`() = runTest {
        val day = LocalDate.of(2026, 11, 6)
        cases.accept(1, 2, isDaily = false, at = Instant.EPOCH, on = day)
        val closed = cases.accept(1, 2, isDaily = false, at = Instant.EPOCH, on = day.minusDays(1))
        cases.close(closed, at = Instant.EPOCH, on = day.minusDays(1), luxEarned = 0)
        val vm = collecting(editor(2))
        vm.onDelete()
        assertThat(form(vm).confirmDelete).isTrue()
        assertThat(form(vm).caseCount).isEqualTo(2)
        assertThat(form(vm).closedCount).isEqualTo(1)

        vm.eventFlow.test {
            vm.onConfirmDelete()
            assertThat(awaitItem()).isEqualTo(EditorEvent.Done)
        }
        assertThat(challenges.challenge(2)).isNull()
        assertThat(cases.caseFiles(1).first()).isEmpty()
    }

    @Test
    fun `saying no to the delete keeps the challenge`() = runTest {
        val vm = collecting(editor(2))
        vm.onDelete()
        vm.onDismissDelete()
        assertThat(form(vm).confirmDelete).isFalse()
        assertThat(challenges.challenge(2)).isNotNull()
    }
}
