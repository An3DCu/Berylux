package com.anadcurukovic.berylux.feature.rivals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.RivalBoard
import com.anadcurukovic.berylux.core.domain.RivalBoardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface RivalsUiState {
    data object Loading : RivalsUiState

    data class Content(val board: RivalBoard) : RivalsUiState

    data object Error : RivalsUiState
}

/** This week's board: you and the five bots. */
@HiltViewModel
class RivalsViewModel @Inject constructor(board: RivalBoardUseCase) : ViewModel() {
    val uiState: StateFlow<RivalsUiState> = board()
        .map<RivalBoard, RivalsUiState> { RivalsUiState.Content(it) }
        .catch { emit(RivalsUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RivalsUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
