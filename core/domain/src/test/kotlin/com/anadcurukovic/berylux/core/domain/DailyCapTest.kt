package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DailyCapTest {
    private val today = LocalDate.of(2026, 11, 6)
    private val yesterday = today.minusDays(1)
    private var nextId = 1L

    private fun case(status: CaseStatus, on: LocalDate = today) = Case(
        id = nextId++,
        challengeId = 1,
        status = status,
        isDaily = false,
        acceptedAt = Instant.EPOCH,
        acceptedOn = on,
    )

    @Test
    fun `open and closed cases today count, dropped ones and other days don't`() {
        val cases = listOf(
            case(CaseStatus.OPEN),
            case(CaseStatus.OPEN),
            case(CaseStatus.OPEN),
            case(CaseStatus.CLOSED),
            case(CaseStatus.DROPPED),
            case(CaseStatus.CLOSED, on = yesterday),
            case(CaseStatus.OPEN, on = yesterday),
        )
        assertThat(DailyCap.usedOn(today, cases)).isEqualTo(4)
        assertThat(DailyCap.remainingOn(today, cases)).isEqualTo(1)
        assertThat(DailyCap.canAcceptOn(today, cases)).isTrue()
    }

    @Test
    fun `the fifth case fills the day`() {
        val cases = List(5) { case(CaseStatus.OPEN) }
        assertThat(DailyCap.remainingOn(today, cases)).isEqualTo(0)
        assertThat(DailyCap.canAcceptOn(today, cases)).isFalse()
    }

    @Test
    fun `dropping a case frees its slot`() {
        val cases = List(4) { case(CaseStatus.OPEN) } + case(CaseStatus.DROPPED)
        assertThat(DailyCap.canAcceptOn(today, cases)).isTrue()
    }

    @Test
    fun `an empty day has all five slots`() {
        assertThat(DailyCap.remainingOn(today, emptyList())).isEqualTo(5)
    }
}
