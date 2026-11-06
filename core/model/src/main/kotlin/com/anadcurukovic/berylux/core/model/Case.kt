package com.anadcurukovic.berylux.core.model

import java.time.Instant
import java.time.LocalDate

enum class CaseStatus {
    OPEN,
    CLOSED,
    DROPPED,
}

/**
 * A challenge the player took on. [acceptedOn] and [closedOn] are game days, which start at the
 * player's chosen hour, not at midnight.
 */
data class Case(
    val id: Long,
    val challengeId: Long,
    val status: CaseStatus,
    val isDaily: Boolean,
    val acceptedAt: Instant,
    val acceptedOn: LocalDate,
    val closedAt: Instant? = null,
    val closedOn: LocalDate? = null,
    val luxEarned: Int = 0,
)
