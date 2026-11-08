package com.anadcurukovic.berylux.feature.today

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.AcceptCaseUseCase
import com.anadcurukovic.berylux.core.domain.AcceptResult
import com.anadcurukovic.berylux.core.domain.DailyCap
import com.anadcurukovic.berylux.core.domain.DailyPicker
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.LuxRules
import com.anadcurukovic.berylux.core.domain.RankProgress
import com.anadcurukovic.berylux.core.domain.RankRules
import com.anadcurukovic.berylux.core.domain.Streaks
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Profile
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** One challenge on offer today, with everything its card shows. */
data class PickUi(
    val challenge: Challenge,
    val categoryName: String,
    val lux: Int,
    val isDaily: Boolean,
    val taken: Boolean,
)

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Content(
        val name: String,
        val sigilSeed: Long,
        val lux: Int,
        val rank: RankProgress,
        val used: Int,
        val daily: PickUi?,
        val bonus: PickUi?,
        val picks: List<PickUi>,
        val filters: List<Category>,
        val selectedFilter: Long?,
        val streak: Int = 0,
    ) : TodayUiState {
        val limit: Int get() = DailyCap.LIMIT
        val dayFull: Boolean get() = used >= limit
    }

    data object Error : TodayUiState
}

/** Things to tell the player once, in a snackbar. */
sealed interface TodayMessage {
    data class Taken(val title: String) : TodayMessage
    data object DayFull : TodayMessage
    data object AlreadyTaken : TodayMessage
    data object Failed : TodayMessage
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    profiles: ProfileRepository,
    challenges: ChallengeRepository,
    cases: CaseRepository,
    clock: GameClock,
    private val acceptCase: AcceptCaseUseCase,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val filter = savedState.getStateFlow<Long?>(FILTER, null)
    private val messages = Channel<TodayMessage>(Channel.BUFFERED)
    private var taking = false

    /** One-off messages for a snackbar; each is shown once. */
    val messageFlow: Flow<TodayMessage> = messages.receiveAsFlow()

    private data class Base(
        val profile: Profile,
        val day: LocalDate,
        val categories: List<Category>,
        val challenges: List<Challenge>,
    )

    private val base: Flow<Base> = combine(
        profiles.profile.filterNotNull(),
        clock.days(),
        challenges.categories,
        challenges.challenges,
    ) { profile, day, categoryList, challengeList -> Base(profile, day, categoryList, challengeList) }

    private val todaysCases: Flow<List<CaseFile>> = base
        .map { it.profile.id to it.day }
        .flatMapLatest { (profileId, day) -> cases.caseFilesAcceptedOn(profileId, day) }

    /** The current streak, for the sign. */
    private val streak: Flow<Int> = base
        .map { it.profile.id to it.day }
        .flatMapLatest { (profileId, day) -> cases.closedDays(profileId).map { Streaks.calculate(it, day).current } }

    val uiState: StateFlow<TodayUiState> = combine(base, todaysCases, filter, streak) { b, todays, selected, days ->
        content(b, todays, selected).copy(streak = days)
    }
        .catch<TodayUiState> { emit(TodayUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TodayUiState.Loading)

    fun onFilter(categoryId: Long?) {
        savedState[FILTER] = categoryId
    }

    fun onTake(pick: PickUi) {
        if (taking) return
        taking = true
        viewModelScope.launch {
            val message = runCatching { acceptCase(pick.challenge.id, pick.isDaily) }.fold(
                onSuccess = { result ->
                    when (result) {
                        is AcceptResult.Accepted -> TodayMessage.Taken(pick.challenge.title)
                        AcceptResult.DayFull -> TodayMessage.DayFull
                        AcceptResult.AlreadyTaken -> TodayMessage.AlreadyTaken
                        AcceptResult.NoProfile -> TodayMessage.Failed
                    }
                },
                onFailure = { TodayMessage.Failed },
            )
            messages.send(message)
            taking = false
        }
    }

    private fun content(b: Base, todays: List<CaseFile>, selected: Long?): TodayUiState.Content {
        val categoryNames = b.categories.associate { it.id to it.name }
        val takenIds = todays.filter { it.case.status != CaseStatus.DROPPED }.map { it.challenge.id }.toSet()
        fun ui(challenge: Challenge, isDaily: Boolean) = PickUi(
            challenge = challenge,
            categoryName = categoryNames[challenge.categoryId].orEmpty(),
            lux = LuxRules.forCase(challenge, isDaily),
            isDaily = isDaily,
            taken = challenge.id in takenIds,
        )

        val pick = DailyPicker.pick(b.challenges, b.categories, b.day, b.profile.pickSeed, selected)
        return TodayUiState.Content(
            name = b.profile.name,
            sigilSeed = b.profile.pickSeed,
            lux = b.profile.lux,
            rank = RankRules.progress(b.profile.lux),
            used = DailyCap.usedOn(b.day, todays.map { it.case }),
            daily = pick.daily?.let { ui(it, isDaily = true) },
            bonus = pick.bonus?.let { ui(it, isDaily = false) },
            picks = pick.picks.map { ui(it, isDaily = false) },
            filters = b.categories.filter { it.kind == CategoryKind.REGULAR },
            selectedFilter = selected,
        )
    }

    private companion object {
        const val FILTER = "filter"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
