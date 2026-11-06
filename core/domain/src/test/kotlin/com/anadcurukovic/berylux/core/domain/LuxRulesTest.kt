package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LuxRulesTest {
    private fun challenge(difficulty: Difficulty, isCustom: Boolean = false) = Challenge(
        id = 1,
        categoryId = 1,
        title = "Test",
        description = "",
        difficulty = difficulty,
        isCustom = isCustom,
    )

    @Test
    fun `each difficulty gives its own Lux`() {
        assertThat(LuxRules.forCase(challenge(Difficulty.EASY), isDaily = false)).isEqualTo(10)
        assertThat(LuxRules.forCase(challenge(Difficulty.MEDIUM), isDaily = false)).isEqualTo(25)
        assertThat(LuxRules.forCase(challenge(Difficulty.HARD), isDaily = false)).isEqualTo(50)
    }

    @Test
    fun `the daily case gives one and a half times as much, halves rounded up`() {
        assertThat(LuxRules.forCase(challenge(Difficulty.EASY), isDaily = true)).isEqualTo(15)
        assertThat(LuxRules.forCase(challenge(Difficulty.MEDIUM), isDaily = true)).isEqualTo(38) // 37.5
        assertThat(LuxRules.forCase(challenge(Difficulty.HARD), isDaily = true)).isEqualTo(75)
    }

    @Test
    fun `custom challenges give no Lux`() {
        Difficulty.entries.forEach { difficulty ->
            assertThat(LuxRules.forCase(challenge(difficulty, isCustom = true), isDaily = false)).isEqualTo(0)
            assertThat(LuxRules.forCase(challenge(difficulty, isCustom = true), isDaily = true)).isEqualTo(0)
        }
    }
}
