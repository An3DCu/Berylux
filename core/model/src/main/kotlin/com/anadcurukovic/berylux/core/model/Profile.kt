package com.anadcurukovic.berylux.core.model

import java.time.Instant

/**
 * The player. A guest profile lives only on the phone. [pickSeed] is random per profile. Two
 * players don't get the same challenges on the same day; it also drives the rival bots.
 * [sigilSeed] draws the sigil. It starts as the pick seed, and redrawing the sigil changes only it.
 */
data class Profile(
    val id: Long,
    val name: String,
    val lux: Int,
    val pickSeed: Long,
    val createdAt: Instant,
    val sigilSeed: Long = pickSeed,
)
