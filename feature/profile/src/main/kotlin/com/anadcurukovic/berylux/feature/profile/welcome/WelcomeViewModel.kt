package com.anadcurukovic.berylux.feature.profile.welcome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.CreateGuestUseCase
import com.anadcurukovic.berylux.core.domain.NameProblem
import com.anadcurukovic.berylux.core.domain.NameRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * @property problem shown only after the player tried to start, so an empty field isn't an error
 * the moment the screen opens.
 */
data class WelcomeUiState(
    val name: String = "",
    val seed: Long = 0,
    val problem: NameProblem? = null,
    val starting: Boolean = false,
    val failed: Boolean = false,
)

/** First launch: a name and a sigil, then the guest profile is made and the app opens. */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val createGuest: CreateGuestUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WelcomeUiState(
            name = savedState[NAME] ?: "",
            seed = savedState[SEED] ?: Random.nextLong().also { savedState[SEED] = it },
        ),
    )
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun onNameChange(name: String) {
        savedState[NAME] = name
        _state.update { it.copy(name = name, problem = null, failed = false) }
    }

    fun onDrawAnother() {
        val seed = Random.nextLong()
        savedState[SEED] = seed
        _state.update { it.copy(seed = seed) }
    }

    fun onStart() {
        val current = _state.value
        if (current.starting) return
        val problem = NameRules.problem(current.name)
        if (problem != null) {
            _state.update { it.copy(problem = problem) }
            return
        }
        _state.update { it.copy(starting = true) }
        viewModelScope.launch {
            // On success the app sees the new profile and leaves this screen by itself.
            runCatching { createGuest(current.name, current.seed) }
                .onFailure { _state.update { it.copy(starting = false, failed = true) } }
        }
    }

    private companion object {
        const val NAME = "name"
        const val SEED = "seed"
    }
}
