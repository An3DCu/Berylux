package com.anadcurukovic.berylux.core.domain

import app.cash.turbine.test
import com.anadcurukovic.berylux.core.model.Achievement
import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.testing.FakeAchievementRepository
import com.anadcurukovic.berylux.core.testing.FakeCaseRepository
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AchievementRulesTest {
    private val today = LocalDate.of(2026, 11, 6)
    private val categories = (1L..6L).map { Category(it, "Regular $it", "", CategoryKind.REGULAR) } +
        Category(7, "Long shots", "", CategoryKind.BONUS) +
        Category(MY_CASES_CATEGORY_ID, "My cases", "", CategoryKind.CUSTOM)
    private val noStreak = Streaks.calculate(emptyList(), today)
    private var nextId = 1L

    private fun case(
        categoryId: Long = 1,
        difficulty: Difficulty = Difficulty.EASY,
        status: CaseStatus = CaseStatus.CLOSED,
        on: LocalDate = today,
        custom: Boolean = false,
    ): CaseFile {
        val id = nextId++
        return CaseFile(
            case = Case(
                id = id,
                challengeId = id,
                status = status,
                isDaily = false,
                acceptedAt = Instant.EPOCH,
                acceptedOn = on,
                closedOn = on.takeIf { status == CaseStatus.CLOSED },
            ),
            challenge = Challenge(id, categoryId, "Challenge $id", "", difficulty, isCustom = custom),
        )
    }

    private fun earned(files: List<CaseFile>, lux: Int = 0, streak: StreakState = noStreak) =
        AchievementRules.earned(files, categories, lux, streak)

    @Test
    fun `nothing closed, nothing earned`() {
        assertThat(earned(listOf(case(status = CaseStatus.OPEN), case(status = CaseStatus.DROPPED)))).isEmpty()
    }

    @Test
    fun `closed counts unlock at 1, 10, 50 and 100`() {
        // Spread over days, so a full day doesn't come along.
        fun closed(n: Int) = (0 until n).map { case(on = today.minusDays(it.toLong())) }
        assertThat(earned(closed(1))).containsExactly(Achievement.FIRST_CLOSED)
        assertThat(earned(closed(9))).doesNotContain(Achievement.CLOSED_10)
        assertThat(earned(closed(10))).contains(Achievement.CLOSED_10)
        assertThat(earned(closed(50))).contains(Achievement.CLOSED_50)
        assertThat(earned(closed(99))).doesNotContain(Achievement.CLOSED_100)
        assertThat(earned(closed(100))).contains(Achievement.CLOSED_100)
    }

    @Test
    fun `a full day needs 5 closed on the same day`() {
        assertThat(earned(List(4) { case() } + case(on = today.minusDays(1)))).doesNotContain(Achievement.FULL_DAY)
        assertThat(earned(List(5) { case() })).contains(Achievement.FULL_DAY)
    }

    @Test
    fun `hard ten counts built-in Hard cases only`() {
        val nine = List(9) { case(difficulty = Difficulty.HARD, on = today.minusDays(it.toLong())) }
        // Stored as Easy, but your own never count, whatever the column says.
        val custom = case(categoryId = MY_CASES_CATEGORY_ID, difficulty = Difficulty.HARD, custom = true)
        assertThat(earned(nine + custom)).doesNotContain(Achievement.HARD_10)
        assertThat(earned(nine + case(difficulty = Difficulty.HARD, on = today.minusDays(20))))
            .contains(Achievement.HARD_10)
    }

    @Test
    fun `five closed Long shots unlock the bonus one`() {
        val four = List(4) { case(categoryId = 7, on = today.minusDays(it.toLong())) }
        assertThat(earned(four)).doesNotContain(Achievement.BONUS_5)
        assertThat(earned(four + case(categoryId = 7, on = today.minusDays(9)))).contains(Achievement.BONUS_5)
    }

    @Test
    fun `every built-in category needs a closed case, My cases doesn't count`() {
        val sixRegular = (1L..6L).map { case(categoryId = it, on = today.minusDays(it)) }
        assertThat(earned(sixRegular)).doesNotContain(Achievement.EVERY_CATEGORY)
        assertThat(earned(sixRegular + case(categoryId = 7, status = CaseStatus.OPEN)))
            .doesNotContain(Achievement.EVERY_CATEGORY)
        assertThat(earned(sixRegular + case(categoryId = 7, on = today.minusDays(9))))
            .contains(Achievement.EVERY_CATEGORY)
    }

    @Test
    fun `your own case unlocks when one is closed, not when it's only taken`() {
        val own = { status: CaseStatus -> case(categoryId = MY_CASES_CATEGORY_ID, status = status, custom = true) }
        assertThat(earned(listOf(own(CaseStatus.OPEN)))).doesNotContain(Achievement.OWN_CASE)
        assertThat(earned(listOf(own(CaseStatus.CLOSED)))).contains(Achievement.OWN_CASE)
    }

    @Test
    fun `streaks count the longest one, and any freeze ever used counts`() {
        val week = (0L..6L).map { today.minusDays(it) }
        assertThat(earned(emptyList(), streak = Streaks.calculate(week, today))).contains(Achievement.STREAK_7)
        // Seven days, one missed day saved by the freeze, then the streak broke for good.
        val saved = (20L..26L).map { today.minusDays(it) } + today.minusDays(18)
        val streak = Streaks.calculate(saved, today)
        assertThat(streak.freezesUsed).isEqualTo(1)
        assertThat(streak.frozenDays).isEmpty()
        assertThat(earned(emptyList(), streak = streak)).contains(Achievement.FREEZE_SAVED)
        assertThat(earned(emptyList(), streak = streak)).doesNotContain(Achievement.STREAK_30)
    }

    @Test
    fun `each gem rank unlocks its own and those below`() {
        assertThat(earned(emptyList(), lux = 499)).isEmpty()
        assertThat(earned(emptyList(), lux = 5_000)).containsExactly(
            Achievement.RANK_HELIODOR,
            Achievement.RANK_MORGANITE,
            Achievement.RANK_AQUAMARINE,
        )
        assertThat(earned(emptyList(), lux = 22_000)).contains(Achievement.RANK_RED_BERYL)
    }

    @Test
    fun `there are 17, with keys that never repeat`() {
        assertThat(Achievement.entries).hasSize(17)
        assertThat(Achievement.entries.map { it.key }.toSet()).hasSize(17)
    }
}

class UnlockAchievementsUseCaseTest {
    private val time = FakeTimeSource(Instant.parse("2026-11-06T14:00:00Z"))
    private val today = LocalDate.of(2026, 11, 6)
    private val challenges = FakeChallengeRepository(
        // Two categories, so one closed case doesn't also cover every category.
        categories = listOf(
            Category(1, "Body", "", CategoryKind.REGULAR),
            Category(2, "Mind", "", CategoryKind.REGULAR),
        ),
        challenges = (1L..3L).map { Challenge(it, 1, "Body $it", "", Difficulty.EASY) },
    )
    private val profiles = FakeProfileRepository(
        Profile(id = 1, name = "Sam", lux = 0, pickSeed = 42, createdAt = Instant.EPOCH),
    )
    private val cases = FakeCaseRepository(challenges, profiles)
    private val achievements = FakeAchievementRepository()
    private val unlock = UnlockAchievementsUseCase(
        profiles,
        challenges,
        cases,
        achievements,
        GameClock(time, FakeSettingsRepository()),
        time,
    )

    @Test
    fun `a newly earned one is stored and comes through once`() = runTest {
        unlock().test {
            val first = cases.accept(1, 1, isDaily = false, at = time.now(), on = today)
            cases.close(first, time.now(), today, luxEarned = 10)
            assertThat(awaitItem()).containsExactly(Achievement.FIRST_CLOSED)

            // Another case changes the record, but nothing new is earned: no emission.
            val second = cases.accept(1, 2, isDaily = false, at = time.now(), on = today)
            cases.close(second, time.now(), today, luxEarned = 10)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(achievements.unlocked(1).first()).containsExactly(Achievement.FIRST_CLOSED.key, time.now())
    }

    @Test
    fun `an unlock stays after the case that earned it is gone`() = runTest {
        achievements.unlock(1, Achievement.FIRST_CLOSED.key, time.now())
        unlock().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(achievements.unlocked(1).first()).containsKey(Achievement.FIRST_CLOSED.key)
    }
}
