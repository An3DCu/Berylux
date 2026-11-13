package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.CaseFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Every case the player has taken, on any day, following each change. Nothing before there's a player. */
class PlayerCaseFiles @Inject constructor(
    private val profiles: ProfileRepository,
    private val cases: CaseRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<CaseFile>> = profiles.profile
        .filterNotNull()
        .map { it.id }
        .distinctUntilChanged()
        .flatMapLatest { cases.caseFiles(it) }
}
