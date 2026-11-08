package com.anadcurukovic.berylux.core.domain

import kotlin.random.Random

/** What the narrator talks about after a close. One close gets one line. */
sealed interface Narration {
    data object Close : Narration

    data object RankUp : Narration

    /** The close made the streak reach [day], one of [NarratorRules.MILESTONES]. */
    data class Milestone(val day: Int) : Narration
}

object NarratorRules {
    /** The streak days with their own line: the end of each week of the sign, and the stamp. */
    val MILESTONES = setOf(7, 14, 21, 28, 29)

    /**
     * A milestone wins over a rank-up, and a rank-up over a plain close. [newStreak] is the
     * streak this close made, or null when today already counted.
     */
    fun narration(rankUp: Boolean, newStreak: Int?): Narration = when {
        newStreak != null && newStreak in MILESTONES -> Narration.Milestone(newStreak)
        rankUp -> Narration.RankUp
        else -> Narration.Close
    }

    /** A line out of [count], never [last] again when there's another to pick. */
    fun nextLine(count: Int, last: Int?, random: Random = Random.Default): Int {
        require(count > 0) { "count must be positive, was $count" }
        if (count == 1 || last == null || last !in 0 until count) return random.nextInt(count)
        // Pick among the others: skip over the last one.
        val pick = random.nextInt(count - 1)
        return if (pick >= last) pick + 1 else pick
    }
}
