package com.anadcurukovic.berylux.feature.rivals

import com.anadcurukovic.berylux.core.domain.RivalBoardUseCase
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.Rival
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.anadcurukovic.berylux.core.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RivalsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `the board shows you and every bot`() = runTest {
        val profiles = FakeProfileRepository(
            Profile(id = 1, name = "Sam", lux = 0, pickSeed = 42, createdAt = Instant.parse("2026-01-01T12:00:00Z")),
        )
        val challenges = FakeChallengeRepository()
        val board = RivalBoardUseCase(
            profiles,
            FakeCaseRepository(challenges, profiles),
            FakeSettingsRepository(),
            FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z")),
        )
        val viewModel = RivalsViewModel(board)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val content = viewModel.uiState.value as RivalsUiState.Content
        assertThat(content.board.rows.mapNotNull { it.rival }).containsExactlyElementsIn(Rival.entries)
        assertThat(content.board.rows.count { it.isYou }).isEqualTo(1)
        assertThat(content.board.place).isIn(1..6)
    }
}
