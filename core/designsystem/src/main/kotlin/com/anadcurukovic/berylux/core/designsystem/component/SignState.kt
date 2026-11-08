package com.anadcurukovic.berylux.core.designsystem.component

/** Where the sign stands on a given streak day. */
internal sealed interface SignState {
    /** [lit] letters on, in [week]'s red (0 to 3). */
    data class Lighting(val lit: Int, val week: Int) : SignState

    /** Day 29 on: the whole word is stamped until the streak ends. */
    data object Stamped : SignState

    companion object {
        fun of(streakDays: Int): SignState = when {
            streakDays <= 0 -> Lighting(lit = 0, week = 0)
            streakDays > WEEKS * LETTERS -> Stamped
            else -> Lighting(lit = (streakDays - 1) % LETTERS + 1, week = (streakDays - 1) / LETTERS)
        }
    }
}

internal const val LETTERS = 7
private const val WEEKS = 4
