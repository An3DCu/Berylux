package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.CaseFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

sealed interface CloseResult {
    data class Closed(val lux: Int) : CloseResult

    /** Its day ended before it was closed. It's cold now and can't be closed. */
    data object WentCold : CloseResult

    /** Already closed, dropped or cold. */
    data object NotOpen : CloseResult

    data object NoProfile : CloseResult
}

sealed interface DropResult {
    /** [lux] is what it really cost: less than the penalty when the player had less. */
    data class Dropped(val lux: Int) : DropResult

    data object WentCold : DropResult

    data object NotOpen : DropResult

    data object NoProfile : DropResult
}

/**
 * What can be done to one of today's open cases: close it for its Lux, or drop it for
 * [LuxRules.DROP_PENALTY] to free its slot. A case whose day already ended goes cold instead.
 */
class CaseActions @Inject constructor(
    private val profiles: ProfileRepository,
    private val cases: CaseRepository,
    private val clock: GameClock,
    private val time: TimeSource,
) {
    suspend fun close(file: CaseFile): CloseResult {
        val profile = profiles.profile.first() ?: return CloseResult.NoProfile
        val today = clock.today()
        val lux = LuxRules.forCase(file.challenge, file.case.isDaily)
        return when {
            wentCold(file, profile.id, today) -> CloseResult.WentCold
            cases.close(file.case.id, time.now(), today, lux) -> CloseResult.Closed(lux)
            else -> CloseResult.NotOpen
        }
    }

    suspend fun drop(file: CaseFile): DropResult {
        val profile = profiles.profile.first() ?: return DropResult.NoProfile
        val cost = minOf(LuxRules.DROP_PENALTY, profile.lux)
        return when {
            wentCold(file, profile.id, clock.today()) -> DropResult.WentCold
            cases.drop(file.case.id, LuxRules.DROP_PENALTY) -> DropResult.Dropped(cost)
            else -> DropResult.NotOpen
        }
    }

    /** The day may have ended while the screen was open, before the cold sweep ran. */
    private suspend fun wentCold(file: CaseFile, profileId: Long, today: LocalDate): Boolean {
        if (!file.case.acceptedOn.isBefore(today)) return false
        cases.markColdBefore(profileId, today)
        return true
    }
}

/**
 * Cases still open when their day ends go cold. Collect [invoke] while the app runs: it sweeps
 * once at the start and again each time the game day changes, and emits how many went cold.
 */
class MarkColdCasesUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val cases: CaseRepository,
    private val clock: GameClock,
) {
    operator fun invoke(): Flow<Int> =
        combine(profiles.profile.filterNotNull().map { it.id }.distinctUntilChanged(), clock.days()) { id, day ->
            id to day
        }.map { (id, day) -> cases.markColdBefore(id, day) }
}
