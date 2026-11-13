package com.anadcurukovic.berylux.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.GameDay
import com.anadcurukovic.berylux.core.domain.RankProgress
import com.anadcurukovic.berylux.core.domain.RankRules
import com.anadcurukovic.berylux.core.domain.StreakState
import com.anadcurukovic.berylux.core.domain.Streaks
import com.anadcurukovic.berylux.core.domain.repository.AchievementRepository
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.Achievement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * One achievement in the list. [unlockedAt] is null while it's locked. [earnedOn] is the game day it
 * was earned on, the day a CLOSED stamp shows: before the start hour, that's the day before.
 */
data class AchievementUi(
    val achievement: Achievement,
    val unlockedAt: Instant?,
    val isNew: Boolean,
    val earnedOn: LocalDate? = null,
)

sealed interface ProgressUiState {
    data object Loading : ProgressUiState

    data class Content(
        val lux: Int,
        val rank: RankProgress,
        val streak: StreakState,
        val achievements: List<AchievementUi>,
    ) : ProgressUiState {
        val unlockedCount: Int get() = achievements.count { it.unlockedAt != null }
    }

    data object Error : ProgressUiState
}

/** Rank, streak and achievements. Unlocks since the last look are marked new. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    profiles: ProfileRepository,
    cases: CaseRepository,
    achievements: AchievementRepository,
    clock: GameClock,
    private val settings: SettingsRepository,
    private val time: TimeSource,
) : ViewModel() {
    /** Unlocks after this are new on this visit. Null until the screen is shown: nothing is new. */
    private val newSince = MutableStateFlow<Instant?>(null)

    val uiState: StateFlow<ProgressUiState> = profiles.profile.filterNotNull()
        .flatMapLatest { profile ->
            combine(
                cases.closedDays(profile.id),
                clock.days(),
                achievements.unlocked(profile.id),
                newSince,
                settings.settings,
            ) { closedDays, today, unlocked, since, userSettings ->
                ProgressUiState.Content(
                    lux = profile.lux,
                    rank = RankRules.progress(profile.lux),
                    streak = Streaks.calculate(closedDays, today),
                    achievements = Achievement.entries.map { achievement ->
                        val at = unlocked[achievement.key]
                        AchievementUi(
                            achievement = achievement,
                            unlockedAt = at,
                            isNew = at != null && since != null && at.isAfter(since),
                            earnedOn = at?.let { GameDay.of(it, time.zone(), userSettings.startHourAt(it)) },
                        )
                    },
                )
            }
        }
        .catch<ProgressUiState> { emit(ProgressUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProgressUiState.Loading)

    /**
     * Called each time the screen comes into view: marks what's new since the last look, then
     * records this look, which clears the mark on the tab.
     */
    fun onShown() {
        viewModelScope.launch {
            newSince.value = settings.achievementsSeenAt.first()
            settings.markAchievementsSeen(time.now())
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
