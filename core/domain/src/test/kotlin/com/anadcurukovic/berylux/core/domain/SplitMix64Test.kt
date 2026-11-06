package com.anadcurukovic.berylux.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SplitMix64Test {
    @Test
    fun `matches the reference output for seed 0`() {
        // Reference values 0xE220A8397B1DCDAF, 0x6E789E6AA1B965F4, 0x06C45D188009454F as signed longs.
        val random = SplitMix64(0)
        assertThat(random.nextLong()).isEqualTo(-2152535657050944081L)
        assertThat(random.nextLong()).isEqualTo(7960286522194355700L)
        assertThat(random.nextLong()).isEqualTo(487617019471545679L)
    }

    @Test
    fun `matches the reference output for seed 42`() {
        val random = SplitMix64(42)
        assertThat(random.nextLong()).isEqualTo(-4767286540954276203L)
        assertThat(random.nextLong()).isEqualTo(2949826092126892291L)
    }

    @Test
    fun `nextInt stays inside its bound`() {
        val random = SplitMix64(7)
        repeat(1_000) { assertThat(random.nextInt(6)).isIn(0..5) }
    }

    @Test
    fun `a seeded shuffle is a reordering, the same every time for the same seed`() {
        val items = (1..20).toList()
        val first = items.seededShuffle(99)
        assertThat(first).containsExactlyElementsIn(items)
        assertThat(items.seededShuffle(99)).isEqualTo(first)
        assertThat(items.seededShuffle(100)).isNotEqualTo(first)
    }
}
