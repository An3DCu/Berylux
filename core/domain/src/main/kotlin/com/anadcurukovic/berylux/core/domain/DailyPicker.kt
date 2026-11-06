package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import java.time.LocalDate

/** What a game day offers. Any part can be missing or short when there aren't enough challenges. */
data class DailyPick(
    val daily: Challenge?,
    val bonus: Challenge?,
    val picks: List<Challenge>,
)

/**
 * Picks the day's cases: one daily case and the picks from regular categories, one bonus case
 * from bonus categories. Custom challenges are never picked.
 *
 * The pick depends only on the profile's seed, the day and the category filter, so it stays the
 * same all day and changes the next day. The order the challenges come in doesn't matter.
 */
object DailyPicker {
    const val PICK_COUNT = 4

    // Different salts, so the bonus shuffle isn't tied to the regular one.
    private const val REGULAR_SALT = 0x52454755L
    private const val BONUS_SALT = 0x424F4E55L

    fun pick(
        challenges: List<Challenge>,
        categories: List<Category>,
        day: LocalDate,
        profileSeed: Long,
        categoryFilter: Long? = null,
    ): DailyPick {
        val bonusCategoryIds = categories.filter { it.kind == CategoryKind.BONUS }.map { it.id }.toSet()
        val builtIn = challenges.filter { !it.isCustom }.sortedBy { it.id }
        val (bonusPool, regularPool) = builtIn.partition { it.categoryId in bonusCategoryIds }

        val regular = regularPool.seededShuffle(seedFor(profileSeed, day, REGULAR_SALT))
        val daily = regular.firstOrNull()
        val picks = regular
            .filter { it != daily && (categoryFilter == null || it.categoryId == categoryFilter) }
            .take(PICK_COUNT)
        val bonus = bonusPool.seededShuffle(seedFor(profileSeed, day, BONUS_SALT)).firstOrNull()

        return DailyPick(daily = daily, bonus = bonus, picks = picks)
    }

    private fun seedFor(profileSeed: Long, day: LocalDate, salt: Long): Long =
        SplitMix64(profileSeed xor (day.toEpochDay() * PRIME) xor salt).nextLong()

    private const val PRIME = 1_000_003L
}
