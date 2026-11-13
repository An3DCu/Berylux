package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import javax.inject.Inject

/** What's wrong with a custom challenge's form, if anything. */
enum class ChallengeProblem {
    TITLE_EMPTY,
    TITLE_TOO_LONG,
    DESCRIPTION_TOO_LONG,
}

object ChallengeRules {
    const val TITLE_MAX = 40
    const val DESCRIPTION_MAX = 200

    /** The title as it's stored: trimmed, with runs of spaces made single. */
    fun cleanTitle(title: String): String = title.trim().replace(Regex("\\s+"), " ")

    /** The description as it's stored: trimmed at both ends, its line breaks kept. */
    fun cleanDescription(description: String): String = description.trim()

    /** Every problem with the form. The screen shows each one at its field. */
    fun problems(title: String, description: String): Set<ChallengeProblem> = buildSet {
        val cleanTitle = cleanTitle(title)
        if (cleanTitle.isEmpty()) add(ChallengeProblem.TITLE_EMPTY)
        if (cleanTitle.length > TITLE_MAX) add(ChallengeProblem.TITLE_TOO_LONG)
        if (cleanDescription(description).length > DESCRIPTION_MAX) add(ChallengeProblem.DESCRIPTION_TOO_LONG)
    }
}

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult

    data class Invalid(val problems: Set<ChallengeProblem>) : SaveResult

    /** The challenge to change isn't there any more, or isn't custom. */
    data object NotFound : SaveResult
}

/** Writes a new custom challenge into "My cases", or changes one. Refuses a bad form. */
class SaveCustomChallengeUseCase @Inject constructor(
    private val challenges: ChallengeRepository,
) {
    /** A null [id] writes a new one. */
    suspend operator fun invoke(id: Long?, title: String, description: String): SaveResult {
        val problems = ChallengeRules.problems(title, description)
        if (problems.isNotEmpty()) return SaveResult.Invalid(problems)
        val cleanTitle = ChallengeRules.cleanTitle(title)
        val cleanDescription = ChallengeRules.cleanDescription(description)
        return when {
            id == null -> SaveResult.Saved(challenges.addCustom(cleanTitle, cleanDescription))
            challenges.updateCustom(id, cleanTitle, cleanDescription) -> SaveResult.Saved(id)
            else -> SaveResult.NotFound
        }
    }
}
