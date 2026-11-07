package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.Profile
import javax.inject.Inject

/** What's wrong with a name, if anything. */
enum class NameProblem {
    EMPTY,
    TOO_LONG,
}

object NameRules {
    const val MAX_LENGTH = 20

    /** The name as it's stored: trimmed, with runs of spaces made single. */
    fun clean(name: String): String = name.trim().replace(Regex("\\s+"), " ")

    fun problem(name: String): NameProblem? {
        val cleaned = clean(name)
        return when {
            cleaned.isEmpty() -> NameProblem.EMPTY
            cleaned.length > MAX_LENGTH -> NameProblem.TOO_LONG
            else -> null
        }
    }
}

/** Makes the guest profile on first launch. Refuses a bad name instead of storing it. */
class CreateGuestUseCase @Inject constructor(
    private val profiles: ProfileRepository,
) {
    suspend operator fun invoke(name: String, pickSeed: Long): Profile {
        val problem = NameRules.problem(name)
        require(problem == null) { "invalid name: $problem" }
        return profiles.createGuest(NameRules.clean(name), pickSeed)
    }
}
