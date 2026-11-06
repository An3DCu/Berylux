package com.anadcurukovic.berylux.core.model

import java.time.Instant

/**
 * The player. A guest profile lives only on the phone. [pickSeed] is random per profile, so two
 * players don't get the same challenges on the same day.
 */
data class Profile(
    val id: Long,
    val name: String,
    val lux: Int,
    val pickSeed: Long,
    val createdAt: Instant,
)
