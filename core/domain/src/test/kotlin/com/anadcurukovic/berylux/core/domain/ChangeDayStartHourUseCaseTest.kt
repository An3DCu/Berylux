package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ChangeDayStartHourUseCaseTest {
    private val zone = ZoneId.of("Europe/Sarajevo")

    // 9 PM in Sarajevo on 9 November, with the day starting at 4 AM.
    private val time = FakeTimeSource(at(2026, 11, 9, 21, 0), zone)
    private val settings = FakeSettingsRepository()
    private val clock = GameClock(time, settings)
    private val change = ChangeDayStartHourUseCase(settings, time)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant()

    private suspend fun dayAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): LocalDate {
        time.instant = at(year, month, day, hour, minute)
        return clock.today()
    }

    @Test
    fun `a later hour keeps today's end, then the next day runs until the new hour`() = runTest {
        change(6)

        assertThat(dayAt(2026, 11, 9, 21, 0)).isEqualTo(LocalDate.of(2026, 11, 9))
        // Today ends at the old 4 AM, and the 6 AM rule takes over from there.
        assertThat(dayAt(2026, 11, 10, 3, 59)).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(dayAt(2026, 11, 10, 5, 0)).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(dayAt(2026, 11, 10, 6, 1)).isEqualTo(LocalDate.of(2026, 11, 10))
        assertThat(dayAt(2026, 11, 11, 5, 59)).isEqualTo(LocalDate.of(2026, 11, 10))
        assertThat(dayAt(2026, 11, 11, 6, 1)).isEqualTo(LocalDate.of(2026, 11, 11))
    }

    @Test
    fun `an earlier hour never ends today early`() = runTest {
        change(2)

        // At 2:30 AM the day would already be over under the new hour, but today keeps its 4 AM end.
        assertThat(dayAt(2026, 11, 10, 2, 30)).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(dayAt(2026, 11, 10, 4, 1)).isEqualTo(LocalDate.of(2026, 11, 10))
        assertThat(dayAt(2026, 11, 11, 2, 1)).isEqualTo(LocalDate.of(2026, 11, 11))
    }

    @Test
    fun `the new hour waits until today ends, and shows as waiting until then`() = runTest {
        change(6)
        val stored = settings.settings.first()

        assertThat(stored.nextHourFrom).isEqualTo(at(2026, 11, 10, 4, 0))
        assertThat(stored.pendingStartHourAt(at(2026, 11, 9, 21, 0))).isEqualTo(6)
        assertThat(stored.pendingStartHourAt(at(2026, 11, 10, 4, 0))).isNull()
        assertThat(stored.startHourAt(at(2026, 11, 10, 4, 0))).isEqualTo(6)
    }

    @Test
    fun `picking the hour in force again cancels the waiting change`() = runTest {
        change(6)
        change(4)

        val stored = settings.settings.first()
        assertThat(stored.dayStartHour).isEqualTo(4)
        assertThat(stored.nextDayStartHour).isNull()
        assertThat(stored.nextHourFrom).isNull()
    }

    @Test
    fun `once the new hour is in force, the next change starts from it`() = runTest {
        change(6)
        time.instant = at(2026, 11, 10, 12, 0)
        change(8)

        val stored = settings.settings.first()
        assertThat(stored.dayStartHour).isEqualTo(6)
        assertThat(stored.nextDayStartHour).isEqualTo(8)
        // Today (10 November under the 6 AM rule) ends at 6 AM on the 11th.
        assertThat(stored.nextHourFrom).isEqualTo(at(2026, 11, 11, 6, 0))
    }

    @Test
    fun `a streak carries across the change without a gap or a double day`() = runTest {
        change(2)
        val closedOn = listOf(dayAt(2026, 11, 9, 23, 0), dayAt(2026, 11, 10, 22, 0), dayAt(2026, 11, 11, 3, 0))

        assertThat(closedOn).containsExactly(
            LocalDate.of(2026, 11, 9),
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 11),
        ).inOrder()
        assertThat(Streaks.calculate(closedOn, LocalDate.of(2026, 11, 11)).current).isEqualTo(3)
    }
}
