package com.anadcurukovic.berylux.feature.tracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.CaseActions
import com.anadcurukovic.berylux.core.domain.CloseResult
import com.anadcurukovic.berylux.core.domain.DailyCap
import com.anadcurukovic.berylux.core.domain.DropResult
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.LuxRules
import com.anadcurukovic.berylux.core.domain.Narration
import com.anadcurukovic.berylux.core.domain.NarratorRules
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * One case as a card shows it. [lux] is what it pays while open, what it earned once closed and
 * what dropping cost once dropped. A cold case shows none.
 */
data class CaseUi(
    val file: CaseFile,
    val categoryName: String,
    val lux: Int,
) {
    val id: Long get() = file.case.id
    val title: String get() = file.challenge.title
    val status: CaseStatus get() = file.case.status
}

/** An earlier game day in the history, newest first. */
data class DayUi(
    val day: LocalDate,
    val cases: List<CaseUi>,
) {
    val closed: Int get() = cases.count { it.status == CaseStatus.CLOSED }
    val luxEarned: Int get() = cases.filter { it.status == CaseStatus.CLOSED }.sumOf { it.lux }
}

/** The dialog waiting for a yes. It survives rotation and process death. */
sealed interface PendingAction {
    val case: CaseUi

    data class Close(override val case: CaseUi) : PendingAction

    data class Drop(override val case: CaseUi) : PendingAction
}

/**
 * The case just closed, stamped, with the narrator's line under it: [line] indexes the pool for
 * [narration] (see [NarratorPool]). It stays until the player leaves Cases.
 */
data class JustClosed(val caseId: Long, val narration: Narration, val line: Int)

/** How many lines each narrator pool in the strings has. */
internal object NarratorPool {
    const val CLOSE = 12
    const val RANK_UP = 6

    fun size(narration: Narration): Int = when (narration) {
        Narration.Close -> CLOSE
        Narration.RankUp -> RANK_UP
        is Narration.Milestone -> 1
    }
}

sealed interface CasesUiState {
    data object Loading : CasesUiState

    data class Content(
        val today: List<CaseUi>,
        val used: Int,
        val history: List<DayUi>,
        val pending: PendingAction?,
        val justClosed: JustClosed? = null,
    ) : CasesUiState {
        val limit: Int get() = DailyCap.LIMIT
    }

    data object Error : CasesUiState
}

sealed interface CasesMessage {
    data class Closed(val title: String, val lux: Int) : CasesMessage
    data class Dropped(val title: String, val lux: Int) : CasesMessage

    /** The day ended while the case was open. */
    data object WentCold : CasesMessage
    data object Failed : CasesMessage
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CasesViewModel @Inject constructor(
    profiles: ProfileRepository,
    challenges: ChallengeRepository,
    cases: CaseRepository,
    clock: GameClock,
    private val actions: CaseActions,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val pendingId = savedState.getStateFlow<Long?>(PENDING_ID, null)
    private val pendingKind = savedState.getStateFlow<String?>(PENDING_KIND, null)
    private val messages = Channel<CasesMessage>(Channel.BUFFERED)
    private val justClosed = MutableStateFlow<JustClosed?>(null)
    private val lastLines = mutableMapOf<Narration, Int>()
    private var working = false

    /** One-off messages for a snackbar; each is shown once. */
    val messageFlow: Flow<CasesMessage> = messages.receiveAsFlow()

    private val allCases: Flow<List<CaseFile>> = profiles.profile
        .filterNotNull()
        .map { it.id }
        .distinctUntilChanged()
        .flatMapLatest { cases.caseFiles(it) }

    private val pending: Flow<Pair<Long?, String?>> = combine(pendingId, pendingKind) { id, kind -> id to kind }

    val uiState: StateFlow<CasesUiState> =
        combine(
            allCases,
            clock.days(),
            challenges.categories,
            pending,
            justClosed
        ) { files, today, categories, (id, kind), just ->
            content(files, today, categories.associate { it.id to it.name }, id, kind).copy(justClosed = just)
        }
            .catch<CasesUiState> { emit(CasesUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CasesUiState.Loading)

    fun onClose(case: CaseUi) = ask(case, CLOSE)

    fun onDrop(case: CaseUi) = ask(case, DROP)

    /** The player left Cases: the stamp and the narrator's line are done. */
    fun onLeave() {
        justClosed.value = null
    }

    fun onDismiss() {
        savedState[PENDING_ID] = null
        savedState[PENDING_KIND] = null
    }

    fun onConfirm() {
        val pending = (uiState.value as? CasesUiState.Content)?.pending ?: return
        if (working) return
        working = true
        onDismiss()
        viewModelScope.launch {
            val file = pending.case.file
            val message = runCatching {
                when (pending) {
                    is PendingAction.Close -> when (val result = actions.close(file)) {
                        is CloseResult.Closed -> {
                            narrate(file.case.id, result.narration)
                            CasesMessage.Closed(file.challenge.title, result.lux)
                        }
                        CloseResult.WentCold -> CasesMessage.WentCold
                        CloseResult.NotOpen, CloseResult.NoProfile -> CasesMessage.Failed
                    }
                    is PendingAction.Drop -> when (val result = actions.drop(file)) {
                        is DropResult.Dropped -> CasesMessage.Dropped(file.challenge.title, result.lux)
                        DropResult.WentCold -> CasesMessage.WentCold
                        DropResult.NotOpen, DropResult.NoProfile -> CasesMessage.Failed
                    }
                }
            }.getOrElse { CasesMessage.Failed }
            messages.send(message)
            working = false
        }
    }

    private fun narrate(caseId: Long, narration: Narration) {
        val size = NarratorPool.size(narration)
        val line = NarratorRules.nextLine(size, lastLines[narration])
        lastLines[narration] = line
        justClosed.value = JustClosed(caseId, narration, line)
    }

    private fun ask(case: CaseUi, kind: String) {
        if (case.status != CaseStatus.OPEN) return
        savedState[PENDING_ID] = case.id
        savedState[PENDING_KIND] = kind
    }

    private fun content(
        files: List<CaseFile>,
        today: LocalDate,
        categoryNames: Map<Long, String>,
        pendingId: Long?,
        pendingKind: String?,
    ): CasesUiState.Content {
        fun ui(file: CaseFile) = CaseUi(
            file = file,
            categoryName = categoryNames[file.challenge.categoryId].orEmpty(),
            lux = when (file.case.status) {
                CaseStatus.OPEN -> LuxRules.forCase(file.challenge, file.case.isDaily)
                CaseStatus.CLOSED -> file.case.luxEarned
                CaseStatus.DROPPED -> LuxRules.DROP_PENALTY
                CaseStatus.COLD -> 0
            },
        )

        val (todays, earlier) = files.partition { it.case.acceptedOn == today }
        val todayList = todays
            .sortedWith(compareBy({ STATUS_ORDER.indexOf(it.case.status) }, { it.case.acceptedAt }))
            .map(::ui)
        val history = earlier
            .filter { it.case.acceptedOn.isBefore(today) }
            .groupBy { it.case.acceptedOn }
            .toSortedMap(reverseOrder())
            .map { (day, dayFiles) -> DayUi(day, dayFiles.sortedBy { it.case.acceptedAt }.map(::ui)) }

        // Only an open case of today can be waiting for a yes; anything else closes the dialog.
        val target = todayList.firstOrNull { it.id == pendingId && it.status == CaseStatus.OPEN }
        val pending = when {
            target == null -> null
            pendingKind == CLOSE -> PendingAction.Close(target)
            pendingKind == DROP -> PendingAction.Drop(target)
            else -> null
        }
        return CasesUiState.Content(
            today = todayList,
            used = DailyCap.usedOn(today, todays.map { it.case }),
            history = history,
            pending = pending,
        )
    }

    private companion object {
        const val PENDING_ID = "pending_id"
        const val PENDING_KIND = "pending_kind"
        const val CLOSE = "close"
        const val DROP = "drop"
        const val STOP_TIMEOUT_MS = 5_000L

        /** Today's list: the ones you can still act on first. */
        val STATUS_ORDER = listOf(CaseStatus.OPEN, CaseStatus.CLOSED, CaseStatus.COLD, CaseStatus.DROPPED)
    }
}
