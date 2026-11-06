package com.anadcurukovic.berylux.core.domain

/**
 * SplitMix64, a small random number generator with a published reference output. It's written
 * out here instead of using `kotlin.random.Random(seed)`, whose sequence for a seed isn't
 * promised to stay the same between Kotlin versions. The daily pick has to be the same on every
 * device and, later, on the server.
 */
internal class SplitMix64(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr SHIFT_1)) * MIX_1
        z = (z xor (z ushr SHIFT_2)) * MIX_2
        return z xor (z ushr SHIFT_3)
    }

    /** A number from 0 until [bound]. The bias from the modulo is too small to matter here. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    private companion object {
        // 0x9E3779B97F4A7C15, 0xBF58476D1CE4E5B9 and 0x94D049BB133111EB as signed longs.
        const val GOLDEN_GAMMA = -0x61C8864680B583EBL
        const val MIX_1 = -0x40A7B892E31B1A47L
        const val MIX_2 = -0x6B2FB644ECCEEE15L
        const val SHIFT_1 = 30
        const val SHIFT_2 = 27
        const val SHIFT_3 = 31
    }
}

/** Fisher-Yates shuffle with a seeded generator: the same seed always gives the same order. */
internal fun <T> List<T>.seededShuffle(seed: Long): List<T> {
    val result = toMutableList()
    val random = SplitMix64(seed)
    for (i in result.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val swap = result[i]
        result[i] = result[j]
        result[j] = swap
    }
    return result
}
