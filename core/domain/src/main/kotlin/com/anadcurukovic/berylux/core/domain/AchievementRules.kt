package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.AchievementRepository
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.Achievement
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.GemRank
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Which achievements the player's record has earned. Pure: the same record, the same answer. */
object AchievementRules {
    private const val FEW = 10
    private const val SOME = 50
    private const val MANY = 100
    private const val HARD_NEEDED = 10
    private const val BONUS_NEEDED = 5
    private const val SHORT_STREAK = 7
    private const val LONG_STREAK = 30

    private val RANKS = mapOf(
        GemRank.HELIODOR to Achievement.RANK_HELIODOR,
        GemRank.MORGANITE to Achievement.RANK_MORGANITE,
        GemRank.AQUAMARINE to Achievement.RANK_AQUAMARINE,
        GemRank.EMERALD to Achievement.RANK_EMERALD,
        GemRank.RED_BERYL to Achievement.RANK_RED_BERYL,
    )

    /**
     * @param files every case of the player, any status.
     * @param categories every category, to tell the built-in ones and the bonus ones apart.
     */
    fun earned(files: List<CaseFile>, categories: List<Category>, lux: Int, streak: StreakState): Set<Achievement> =
        fromCases(files.filter { it.case.status == CaseStatus.CLOSED }, categories) + fromProgress(lux, streak)

    private fun fromCases(closed: List<CaseFile>, categories: List<Category>): Set<Achievement> {
        val bonusIds = categories.filter { it.kind == CategoryKind.BONUS }.map { it.id }.toSet()
        val builtInIds = categories.filter { it.kind != CategoryKind.CUSTOM }.map { it.id }.toSet()
        val closedCategoryIds = closed.map { it.challenge.categoryId }.toSet()
        // Your own challenges have no difficulty. Only built-in Hard ones count.
        val hard = closed.count { !it.challenge.isCustom && it.challenge.difficulty == Difficulty.HARD }
        val fullDay = closed.groupingBy { it.case.closedOn }.eachCount().values.any { it >= DailyCap.LIMIT }
        return buildSet {
            if (closed.isNotEmpty()) add(Achievement.FIRST_CLOSED)
            if (closed.size >= FEW) add(Achievement.CLOSED_10)
            if (closed.size >= SOME) add(Achievement.CLOSED_50)
            if (closed.size >= MANY) add(Achievement.CLOSED_100)
            if (fullDay) add(Achievement.FULL_DAY)
            if (hard >= HARD_NEEDED) add(Achievement.HARD_10)
            if (closed.count { it.challenge.categoryId in bonusIds } >= BONUS_NEEDED) add(Achievement.BONUS_5)
            if (builtInIds.isNotEmpty() && closedCategoryIds.containsAll(builtInIds)) add(Achievement.EVERY_CATEGORY)
            if (closed.any { it.challenge.isCustom }) add(Achievement.OWN_CASE)
        }
    }

    private fun fromProgress(lux: Int, streak: StreakState): Set<Achievement> = buildSet {
        if (streak.longest >= SHORT_STREAK) add(Achievement.STREAK_7)
        if (streak.longest >= LONG_STREAK) add(Achievement.STREAK_30)
        if (streak.freezesUsed > 0) add(Achievement.FREEZE_SAVED)
        val rank = RankRules.rankFor(lux)
        RANKS.forEach { (gem, achievement) -> if (rank >= gem) add(achievement) }
    }
}

/**
 * Watches the player's record and stores every achievement it newly earns. Collect [invoke]
 * while the app runs: it emits the ones unlocked just now, for a message. Unlocks are kept for
 * good, even if a drop later lowers the rank or a deleted challenge takes its cases.
 */
class UnlockAchievementsUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val challenges: ChallengeRepository,
    private val cases: CaseRepository,
    private val achievements: AchievementRepository,
    private val clock: GameClock,
    private val time: TimeSource,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Achievement>> {
        val profile = profiles.profile.filterNotNull()
        // Restart only for another player. Closing a case changes the profile's Lux too; restarting
        // on that could cancel an unlock after it was stored but before it came through, and the
        // message would be lost.
        return profile.map { it.id }.distinctUntilChanged().flatMapLatest { profileId ->
            combine(
                profile,
                cases.caseFiles(profileId),
                cases.closedDays(profileId),
                challenges.categories,
                clock.days(),
            ) { player, files, closedDays, categories, today ->
                val streak = Streaks.calculate(closedDays, today)
                AchievementRules.earned(files, categories, player.lux, streak)
            }.map { earned ->
                // unlock() is false for one already stored. Each comes through once.
                earned.filter { achievements.unlock(profileId, it.key, time.now()) }
            }
        }.filter { it.isNotEmpty() }
    }
}
