package com.anadcurukovic.berylux.feature.settings

import com.anadcurukovic.berylux.core.domain.ChangeDayStartHourUseCase
import com.anadcurukovic.berylux.core.model.MotionMode
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
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
import java.time.LocalTime

class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val time = FakeTimeSource()
    private val settings = FakeSettingsRepository()

    // Built lazily: the main dispatcher rule has to be in place first.
    private val viewModel by lazy { SettingsViewModel(settings, ChangeDayStartHourUseCase(settings, time), time) }

    private fun TestScope.keepCollecting() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun content() = viewModel.uiState.value as SettingsUiState.Content

    @Test
    fun `it opens with the hour, the docket, the theme, the effects and the motion in force`() = runTest {
        keepCollecting()
        assertThat(content()).isEqualTo(
            SettingsUiState.Content(
                hour = 4,
                pendingHour = null,
                theme = ThemeMode.DARK,
                effectsLevel = 3,
                motion = MotionMode.FOLLOW_PHONE,
                reminderOn = false,
                reminderTime = LocalTime.of(9, 0),
            ),
        )
    }

    @Test
    fun `a later hour waits for tomorrow, and the buttons move from it`() = runTest {
        keepCollecting()
        viewModel.onLater()
        assertThat(content().hour).isEqualTo(4)
        assertThat(content().pendingHour).isEqualTo(5)

        viewModel.onLater()
        assertThat(content().pendingHour).isEqualTo(6)
    }

    @Test
    fun `going back to the hour in force cancels the wait`() = runTest {
        keepCollecting()
        viewModel.onLater()
        viewModel.onEarlier()
        assertThat(content().pendingHour).isNull()
        assertThat(settings.settings.first().nextDayStartHour).isNull()
    }

    @Test
    fun `the hour wraps around midnight`() = runTest {
        settings.setDayStartHour(0)
        keepCollecting()
        viewModel.onEarlier()
        assertThat(content().pendingHour).isEqualTo(23)
    }

    @Test
    fun `the theme and the effects are kept at once, and a level out of range is ignored`() = runTest {
        keepCollecting()
        viewModel.onTheme(ThemeMode.DAYLIGHT)
        viewModel.onEffectsLevel(5)
        viewModel.onEffectsLevel(9)
        assertThat(settings.settings.first()).isEqualTo(UserSettings(theme = ThemeMode.DAYLIGHT, effectsLevel = 5))
    }

    @Test
    fun `the morning docket is turned on and moved at once`() = runTest {
        keepCollecting()
        viewModel.onReminderOn(true)
        viewModel.onReminderTime(LocalTime.of(7, 30))
        assertThat(content().reminderOn).isTrue()
        assertThat(content().reminderTime).isEqualTo(LocalTime.of(7, 30))

        viewModel.onReminderOn(false)
        assertThat(settings.settings.first().reminderOn).isFalse()
    }

    @Test
    fun `a motion pick is kept at once and shows as picked`() = runTest {
        keepCollecting()
        viewModel.onMotion(MotionMode.REDUCED)
        assertThat(settings.settings.first().motion).isEqualTo(MotionMode.REDUCED)
        assertThat(content().motion).isEqualTo(MotionMode.REDUCED)
    }
}
