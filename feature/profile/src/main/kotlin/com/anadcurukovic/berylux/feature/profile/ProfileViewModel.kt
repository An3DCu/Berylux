package com.anadcurukovic.berylux.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.NameProblem
import com.anadcurukovic.berylux.core.domain.NameRules
import com.anadcurukovic.berylux.core.domain.Streaks
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    /**
     * @property sigilSeed the sigil on screen: the stored one, or a new draw not filed yet.
     * @property problem shown after a try to file a bad name, until the name changes.
     */
    data class Content(
        val profile: Profile,
        val streak: Int,
        val sigilSeed: Long,
        val problem: NameProblem?,
        val saving: Boolean,
    ) : ProfileUiState

    data object Error : ProfileUiState
}

sealed interface ProfileEvent {
    /** Filed: leave the profile. */
    data object Done : ProfileEvent

    data object Failed : ProfileEvent
}

/** The personnel file: change the name and draw a new sigil, then file both. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    cases: CaseRepository,
    clock: GameClock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val problem = MutableStateFlow<NameProblem?>(null)
    private val saving = MutableStateFlow(false)
    private val events = Channel<ProfileEvent>(Channel.BUFFERED)

    /** One-off events: leave the screen, or say it didn't work. */
    val eventFlow: Flow<ProfileEvent> = events.receiveAsFlow()

    /**
     * The name in the field, as Compose state: a keystroke shows in the same frame. [savedState]
     * keeps a copy for process death; it's filled from the profile once.
     */
    var name: String by mutableStateOf(savedState[NAME] ?: "")
        private set

    private val drawnSeed = savedState.getStateFlow<Long?>(SEED, null)

    private val streak: Flow<Int> = profiles.profile.filterNotNull().flatMapLatest { profile ->
        combine(cases.closedDays(profile.id), clock.days()) { days, today -> Streaks.calculate(days, today).current }
    }

    val uiState: StateFlow<ProfileUiState> =
        combine(
            profiles.profile.filterNotNull(),
            streak,
            drawnSeed,
            problem,
            saving,
        ) { profile, days, seed, issue, busy ->
            ProfileUiState.Content(profile, days, seed ?: profile.sigilSeed, issue, busy) as ProfileUiState
        }
            .catch { emit(ProfileUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfileUiState.Loading)

    init {
        // Fill the field from the profile once; after process death the saved text wins.
        if (savedState.get<Boolean>(LOADED) != true) {
            viewModelScope.launch {
                val profile = profiles.profile.filterNotNull().first()
                onNameChange(profile.name)
                savedState[LOADED] = true
            }
        }
    }

    fun onNameChange(name: String) {
        this.name = name
        savedState[NAME] = name
        problem.value = null
    }

    fun onDrawAnother() {
        savedState[SEED] = Random.nextLong()
    }

    fun onSave() {
        val content = (uiState.value as? ProfileUiState.Content)?.takeIf { !it.saving } ?: return
        val typed = name
        val issue = NameRules.problem(typed)
        if (issue != null) {
            problem.value = issue
        } else {
            file(content, typed)
        }
    }

    private fun file(content: ProfileUiState.Content, typed: String) {
        saving.value = true
        viewModelScope.launch {
            val filed = runCatching {
                profiles.rename(content.profile.id, NameRules.clean(typed))
                profiles.setSigilSeed(content.profile.id, content.sigilSeed)
            }.isSuccess
            saving.value = false
            events.send(if (filed) ProfileEvent.Done else ProfileEvent.Failed)
        }
    }

    private companion object {
        const val NAME = "name"
        const val SEED = "sigil_seed"
        const val LOADED = "loaded"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
