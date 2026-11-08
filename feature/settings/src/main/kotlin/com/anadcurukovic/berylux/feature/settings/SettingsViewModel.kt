package com.anadcurukovic.berylux.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.ChangeDayStartHourUseCase
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    /**
     * @property hour the hour the game day starts now.
     * @property pendingHour a new hour waiting for tomorrow, or null.
     */
    data class Content(
        val hour: Int,
        val pendingHour: Int?,
        val theme: ThemeMode,
        val effectsLevel: Int,
    ) : SettingsUiState {
        /** The hour the − and + buttons move from: the waiting one, if there is one. */
        val shownHour: Int get() = pendingHour ?: hour
    }

    data object Error : SettingsUiState
}

/** The start hour, the theme and the grain and scanlines. Every change is kept at once. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val changeDayStartHour: ChangeDayStartHourUseCase,
    private val time: TimeSource,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = settings.settings
        .map { content(it) as SettingsUiState }
        .catch { emit(SettingsUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState.Loading)

    /** One hour earlier, wrapping from midnight to 11 PM. */
    fun onEarlier() = moveHour(-1)

    /** One hour later, wrapping from 11 PM to midnight. */
    fun onLater() = moveHour(1)

    fun onTheme(theme: ThemeMode) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun onEffectsLevel(level: Int) {
        if (level !in UserSettings.EFFECTS_LEVELS) return
        viewModelScope.launch { settings.setEffectsLevel(level) }
    }

    private fun moveHour(by: Int) {
        val content = uiState.value as? SettingsUiState.Content ?: return
        val hours = UserSettings.DAY_START_HOURS.count()
        viewModelScope.launch { changeDayStartHour((content.shownHour + by + hours) % hours) }
    }

    private fun content(userSettings: UserSettings): SettingsUiState.Content {
        val now = time.now()
        return SettingsUiState.Content(
            hour = userSettings.startHourAt(now),
            pendingHour = userSettings.pendingStartHourAt(now),
            theme = userSettings.theme,
            effectsLevel = userSettings.effectsLevel,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
