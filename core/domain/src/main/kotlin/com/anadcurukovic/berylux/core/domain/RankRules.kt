package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.GemRank

/**
 * Where a player stands between two ranks. At red beryl, [next] is null and the bar is full.
 */
data class RankProgress(
    val rank: GemRank,
    val next: GemRank?,
    val luxIntoRank: Int,
    val luxForRank: Int,
) {
    val luxToNext: Int get() = if (next == null) 0 else luxForRank - luxIntoRank
    val fraction: Float get() = if (next == null) 1f else luxIntoRank.toFloat() / luxForRank
}

object RankRules {
    fun rankFor(lux: Int): GemRank {
        require(lux >= 0) { "lux can't be negative, was $lux" }
        return GemRank.entries.last { lux >= it.minLux }
    }

    fun progress(lux: Int): RankProgress {
        val rank = rankFor(lux)
        val next = GemRank.entries.getOrNull(rank.ordinal + 1)
        return RankProgress(
            rank = rank,
            next = next,
            luxIntoRank = lux - rank.minLux,
            luxForRank = if (next == null) 0 else next.minLux - rank.minLux,
        )
    }
}
