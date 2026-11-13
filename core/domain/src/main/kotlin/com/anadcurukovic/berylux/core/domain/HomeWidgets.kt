package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.Challenge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

/** Where today's daily case stands on the Today's case widget. */
enum class DailyStanding {
    NOT_TAKEN,
    OPEN,
    CLOSED,
}

/**
 * The daily case as the widget shows it. [lux] is what it pays, or what it earned once closed.
 * [caseId] is the case to close; null until it's taken.
 */
data class DailyOnDesk(
    val challenge: Challenge,
    val categoryName: String,
    val standing: DailyStanding,
    val lux: Int,
    val caseId: Long?,
)

/**
 * What the two home screen widgets show: the streak for the sign, and today's daily case, which is
 * null before there's a player.
 */
data class HomeWidgetsState(
    val streak: Int,
    val daily: DailyOnDesk?,
)

/**
 * The widgets' state as one flow: it follows every case change, a new player, and the game day.
 * A widget collects it while it's on screen.
 */
class HomeWidgetsUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val challenges: ChallengeRepository,
    private val cases: CaseRepository,
    private val clock: GameClock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<HomeWidgetsState> = profiles.profile.flatMapLatest { profile ->
        if (profile == null) {
            flowOf(HomeWidgetsState(streak = 0, daily = null))
        } else {
            combine(
                clock.days(),
                challenges.categories,
                challenges.challenges,
                cases.caseFiles(profile.id),
            ) { day, categories, list, files -> HomeWidgets.state(profile.pickSeed, day, categories, list, files) }
        }
    }.distinctUntilChanged()
}

/** What the widgets show, worked out from the player's cases on a game day. */
object HomeWidgets {
    fun state(
        pickSeed: Long,
        day: LocalDate,
        categories: List<Category>,
        challenges: List<Challenge>,
        files: List<CaseFile>,
    ): HomeWidgetsState {
        val closedDays = files.filter { it.case.status == CaseStatus.CLOSED }.mapNotNull { it.case.closedOn }
        val streak = Streaks.calculate(closedDays, day).current
        val daily = DailyPicker.pick(challenges, categories, day, pickSeed).daily
        return HomeWidgetsState(streak, daily?.let { onDesk(it, day, categories, files) })
    }

    private fun onDesk(
        daily: Challenge,
        day: LocalDate,
        categories: List<Category>,
        files: List<CaseFile>,
    ): DailyOnDesk {
        // As on Today: a dropped daily case can be taken again, and only one is ever on the desk.
        val case = files
            .firstOrNull {
                it.case.acceptedOn == day && it.challenge.id == daily.id && it.case.status != CaseStatus.DROPPED
            }
            ?.case
        val standing = when (case?.status) {
            null -> DailyStanding.NOT_TAKEN
            CaseStatus.CLOSED -> DailyStanding.CLOSED
            else -> DailyStanding.OPEN
        }
        val lux = case?.takeIf { it.status == CaseStatus.CLOSED }?.luxEarned
            ?: LuxRules.forCase(daily, isDaily = case?.isDaily ?: true)
        return DailyOnDesk(
            challenge = daily,
            categoryName = categories.firstOrNull { it.id == daily.categoryId }?.name.orEmpty(),
            standing = standing,
            lux = lux,
            caseId = case?.id,
        )
    }
}
