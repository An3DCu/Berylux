package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.Profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** The player, the game day, and the cases taken on it. */
data class Today(
    val profile: Profile,
    val day: LocalDate,
    val cases: List<CaseFile>,
)

/** Today as one flow: it moves to the next day at the start hour and follows every case change. */
class TodaysCases @Inject constructor(
    private val profiles: ProfileRepository,
    private val cases: CaseRepository,
    private val clock: GameClock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Today> =
        combine(profiles.profile.filterNotNull(), clock.days()) { profile, day -> profile to day }
            .flatMapLatest { (profile, day) ->
                cases.caseFilesAcceptedOn(profile.id, day).map { Today(profile, day, it) }
            }
}
