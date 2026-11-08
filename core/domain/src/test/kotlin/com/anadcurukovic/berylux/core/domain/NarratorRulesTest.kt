package com.anadcurukovic.berylux.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class NarratorRulesTest {
    @Test
    fun `a milestone wins over a rank-up, and a rank-up over a plain close`() {
        assertThat(NarratorRules.narration(rankUp = true, newStreak = 7)).isEqualTo(Narration.Milestone(7))
        assertThat(NarratorRules.narration(rankUp = true, newStreak = 3)).isEqualTo(Narration.RankUp)
        assertThat(NarratorRules.narration(rankUp = false, newStreak = 3)).isEqualTo(Narration.Close)
    }

    @Test
    fun `only the end of each week and day 29 are milestones`() {
        val milestones = (1..40).filter { NarratorRules.narration(false, it) is Narration.Milestone }
        assertThat(milestones).containsExactly(7, 14, 21, 28, 29).inOrder()
    }

    @Test
    fun `a day that already counted brings no milestone`() {
        assertThat(NarratorRules.narration(rankUp = false, newStreak = null)).isEqualTo(Narration.Close)
    }

    @Test
    fun `the same line never comes twice in a row`() {
        val random = Random(412)
        var last: Int? = null
        repeat(1_000) {
            val line = NarratorRules.nextLine(12, last, random)
            assertThat(line).isIn(0 until 12)
            assertThat(line).isNotEqualTo(last)
            last = line
        }
    }

    @Test
    fun `every line of the pool comes up`() {
        val random = Random(7)
        var last: Int? = null
        val seen = mutableSetOf<Int>()
        repeat(500) { last = NarratorRules.nextLine(12, last, random).also { seen += it } }
        assertThat(seen).containsExactlyElementsIn(0 until 12)
    }

    @Test
    fun `a pool of one always gives its only line`() {
        assertThat(NarratorRules.nextLine(1, last = 0)).isEqualTo(0)
    }
}
