package com.anadcurukovic.berylux.feature.tracker

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.anadcurukovic.berylux.core.domain.CaseActions
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.Narration
import com.anadcurukovic.berylux.core.domain.PlayerCaseFiles
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.Profile
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
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class CasesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 6)
    private val challenges = FakeChallengeRepository(
        categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR)),
        challenges = (1L..8L).map { Challenge(it, 1, "Body $it", "", Difficulty.MEDIUM) },
    )
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 100, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(challenges, profiles)

    // 14:00 UTC is 15:00 in Sarajevo: the game day is 6 November.
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val settings = FakeSettingsRepository()
    private val clock = GameClock(time, settings)
    private val saved = SavedStateHandle()

    private fun newViewModel() = CasesViewModel(
        playerCaseFiles = PlayerCaseFiles(profiles, cases),
        challenges = challenges,
        clock = clock,
        actions = CaseActions(profiles, cases, clock, time),
        settings = settings,
        savedState = saved,
    )

    // Built on first use inside a test, after the rule has swapped in the test dispatcher.
    private val viewModel by lazy { newViewModel() }

    private fun TestScope.keepCollecting(vm: CasesViewModel = viewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun content(vm: CasesViewModel = viewModel) = vm.uiState.value as CasesUiState.Content

    /** Takes a case, a minute after the one before, so the order is known. */
    private var minute = 0L

    private suspend fun take(challengeId: Long, on: LocalDate = today): Long =
        cases.accept(1, challengeId, isDaily = false, at = time.now().plusSeconds(minute++ * 60), on = on)

    private suspend fun lux() = profiles.profile.first()!!.lux

    @Test
    fun `with no cases, today and the history are empty`() = runTest {
        keepCollecting()
        assertThat(content().today).isEmpty()
        assertThat(content().history).isEmpty()
        assertThat(content().used).isEqualTo(0)
    }

    @Test
    fun `today lists open cases first, then closed, then dropped`() = runTest {
        val closed = take(1)
        val dropped = take(2)
        val open = take(3)
        cases.close(closed, time.now(), today, luxEarned = 25)
        cases.drop(dropped, penalty = 5)
        keepCollecting()

        assertThat(content().today.map { it.id }).containsExactly(open, closed, dropped).inOrder()
        assertThat(content().today.map { it.lux }).containsExactly(25, 25, 5).inOrder()
        assertThat(content().used).isEqualTo(2)
    }

    @Test
    fun `earlier days come newest first, with what was closed and earned`() = runTest {
        val twoDaysAgo = take(1, on = today.minusDays(2))
        cases.close(twoDaysAgo, time.now(), today.minusDays(2), luxEarned = 25)
        val yesterday = take(2, on = today.minusDays(1))
        cases.close(yesterday, time.now(), today.minusDays(1), luxEarned = 25)
        take(3, on = today.minusDays(1))
        cases.markColdBefore(1, today)
        take(4)
        keepCollecting()

        val history = content().history
        assertThat(history.map { it.day }).containsExactly(today.minusDays(1), today.minusDays(2)).inOrder()
        assertThat(history.first().cases.map { it.status }).containsExactly(CaseStatus.CLOSED, CaseStatus.COLD)
        assertThat(history.first().closed).isEqualTo(1)
        assertThat(history.first().luxEarned).isEqualTo(25)
        assertThat(content().today).hasSize(1)
    }

    @Test
    fun `closing asks first, then pays the Lux and says so`() = runTest {
        take(1)
        keepCollecting()
        val case = content().today.single()

        viewModel.onClose(case)
        assertThat(content().pending).isEqualTo(PendingAction.Close(case))
        assertThat(lux()).isEqualTo(100)

        viewModel.messageFlow.test {
            viewModel.onConfirm()
            assertThat(awaitItem()).isEqualTo(CasesMessage.Closed("Body 1", 25))
        }
        assertThat(content().pending).isNull()
        assertThat(content().today.single().status).isEqualTo(CaseStatus.CLOSED)
        assertThat(lux()).isEqualTo(125)
    }

    @Test
    fun `a closed case keeps its stamp and the narrator's line until the player leaves`() = runTest {
        take(1)
        keepCollecting()
        val case = content().today.single()

        viewModel.onClose(case)
        viewModel.messageFlow.test {
            viewModel.onConfirm()
            awaitItem()
        }
        val just = content().justClosed
        assertThat(just?.caseId).isEqualTo(case.id)
        assertThat(just?.narration).isEqualTo(Narration.Close)
        assertThat(just?.line).isIn(0 until NarratorPool.CLOSE)

        viewModel.onLeave()
        assertThat(content().justClosed).isNull()
    }

    private suspend fun TestScope.closeOne(challengeId: Long) {
        take(challengeId)
        val case = content().today.first { it.status == CaseStatus.OPEN }
        viewModel.onClose(case)
        viewModel.messageFlow.test {
            viewModel.onConfirm()
            awaitItem()
        }
    }

    @Test
    fun `after a close the morning docket is offered until the player answers yes`() = runTest {
        keepCollecting()
        assertThat(content().reminderOffer).isNull()

        closeOne(1)
        assertThat(content().reminderOffer).isEqualTo(LocalTime.of(9, 0))

        viewModel.onReminderAnswer(true)
        assertThat(settings.settings.first().reminderOn).isTrue()
        assertThat(settings.settings.first().reminderAsked).isTrue()
        assertThat(content().reminderOffer).isNull()
    }

    @Test
    fun `no ends the offer for good and leaves the reminder off`() = runTest {
        keepCollecting()
        closeOne(1)
        viewModel.onReminderAnswer(false)
        assertThat(settings.settings.first().reminderOn).isFalse()

        closeOne(2)
        assertThat(content().justClosed).isNotNull()
        assertThat(content().reminderOffer).isNull()
    }

    @Test
    fun `an unanswered offer comes back with the next close`() = runTest {
        keepCollecting()
        closeOne(1)
        viewModel.onLeave()
        assertThat(content().reminderOffer).isNull()

        closeOne(2)
        assertThat(content().reminderOffer).isEqualTo(LocalTime.of(9, 0))
    }

    @Test
    fun `saying no to the dialog leaves the case open`() = runTest {
        take(1)
        keepCollecting()
        viewModel.onClose(content().today.single())
        viewModel.onDismiss()
        assertThat(content().pending).isNull()
        assertThat(content().today.single().status).isEqualTo(CaseStatus.OPEN)
        assertThat(lux()).isEqualTo(100)
    }

    @Test
    fun `dropping asks first, then costs 5 Lux and frees the slot`() = runTest {
        take(1)
        keepCollecting()
        viewModel.onDrop(content().today.single())
        assertThat(content().pending).isInstanceOf(PendingAction.Drop::class.java)

        viewModel.messageFlow.test {
            viewModel.onConfirm()
            assertThat(awaitItem()).isEqualTo(CasesMessage.Dropped("Body 1", 5))
        }
        assertThat(content().today.single().status).isEqualTo(CaseStatus.DROPPED)
        assertThat(content().used).isEqualTo(0)
        assertThat(lux()).isEqualTo(95)
    }

    @Test
    fun `a closed case can't be asked about`() = runTest {
        val id = take(1)
        cases.close(id, time.now(), today, luxEarned = 25)
        keepCollecting()
        viewModel.onDrop(content().today.single())
        assertThat(content().pending).isNull()
    }

    @Test
    fun `the open dialog survives the process being killed`() = runTest {
        take(1)
        keepCollecting()
        viewModel.onDrop(content().today.single())

        val restored = newViewModel()
        keepCollecting(restored)
        assertThat(content(restored).pending).isInstanceOf(PendingAction.Drop::class.java)
    }

    @Test
    fun `a case whose day ended while the screen was open goes cold instead of closing`() = runTest {
        take(1)
        keepCollecting()
        viewModel.onClose(content().today.single())
        time.instant = Instant.parse("2026-11-07T03:00:00Z") // 04:00 local on the 7th

        viewModel.messageFlow.test {
            viewModel.onConfirm()
            assertThat(awaitItem()).isEqualTo(CasesMessage.WentCold)
        }
        assertThat(cases.caseFiles(1).first().single().case.status).isEqualTo(CaseStatus.COLD)
        assertThat(lux()).isEqualTo(100)
    }
}
