package com.anadcurukovic.berylux.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.ChallengeProblem
import com.anadcurukovic.berylux.core.domain.ChallengeRules
import com.anadcurukovic.berylux.core.domain.SaveCustomChallengeUseCase
import com.anadcurukovic.berylux.core.domain.SaveResult
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.CaseStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EditorUiState {
    data object Loading : EditorUiState

    /** The challenge to change isn't there, or isn't one of yours. */
    data object NotFound : EditorUiState

    /**
     * @property problems shown only after the first try to save, then kept up to date as you type.
     * @property caseCount how many cases go with it when it's deleted.
     * @property closedCount how many of those were closed: their days may leave the streak.
     */
    data class Form(
        val isNew: Boolean,
        val title: String,
        val description: String,
        val problems: Set<ChallengeProblem>,
        val saving: Boolean,
        val confirmDelete: Boolean,
        val caseCount: Int,
        val closedCount: Int,
    ) : EditorUiState {
        val titleLength: Int get() = ChallengeRules.cleanTitle(title).length
        val descriptionLength: Int get() = ChallengeRules.cleanDescription(description).length
    }
}

sealed interface EditorEvent {
    /** Saved or deleted: leave the editor. */
    data object Done : EditorEvent

    data object Failed : EditorEvent
}

/** Writes a new challenge for "My cases", or changes or deletes one. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = EditorViewModel.Factory::class)
class EditorViewModel @AssistedInject constructor(
    @Assisted private val challengeId: Long?,
    private val challenges: ChallengeRepository,
    profiles: ProfileRepository,
    cases: CaseRepository,
    private val save: SaveCustomChallengeUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(challengeId: Long?): EditorViewModel
    }

    private enum class Status { LOADING, READY, NOT_FOUND }

    private data class Working(val status: Status, val saving: Boolean = false)

    private val working = MutableStateFlow(
        Working(if (challengeId == null || savedState.get<Boolean>(LOADED) == true) Status.READY else Status.LOADING),
    )
    private val events = Channel<EditorEvent>(Channel.BUFFERED)

    /** One-off events: leave the screen, or say it didn't work. */
    val eventFlow: Flow<EditorEvent> = events.receiveAsFlow()

    /** This challenge's cases: all of them, and the closed ones. */
    private val caseCounts: Flow<Pair<Int, Int>> = if (challengeId == null) {
        flowOf(0 to 0)
    } else {
        profiles.profile.filterNotNull()
            .flatMapLatest { cases.caseFiles(it.id) }
            .map { files ->
                val mine = files.filter { it.challenge.id == challengeId }
                mine.size to mine.count { it.case.status == CaseStatus.CLOSED }
            }
    }

    private data class Fields(val title: String, val description: String, val tried: Boolean, val confirm: Boolean)

    private val fields: Flow<Fields> = combine(
        savedState.getStateFlow(TITLE, ""),
        savedState.getStateFlow(DESCRIPTION, ""),
        savedState.getStateFlow(TRIED, false),
        savedState.getStateFlow(CONFIRM_DELETE, false),
        ::Fields,
    )

    val uiState: StateFlow<EditorUiState> = combine(working, fields, caseCounts) { work, f, (count, closed) ->
        when (work.status) {
            Status.LOADING -> EditorUiState.Loading
            Status.NOT_FOUND -> EditorUiState.NotFound
            Status.READY -> EditorUiState.Form(
                isNew = challengeId == null,
                title = f.title,
                description = f.description,
                problems = if (f.tried) ChallengeRules.problems(f.title, f.description) else emptySet(),
                saving = work.saving,
                confirmDelete = f.confirm && challengeId != null,
                caseCount = count,
                closedCount = closed,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EditorUiState.Loading)

    init {
        if (working.value.status == Status.LOADING) load()
    }

    fun onTitleChange(title: String) {
        savedState[TITLE] = title
    }

    fun onDescriptionChange(description: String) {
        savedState[DESCRIPTION] = description
    }

    fun onSave() {
        if (working.value.saving || working.value.status != Status.READY) return
        savedState[TRIED] = true
        val title = savedState.get<String>(TITLE).orEmpty()
        val description = savedState.get<String>(DESCRIPTION).orEmpty()
        if (ChallengeRules.problems(title, description).isNotEmpty()) return
        working.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = runCatching { save(challengeId, title, description) }.getOrNull()
            working.update { it.copy(saving = false) }
            events.send(if (result is SaveResult.Saved) EditorEvent.Done else EditorEvent.Failed)
        }
    }

    fun onDelete() {
        savedState[CONFIRM_DELETE] = true
    }

    fun onDismissDelete() {
        savedState[CONFIRM_DELETE] = false
    }

    fun onConfirmDelete() {
        val id = challengeId ?: return
        savedState[CONFIRM_DELETE] = false
        viewModelScope.launch {
            val deleted = runCatching { challenges.deleteCustom(id) }.getOrDefault(false)
            events.send(if (deleted) EditorEvent.Done else EditorEvent.Failed)
        }
    }

    /** Fills the form once from the stored challenge. After process death the saved form wins. */
    private fun load() {
        val id = challengeId ?: return
        viewModelScope.launch {
            val challenge = runCatching { challenges.challenge(id) }.getOrNull()
            if (challenge == null || !challenge.isCustom) {
                working.value = Working(Status.NOT_FOUND)
                return@launch
            }
            savedState[TITLE] = challenge.title
            savedState[DESCRIPTION] = challenge.description
            savedState[LOADED] = true
            working.value = Working(Status.READY)
        }
    }

    private companion object {
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val TRIED = "tried"
        const val CONFIRM_DELETE = "confirm_delete"
        const val LOADED = "loaded"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
