package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.CaseStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface AcceptResult {
    data class Accepted(val caseId: Long) : AcceptResult

    /** All 5 slots for today are used. */
    data object DayFull : AcceptResult

    /** This challenge is already one of today's cases. */
    data object AlreadyTaken : AcceptResult

    data object NoProfile : AcceptResult
}

/** Takes on a challenge as a case for today, within the day's cap. */
class AcceptCaseUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val cases: CaseRepository,
    private val clock: GameClock,
    private val time: TimeSource,
) {
    suspend operator fun invoke(challengeId: Long, isDaily: Boolean): AcceptResult {
        val profile = profiles.profile.first() ?: return AcceptResult.NoProfile
        val day = clock.today()
        val todays = cases.caseFilesAcceptedOn(profile.id, day).first()
        val alreadyTaken = todays.any { it.challenge.id == challengeId && it.case.status != CaseStatus.DROPPED }
        return when {
            alreadyTaken -> AcceptResult.AlreadyTaken
            !DailyCap.canAcceptOn(day, todays.map { it.case }) -> AcceptResult.DayFull
            else -> AcceptResult.Accepted(
                cases.accept(profile.id, challengeId, isDaily = isDaily, at = time.now(), on = day),
            )
        }
    }
}
