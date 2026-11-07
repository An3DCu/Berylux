package com.anadcurukovic.berylux.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AppState {
    data object Loading : AppState

    /** First launch: no profile yet, show the welcome screen. */
    data object NeedsProfile : AppState

    data object Ready : AppState
}

/** Decides between the welcome screen and the app, from whether a profile exists. */
@HiltViewModel
class AppViewModel @Inject constructor(profiles: ProfileRepository) : ViewModel() {
    val state: StateFlow<AppState> = profiles.profile
        .map { if (it == null) AppState.NeedsProfile else AppState.Ready }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)
}
