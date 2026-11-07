package com.anadcurukovic.berylux.core.database

import com.anadcurukovic.berylux.core.database.entity.AchievementUnlockEntity
import com.anadcurukovic.berylux.core.database.entity.CaseEntity
import com.anadcurukovic.berylux.core.database.entity.ChallengeEntity
import com.anadcurukovic.berylux.core.database.entity.ProfileEntity
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: BeryluxDatabase
    private val now = Instant.parse("2026-11-06T18:00:00Z")
    private val today = LocalDate.of(2026, 11, 6)

    @Before
    fun open() {
        db = seededInMemoryDatabase()
    }

    @After
    fun close() {
        db.close()
    }

    private suspend fun newProfile(): Long =
        db.profileDao().insert(ProfileEntity(name = "Guest", pickSeed = 42, createdAt = now))

    private suspend fun accept(profileId: Long, challengeId: Long, on: LocalDate = today): Long =
        db.caseDao().insert(
            CaseEntity(
                profileId = profileId,
                challengeId = challengeId,
                status = CaseStatus.OPEN,
                isDaily = false,
                acceptedAt = now,
                acceptedOn = on,
            ),
        )

    private suspend fun insertCustom(): Long = db.challengeDao().insert(
        ChallengeEntity(
            categoryId = MY_CASES_CATEGORY_ID,
            title = "My own",
            description = "",
            difficulty = Difficulty.EASY,
            isCustom = true,
        ),
    )

    @Test
    fun `a profile starts with no Lux and adds it up`() = runTest {
        val id = newProfile()
        db.profileDao().addLux(id, 25)
        db.profileDao().addLux(id, 50)
        db.profileDao().rename(id, "Noir")
        val profile = db.profileDao().observeFirst().first()!!
        assertThat(profile.lux).isEqualTo(75)
        assertThat(profile.name).isEqualTo("Noir")
        assertThat(profile.createdAt).isEqualTo(now)
    }

    @Test
    fun `no profile before the first one is made`() = runTest {
        assertThat(db.profileDao().observeFirst().first()).isNull()
    }

    @Test
    fun `a custom challenge gets an id above the seed`() = runTest {
        val id = insertCustom()
        assertThat(id).isGreaterThan(56L)
        assertThat(db.challengeDao().observeCustom().first().map { it.title }).containsExactly("My own")
        assertThat(db.challengeDao().observeInCategory(MY_CASES_CATEGORY_ID).first()).hasSize(1)
    }

    @Test
    fun `only custom challenges can be deleted`() = runTest {
        val customId = insertCustom()
        assertThat(db.challengeDao().deleteCustom(1)).isEqualTo(0)
        assertThat(db.challengeDao().deleteCustom(customId)).isEqualTo(1)
        assertThat(db.challengeDao().get(1)).isNotNull()
    }

    @Test
    fun `deleting a custom challenge deletes its cases too`() = runTest {
        val profileId = newProfile()
        val customId = insertCustom()
        val caseId = accept(profileId, customId)
        db.caseDao().close(caseId, at = now, on = today, lux = 0)
        accept(profileId, challengeId = 1)

        assertThat(db.challengeDao().deleteCustom(customId)).isEqualTo(1)
        val left = db.caseDao().observeAll(profileId).first().map { it.challenge.id }
        assertThat(left).containsExactly(1L)
        assertThat(db.caseDao().observeClosedDays(profileId).first()).isEmpty()
    }

    @Test
    fun `cases left open from an earlier day go cold, today's stay open`() = runTest {
        val profileId = newProfile()
        val yesterdays = accept(profileId, challengeId = 1, on = today.minusDays(1))
        val closedYesterday = accept(profileId, challengeId = 2, on = today.minusDays(1))
        db.caseDao().close(closedYesterday, at = now, on = today.minusDays(1), lux = 10)
        accept(profileId, challengeId = 3)

        assertThat(db.caseDao().markColdBefore(profileId, today)).isEqualTo(1)
        val statuses = db.caseDao().observeAll(profileId).first().associate { it.case.id to it.case.status }
        assertThat(statuses[yesterdays]).isEqualTo(CaseStatus.COLD)
        assertThat(statuses[closedYesterday]).isEqualTo(CaseStatus.CLOSED)
        assertThat(statuses.values.count { it == CaseStatus.OPEN }).isEqualTo(1)
        assertThat(db.caseDao().close(yesterdays, at = now, on = today, lux = 10)).isEqualTo(0)
    }

    @Test
    fun `a case can be closed once`() = runTest {
        val profileId = newProfile()
        val caseId = accept(profileId, challengeId = 4)
        assertThat(db.caseDao().close(caseId, at = now, on = today, lux = 25)).isEqualTo(1)
        assertThat(db.caseDao().close(caseId, at = now, on = today, lux = 25)).isEqualTo(0)

        val case = db.caseDao().observeAll(profileId).first().single()
        assertThat(case.case.status).isEqualTo(CaseStatus.CLOSED)
        assertThat(case.case.luxEarned).isEqualTo(25)
        assertThat(case.case.closedOn).isEqualTo(today)
        assertThat(case.challenge.title).isEqualTo("Brisk walk")
    }

    @Test
    fun `a closed case can't be dropped`() = runTest {
        val profileId = newProfile()
        val caseId = accept(profileId, challengeId = 1)
        db.caseDao().close(caseId, at = now, on = today, lux = 10)
        assertThat(db.caseDao().setStatusIfOpen(caseId, CaseStatus.DROPPED)).isEqualTo(0)
    }

    @Test
    fun `cases are listed by the day they were accepted`() = runTest {
        val profileId = newProfile()
        accept(profileId, challengeId = 1, on = today.minusDays(1))
        accept(profileId, challengeId = 2)
        accept(profileId, challengeId = 3)
        val titles = db.caseDao().observeAcceptedOn(profileId, today).first().map { it.challenge.title }
        assertThat(titles).containsExactly("Stairs only", "Water first")
    }

    @Test
    fun `closed days come back once each, in order, without dropped or open cases`() = runTest {
        val profileId = newProfile()
        val days = listOf(today, today.minusDays(2), today)
        days.forEachIndexed { index, day ->
            val id = accept(profileId, challengeId = index + 1L, on = day)
            db.caseDao().close(id, at = now, on = day, lux = 10)
        }
        val dropped = accept(profileId, challengeId = 9, on = today.minusDays(1))
        db.caseDao().setStatusIfOpen(dropped, CaseStatus.DROPPED)
        accept(profileId, challengeId = 10, on = today.minusDays(3))

        assertThat(db.caseDao().observeClosedDays(profileId).first())
            .containsExactly(today.minusDays(2), today)
            .inOrder()
    }

    @Test
    fun `unlocking an achievement twice keeps the first time`() = runTest {
        val profileId = newProfile()
        db.achievementDao().unlock(AchievementUnlockEntity(profileId, "first_case", now))
        db.achievementDao().unlock(AchievementUnlockEntity(profileId, "first_case", now.plusSeconds(60)))
        val unlocked = db.achievementDao().observeUnlocked(profileId).first()
        assertThat(unlocked).hasSize(1)
        assertThat(unlocked.single().unlockedAt).isEqualTo(now)
    }
}
