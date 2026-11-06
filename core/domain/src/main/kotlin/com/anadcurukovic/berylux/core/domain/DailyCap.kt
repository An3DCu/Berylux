package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.CaseStatus
import java.time.LocalDate

/**
 * At most [LIMIT] cases a game day, open and closed together. A dropped case doesn't count, so
 * dropping one frees its slot.
 */
object DailyCap {
    const val LIMIT = 5

    fun usedOn(day: LocalDate, cases: List<Case>): Int =
        cases.count { it.acceptedOn == day && it.status != CaseStatus.DROPPED }

    fun remainingOn(day: LocalDate, cases: List<Case>): Int = (LIMIT - usedOn(day, cases)).coerceAtLeast(0)

    fun canAcceptOn(day: LocalDate, cases: List<Case>): Boolean = remainingOn(day, cases) > 0
}
