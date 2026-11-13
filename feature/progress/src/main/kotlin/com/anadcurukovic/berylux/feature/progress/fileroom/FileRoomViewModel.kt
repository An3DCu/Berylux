package com.anadcurukovic.berylux.feature.progress.fileroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.CaseHistoryState
import com.anadcurukovic.berylux.core.domain.CaseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface FileRoomUiState {
    data object Loading : FileRoomUiState

    data class Content(val history: CaseHistoryState) : FileRoomUiState

    data object Error : FileRoomUiState
}

/** The file room: Lux by week, the streak calendar and closed cases by category. */
@HiltViewModel
class FileRoomViewModel @Inject constructor(caseHistory: CaseHistoryUseCase) : ViewModel() {
    val uiState: StateFlow<FileRoomUiState> = caseHistory()
        .map<CaseHistoryState, FileRoomUiState> { FileRoomUiState.Content(it) }
        .catch { emit(FileRoomUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FileRoomUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
