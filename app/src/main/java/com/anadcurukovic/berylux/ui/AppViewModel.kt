package com.anadcurukovic.berylux.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.MarkColdCasesUseCase
import com.anadcurukovic.berylux.core.domain.UnlockAchievementsUseCase
import com.anadcurukovic.berylux.core.domain.repository.AchievementRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.model.Achievement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AppState {
    data object Loading : AppState

    /** First launch: no profile yet, show the welcome screen. */
    data object NeedsProfile : AppState

    data object Ready : AppState
}

/**
 * Decides between the welcome screen and the app, from whether a profile exists. While the app
 * runs, it also turns cases left open from an earlier day cold (at the start and at each new day)
 * and unlocks achievements as they're earned.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    profiles: ProfileRepository,
    achievements: AchievementRepository,
    settings: SettingsRepository,
    markColdCases: MarkColdCasesUseCase,
    unlockAchievements: UnlockAchievementsUseCase,
) : ViewModel() {
    val state: StateFlow<AppState> = profiles.profile
        .map { if (it == null) AppState.NeedsProfile else AppState.Ready }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    private val unlocks = Channel<Achievement>(Channel.UNLIMITED)

    /** Each achievement unlocked while the app runs, once, for a message. */
    val unlockFlow: Flow<Achievement> = unlocks.receiveAsFlow()

    /** Whether something was unlocked after the last look at Progress: a mark on its tab. */
    val hasNewAchievements: StateFlow<Boolean> = profiles.profile.filterNotNull()
        .flatMapLatest { profile ->
            combine(achievements.unlocked(profile.id), settings.achievementsSeenAt) { unlocked, seenAt ->
                unlocked.values.any { it.isAfter(seenAt) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    init {
        // Neither is worth a crash: the close and drop use cases check the day too, and the
        // achievements are checked again on the next change.
        markColdCases().catch { }.launchIn(viewModelScope)
        unlockAchievements()
            .onEach { new -> new.forEach { unlocks.send(it) } }
            .catch { }
            .launchIn(viewModelScope)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
