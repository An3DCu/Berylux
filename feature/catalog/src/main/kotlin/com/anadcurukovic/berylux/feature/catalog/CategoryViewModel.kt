package com.anadcurukovic.berylux.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.AcceptCaseUseCase
import com.anadcurukovic.berylux.core.domain.AcceptResult
import com.anadcurukovic.berylux.core.domain.DailyPicker
import com.anadcurukovic.berylux.core.domain.LuxRules
import com.anadcurukovic.berylux.core.domain.TodaysCases
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What a challenge's card offers. */
enum class Offer {
    TAKE,

    /** One of today's cases already. */
    TAKEN,

    /** Today's daily case: it can only be taken on Today, where it pays 1.5 times. */
    ON_TODAY,
}

data class ChallengeUi(
    val challenge: Challenge,
    val lux: Int,
    val offer: Offer,
)

sealed interface CategoryUiState {
    data object Loading : CategoryUiState

    /** [difficulty] is the filter, null for all; "My cases" has no filter. */
    data class Content(
        val category: Category,
        val challenges: List<ChallengeUi>,
        val difficulty: Difficulty?,
    ) : CategoryUiState {
        val isMyCases: Boolean get() = category.kind == CategoryKind.CUSTOM
    }

    data object NotFound : CategoryUiState

    data object Error : CategoryUiState
}

sealed interface CategoryMessage {
    data class Taken(val title: String) : CategoryMessage
    data object DayFull : CategoryMessage
    data object AlreadyTaken : CategoryMessage
    data object Failed : CategoryMessage
}

/** One category's challenges, filtered by difficulty, each one ready to take. */
@HiltViewModel(assistedFactory = CategoryViewModel.Factory::class)
class CategoryViewModel @AssistedInject constructor(
    @Assisted private val categoryId: Long,
    challenges: ChallengeRepository,
    todaysCases: TodaysCases,
    private val acceptCase: AcceptCaseUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(categoryId: Long): CategoryViewModel
    }

    private val difficulty = savedState.getStateFlow<String?>(DIFFICULTY, null)
    private val messages = Channel<CategoryMessage>(Channel.BUFFERED)
    private var taking = false

    /** One-off messages for a snackbar; each is shown once. */
    val messageFlow: Flow<CategoryMessage> = messages.receiveAsFlow()

    val uiState: StateFlow<CategoryUiState> =
        combine(
            challenges.categories,
            challenges.challenges,
            todaysCases(),
            difficulty.map { name -> name?.let(Difficulty::valueOf) },
        ) { categories, all, today, selected ->
            val category = categories.firstOrNull { it.id == categoryId }
            if (category == null) {
                CategoryUiState.NotFound
            } else {
                // Today's daily case: the same pick Today shows.
                val dailyId = DailyPicker.pick(all, categories, today.day, today.profile.pickSeed).daily?.id
                content(category, all.filter { it.categoryId == categoryId }, today.cases, dailyId, selected)
            }
        }
            .catch<CategoryUiState> { emit(CategoryUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CategoryUiState.Loading)

    fun onDifficulty(selected: Difficulty?) {
        savedState[DIFFICULTY] = selected?.name
    }

    fun onTake(item: ChallengeUi) {
        if (taking || item.offer != Offer.TAKE) return
        taking = true
        viewModelScope.launch {
            val message = runCatching { acceptCase(item.challenge.id, isDaily = false) }.fold(
                onSuccess = { result ->
                    when (result) {
                        is AcceptResult.Accepted -> CategoryMessage.Taken(item.challenge.title)
                        AcceptResult.DayFull -> CategoryMessage.DayFull
                        AcceptResult.AlreadyTaken -> CategoryMessage.AlreadyTaken
                        AcceptResult.NoProfile -> CategoryMessage.Failed
                    }
                },
                onFailure = { CategoryMessage.Failed },
            )
            messages.send(message)
            taking = false
        }
    }

    private fun content(
        category: Category,
        inCategory: List<Challenge>,
        todayCases: List<CaseFile>,
        dailyId: Long?,
        selected: Difficulty?,
    ): CategoryUiState.Content {
        val isMyCases = category.kind == CategoryKind.CUSTOM
        val filter = selected.takeUnless { isMyCases }
        val takenIds = todayCases.filter { it.case.status != CaseStatus.DROPPED }.map { it.challenge.id }.toSet()
        val shown = inCategory
            .filter { filter == null || it.difficulty == filter }
            .map { challenge ->
                ChallengeUi(
                    challenge = challenge,
                    lux = LuxRules.forCase(challenge, isDaily = false),
                    offer = when (challenge.id) {
                        in takenIds -> Offer.TAKEN
                        dailyId -> Offer.ON_TODAY
                        else -> Offer.TAKE
                    },
                )
            }
        return CategoryUiState.Content(category, shown, filter)
    }

    private companion object {
        const val DIFFICULTY = "difficulty"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
