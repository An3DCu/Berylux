package com.anadcurukovic.berylux.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.MarkColdCasesUseCase
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
 * runs, it also turns cases left open from an earlier day cold: at the start and at each new day.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    profiles: ProfileRepository,
    markColdCases: MarkColdCasesUseCase,
) : ViewModel() {
    val state: StateFlow<AppState> = profiles.profile
        .map { if (it == null) AppState.NeedsProfile else AppState.Ready }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    init {
        // A failed sweep isn't worth a crash: the close and drop use cases check the day too.
        markColdCases().catch { }.launchIn(viewModelScope)
    }
}
