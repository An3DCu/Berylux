package com.anadcurukovic.berylux.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GameDayTest {
    private val sarajevo = ZoneId.of("Europe/Sarajevo")
    private val newYork = ZoneId.of("America/New_York")

    private fun day(instant: String, zone: ZoneId = sarajevo, startHour: Int = GameDay.DEFAULT_START_HOUR) =
        GameDay.of(Instant.parse(instant), zone, startHour)

    @Test
    fun `a minute before 4 AM still belongs to the day before`() {
        // 02:59 UTC is 03:59 in Sarajevo in November (UTC+1).
        assertThat(day("2026-11-06T02:59:00Z")).isEqualTo(LocalDate.of(2026, 11, 5))
    }

    @Test
    fun `the new day starts at 4 AM`() {
        assertThat(day("2026-11-06T03:00:00Z")).isEqualTo(LocalDate.of(2026, 11, 6))
    }

    @Test
    fun `with a start hour of 0 the day starts at midnight`() {
        // 22:59 UTC is 23:59 local, 23:30 UTC is 00:30 the next local day.
        assertThat(day("2026-11-05T22:59:00Z", startHour = 0)).isEqualTo(LocalDate.of(2026, 11, 5))
        assertThat(day("2026-11-05T23:30:00Z", startHour = 0)).isEqualTo(LocalDate.of(2026, 11, 6))
    }

    @Test
    fun `the day the clocks go forward is 23 hours long`() {
        // On 29 March 2026 Sarajevo jumps from 02:00 to 03:00 (UTC+1 to UTC+2) at 01:00 UTC.
        // 28 March starts at 04:00 UTC+1 (03:00 UTC) and ends at 04:00 UTC+2 (02:00 UTC on the 29th).
        assertThat(day("2026-03-28T03:00:00Z")).isEqualTo(LocalDate.of(2026, 3, 28))
        assertThat(day("2026-03-29T01:30:00Z")).isEqualTo(LocalDate.of(2026, 3, 28)) // 03:30 local
        assertThat(day("2026-03-29T01:59:00Z")).isEqualTo(LocalDate.of(2026, 3, 28)) // 03:59 local
        assertThat(day("2026-03-29T02:00:00Z")).isEqualTo(LocalDate.of(2026, 3, 29)) // 04:00 local
    }

    @Test
    fun `the day the clocks go back is 25 hours long`() {
        // On 25 October 2026 Sarajevo goes from 03:00 back to 02:00 (UTC+2 to UTC+1) at 01:00 UTC.
        // 24 October starts at 04:00 UTC+2 (02:00 UTC) and ends at 04:00 UTC+1 (03:00 UTC on the 25th).
        assertThat(day("2026-10-24T02:00:00Z")).isEqualTo(LocalDate.of(2026, 10, 24))
        assertThat(day("2026-10-25T00:30:00Z")).isEqualTo(LocalDate.of(2026, 10, 24)) // 02:30 summer time
        assertThat(day("2026-10-25T02:30:00Z")).isEqualTo(LocalDate.of(2026, 10, 24)) // 03:30 winter time
        assertThat(day("2026-10-25T03:00:00Z")).isEqualTo(LocalDate.of(2026, 10, 25)) // 04:00 winter time
    }

    @Test
    fun `the day follows the zone the phone is in`() {
        // 23:00 in Sarajevo on the 5th, then 10:00 in New York (UTC-5) on the 6th.
        assertThat(day("2026-11-05T22:00:00Z", zone = sarajevo)).isEqualTo(LocalDate.of(2026, 11, 5))
        assertThat(day("2026-11-06T15:00:00Z", zone = newYork)).isEqualTo(LocalDate.of(2026, 11, 6))
    }

    @Test
    fun `a day ends at the next start hour, even when it began before midnight`() {
        // 15:00 local on 12 November ends at 4 AM on the 13th, 03:00 UTC.
        assertThat(GameDay.endOf(Instant.parse("2026-11-12T14:00:00Z"), sarajevo, 4))
            .isEqualTo(Instant.parse("2026-11-13T03:00:00Z"))
        // 2 AM local on the 13th is still the 12th's game day: it ends two hours later, at 4.
        assertThat(GameDay.endOf(Instant.parse("2026-11-13T01:00:00Z"), sarajevo, 4))
            .isEqualTo(Instant.parse("2026-11-13T03:00:00Z"))
    }

    @Test
    fun `a day that ends at a start hour the clocks skip ends an hour later`() {
        // With the day starting at 2 AM, 28 March 2026 would end at 2:00 on the 29th, which doesn't
        // exist: the clocks jump to 3:00 summer time, 01:00 UTC.
        assertThat(GameDay.endOf(Instant.parse("2026-03-28T12:00:00Z"), sarajevo, 2))
            .isEqualTo(Instant.parse("2026-03-29T01:00:00Z"))
    }

    @Test
    fun `a start hour outside 0 to 23 is refused`() {
        assertThrows(IllegalArgumentException::class.java) { day("2026-11-06T03:00:00Z", startHour = 24) }
        assertThrows(IllegalArgumentException::class.java) { day("2026-11-06T03:00:00Z", startHour = -1) }
    }
}
