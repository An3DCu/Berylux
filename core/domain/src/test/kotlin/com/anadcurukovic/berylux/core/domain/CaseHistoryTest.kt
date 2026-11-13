package com.anadcurukovic.berylux.core.domain

import app.cash.turbine.test
import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Friday 13 November 2026. This week starts on Monday 9 November; twelve weeks start on Monday
 * 24 August. The calendar runs from Monday 12 October to Sunday 15 November.
 */
class CaseHistoryTest {
    private val today = LocalDate.of(2026, 11, 13)
    private val categories = listOf(
        Category(1, "Mind", "", CategoryKind.REGULAR),
        Category(2, "Body", "", CategoryKind.REGULAR),
        Category(3, "Calm", "", CategoryKind.REGULAR),
        Category(4, "Skills", "", CategoryKind.REGULAR),
    )
    private var nextId = 1L

    private fun case(
        on: LocalDate,
        status: CaseStatus = CaseStatus.CLOSED,
        lux: Int = 25,
        category: Long = 1,
    ): CaseFile {
        val id = nextId++
        val closed = status == CaseStatus.CLOSED
        return CaseFile(
            Case(
                id = id,
                challengeId = id,
                status = status,
                isDaily = false,
                acceptedAt = Instant.EPOCH,
                acceptedOn = on,
                closedAt = if (closed) Instant.EPOCH else null,
                closedOn = if (closed) on else null,
                luxEarned = if (closed) lux else 0,
            ),
            Challenge(id, category, "Case $id", "", Difficulty.MEDIUM),
        )
    }

    private fun day(month: Int, dayOfMonth: Int) = LocalDate.of(2026, month, dayOfMonth)

    @Test
    fun `Lux adds up by the week a case was closed, over the last twelve`() {
        val files = listOf(
            case(day(11, 10), lux = 25),
            case(day(11, 13), lux = 38),
            case(day(11, 2), lux = 50),
            // Sunday 23 August belongs to the week of 17 August, before the twelve.
            case(day(8, 23), lux = 10),
            case(day(8, 24), lux = 15),
            case(day(11, 12), status = CaseStatus.OPEN),
            case(day(11, 11), status = CaseStatus.DROPPED),
        )
        val history = CaseHistory.of(files, categories, today)

        assertThat(history.weeks).hasSize(12)
        assertThat(history.weeks.first()).isEqualTo(WeekLux(day(8, 24), 15))
        assertThat(history.weeks[10]).isEqualTo(WeekLux(day(11, 2), 50))
        // 25 + 38 this week.
        assertThat(history.weeks.last()).isEqualTo(WeekLux(day(11, 9), 63))
        assertThat(history.weeks.subList(1, 10).map { it.lux }).containsExactlyElementsIn(List(9) { 0 })
        assertThat(history.luxThisWeek).isEqualTo(63)
        // 15 + 50 + 63.
        assertThat(history.luxInWeeks).isEqualTo(128)
        assertThat(history.bestWeek).isEqualTo(63)
    }

    @Test
    fun `the calendar marks closed days, a day a freeze saved, and today`() {
        // 1 to 7 November in a row earn a freeze; it saves the 8th; the 9th and 10th count; the
        // 11th is missed with no freeze left, which ends that streak.
        val closedOn = (1..7).map { day(11, it) } + listOf(day(11, 9), day(11, 10))
        val history = CaseHistory.of(closedOn.map { case(it) }, categories, today)

        assertThat(history.calendar).hasSize(35)
        assertThat(history.calendar.first().day).isEqualTo(day(10, 12))
        assertThat(history.calendar.last().day).isEqualTo(day(11, 15))
        val marks = history.calendar.associate { it.day to it.mark }
        closedOn.forEach { assertThat(marks[it]).isEqualTo(DayMark.CLOSED) }
        // Saved in a streak that has ended since: still marked.
        assertThat(marks[day(11, 8)]).isEqualTo(DayMark.SAVED)
        assertThat(marks[day(11, 11)]).isEqualTo(DayMark.NONE)
        assertThat(history.calendar.single { it.isToday }.day).isEqualTo(today)
        assertThat(history.closedDays).isEqualTo(9)
        assertThat(history.savedDays).isEqualTo(1)
        // 12 October to 13 November: 20 days of October and 13 of November.
        assertThat(history.daysSoFar).isEqualTo(33)
    }

    @Test
    fun `categories count closed cases only, most first, a tie by name`() {
        val files = listOf(
            case(day(11, 1), category = 1),
            case(day(11, 2), category = 1),
            case(day(11, 3), category = 1),
            case(day(11, 4), category = 3),
            case(day(11, 5), category = 2),
            case(day(11, 6), category = 2, status = CaseStatus.DROPPED),
            case(day(11, 13), category = 2, status = CaseStatus.OPEN),
        )
        val history = CaseHistory.of(files, categories, today)

        assertThat(history.categories).containsExactly(
            CategoryCount("Mind", 3),
            CategoryCount("Body", 1),
            CategoryCount("Calm", 1),
        ).inOrder()
    }

    @Test
    fun `with nothing closed, every panel is empty and today is still marked`() {
        val history = CaseHistory.of(listOf(case(today, status = CaseStatus.OPEN)), categories, today)

        assertThat(history.anyClosed).isFalse()
        assertThat(history.weeks.map { it.lux }).containsExactlyElementsIn(List(12) { 0 })
        assertThat(history.bestWeek).isEqualTo(0)
        assertThat(history.calendar.map { it.mark }.distinct()).containsExactly(DayMark.NONE)
        assertThat(history.calendar.single { it.isToday }.day).isEqualTo(today)
        assertThat(history.categories).isEmpty()
    }

    @Test
    fun `the file room follows a case being closed`() = runTest {
        val challenges = FakeChallengeRepository(
            categories = categories,
            challenges = listOf(Challenge(1, 2, "Thirty push-ups", "", Difficulty.MEDIUM)),
        )
        val profiles = FakeProfileRepository()
        val cases = FakeCaseRepository(challenges, profiles)
        // Noon in Sarajevo on 13 November: the game day is the 13th.
        val time = FakeTimeSource(Instant.parse("2026-11-13T11:00:00Z"))
        val history = CaseHistoryUseCase(
            PlayerCaseFiles(profiles, cases),
            challenges,
            GameClock(time, FakeSettingsRepository()),
        )
        val sam = profiles.createGuest(name = "Sam", pickSeed = 42)

        history().test {
            assertThat(awaitItem().anyClosed).isFalse()
            val id = cases.accept(sam.id, 1, isDaily = false, at = time.instant, on = today)
            cases.close(id, at = time.instant, on = today, luxEarned = 25)
            val closed = expectMostRecentItem()
            assertThat(closed.categories).containsExactly(CategoryCount("Body", 1))
            assertThat(closed.luxThisWeek).isEqualTo(25)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
