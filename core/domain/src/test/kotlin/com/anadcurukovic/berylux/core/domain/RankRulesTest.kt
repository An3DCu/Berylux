package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.GemRank
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class RankRulesTest {
    @Test
    fun `every rank starts exactly at its threshold`() {
        val expected = listOf(
            0 to GemRank.GOSHENITE,
            499 to GemRank.GOSHENITE,
            500 to GemRank.HELIODOR,
            1_999 to GemRank.HELIODOR,
            2_000 to GemRank.MORGANITE,
            4_999 to GemRank.MORGANITE,
            5_000 to GemRank.AQUAMARINE,
            10_999 to GemRank.AQUAMARINE,
            11_000 to GemRank.EMERALD,
            21_999 to GemRank.EMERALD,
            22_000 to GemRank.RED_BERYL,
            1_000_000 to GemRank.RED_BERYL,
        )
        expected.forEach { (lux, rank) -> assertThat(RankRules.rankFor(lux)).isEqualTo(rank) }
    }

    @Test
    fun `progress halfway through heliodor`() {
        // Heliodor runs from 500 to 2,000: 1,500 Lux wide. 1,250 is 750 in, 750 to go.
        val progress = RankRules.progress(1_250)
        assertThat(progress.rank).isEqualTo(GemRank.HELIODOR)
        assertThat(progress.next).isEqualTo(GemRank.MORGANITE)
        assertThat(progress.luxIntoRank).isEqualTo(750)
        assertThat(progress.luxForRank).isEqualTo(1_500)
        assertThat(progress.luxToNext).isEqualTo(750)
        assertThat(progress.fraction).isEqualTo(0.5f)
    }

    @Test
    fun `a new player has 500 Lux to go`() {
        val progress = RankRules.progress(0)
        assertThat(progress.rank).isEqualTo(GemRank.GOSHENITE)
        assertThat(progress.luxToNext).isEqualTo(500)
        assertThat(progress.fraction).isEqualTo(0f)
    }

    @Test
    fun `red beryl is the top, with a full bar`() {
        val progress = RankRules.progress(30_000)
        assertThat(progress.rank).isEqualTo(GemRank.RED_BERYL)
        assertThat(progress.next).isNull()
        assertThat(progress.luxToNext).isEqualTo(0)
        assertThat(progress.fraction).isEqualTo(1f)
    }

    @Test
    fun `negative Lux is refused`() {
        assertThrows(IllegalArgumentException::class.java) { RankRules.rankFor(-1) }
    }
}
