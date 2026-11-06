package com.anadcurukovic.berylux.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anadcurukovic.berylux.core.database.BeryluxDatabase
import com.anadcurukovic.berylux.core.database.seed.SeedCallback
import com.anadcurukovic.berylux.core.database.seed.SeedData
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Difficulty
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class OfflineRepositoriesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, BeryluxDatabase::class.java)
        .addCallback(SeedCallback { context.assets.open(SeedData.ASSET_PATH).use(SeedData::parse) })
        .allowMainThreadQueries()
        .build()

    private val now = Instant.parse("2026-11-06T18:00:00Z")
    private val today = LocalDate.of(2026, 11, 6)
    private val time = object : TimeSource {
        override fun now() = now
        override fun zone(): ZoneId = ZoneId.of("Europe/Sarajevo")
    }

    private val profiles = OfflineProfileRepository(db.profileDao(), time)
    private val challenges = OfflineChallengeRepository(db.categoryDao(), db.challengeDao())
    private val cases = OfflineCaseRepository(db.caseDao())
    private val achievements = OfflineAchievementRepository(db.achievementDao())

    @After
    fun close() = db.close()

    @Test
    fun `a guest profile is created with a seed of its own`() = runTest {
        assertThat(profiles.profile.first()).isNull()
        val created = profiles.createGuest("Guest")
        val stored = profiles.profile.first()!!
        assertThat(stored).isEqualTo(created)
        assertThat(stored.createdAt).isEqualTo(now)
        profiles.addLux(created.id, 38)
        assertThat(profiles.profile.first()!!.lux).isEqualTo(38)
    }

    @Test
    fun `the seed comes through as models`() = runTest {
        assertThat(challenges.categories.first()).hasSize(7)
        assertThat(challenges.challenges.first()).hasSize(56)
        assertThat(challenges.challengesIn(7).first().all { it.difficulty == Difficulty.HARD }).isTrue()
        assertThat(challenges.challenge(1)!!.title).isEqualTo("Stretch break")
    }

    @Test
    fun `custom challenges can be added, changed and deleted until they have cases`() = runTest {
        val profile = profiles.createGuest("Guest")
        val id = challenges.addCustom(
            categoryId = 2,
            title = "Read a poem",
            description = "",
            difficulty = Difficulty.EASY,
        )
        challenges.updateCustom(challenges.challenge(id)!!.copy(title = "Read two poems"))
        assertThat(challenges.challenge(id)!!.title).isEqualTo("Read two poems")

        cases.accept(profile.id, id, isDaily = false, at = now, on = today)
        assertThat(challenges.deleteCustom(id)).isFalse()
        assertThat(challenges.deleteCustom(1)).isFalse()
    }

    @Test
    fun `a case is accepted, closed once and shows up with its challenge`() = runTest {
        val profile = profiles.createGuest("Guest")
        val caseId = cases.accept(profile.id, challengeId = 4, isDaily = true, at = now, on = today)
        assertThat(cases.close(caseId, at = now, on = today, luxEarned = 38)).isTrue()
        assertThat(cases.close(caseId, at = now, on = today, luxEarned = 38)).isFalse()
        assertThat(cases.drop(caseId)).isFalse()

        val file = cases.caseFilesAcceptedOn(profile.id, today).first().single()
        assertThat(file.challenge.title).isEqualTo("Brisk walk")
        assertThat(file.case.status).isEqualTo(CaseStatus.CLOSED)
        assertThat(file.case.isDaily).isTrue()
        assertThat(cases.closedDays(profile.id).first()).containsExactly(today)
    }

    @Test
    fun `an achievement unlocks once`() = runTest {
        val profile = profiles.createGuest("Guest")
        assertThat(achievements.unlock(profile.id, "first_case", now)).isTrue()
        assertThat(achievements.unlock(profile.id, "first_case", now)).isFalse()
        assertThat(achievements.unlocked(profile.id).first()).containsExactly("first_case")
    }
}
