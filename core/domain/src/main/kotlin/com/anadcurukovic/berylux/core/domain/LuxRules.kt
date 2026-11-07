package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty

/** How much Lux closing a case gives. */
object LuxRules {
    const val EASY = 10
    const val MEDIUM = 25
    const val HARD = 50

    /** What dropping a case costs. Lux never goes below 0. */
    const val DROP_PENALTY = 5

    // The daily case pays 1.5 times as much, kept as 3 halves so the math stays in whole numbers.
    private const val DAILY_HALVES = 3

    fun base(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> EASY
        Difficulty.MEDIUM -> MEDIUM
        Difficulty.HARD -> HARD
    }

    /**
     * Custom challenges give nothing; they only count for streaks. The daily case gives 1.5 times
     * the base, with a half rounded up: a Medium daily case gives 38, not 37.5.
     */
    fun forCase(challenge: Challenge, isDaily: Boolean): Int {
        if (challenge.isCustom) return 0
        val base = base(challenge.difficulty)
        return if (isDaily) (base * DAILY_HALVES + 1) / 2 else base
    }
}
