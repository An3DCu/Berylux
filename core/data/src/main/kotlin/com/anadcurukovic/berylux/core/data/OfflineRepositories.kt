package com.anadcurukovic.berylux.core.data

import android.database.sqlite.SQLiteConstraintException
import com.anadcurukovic.berylux.core.database.asEntity
import com.anadcurukovic.berylux.core.database.asModel
import com.anadcurukovic.berylux.core.database.dao.AchievementDao
import com.anadcurukovic.berylux.core.database.dao.CaseDao
import com.anadcurukovic.berylux.core.database.dao.CategoryDao
import com.anadcurukovic.berylux.core.database.dao.ChallengeDao
import com.anadcurukovic.berylux.core.database.dao.ProfileDao
import com.anadcurukovic.berylux.core.database.entity.AchievementUnlockEntity
import com.anadcurukovic.berylux.core.database.entity.CaseEntity
import com.anadcurukovic.berylux.core.database.entity.CaseWithChallenge
import com.anadcurukovic.berylux.core.database.entity.ChallengeEntity
import com.anadcurukovic.berylux.core.database.entity.ProfileEntity
import com.anadcurukovic.berylux.core.datastore.SettingsDataSource
import com.anadcurukovic.berylux.core.domain.repository.AchievementRepository
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

internal class OfflineProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val time: TimeSource,
) : ProfileRepository {
    override val profile: Flow<Profile?> = dao.observeFirst().map { it?.asModel() }

    override suspend fun createGuest(name: String): Profile {
        val entity = ProfileEntity(name = name, pickSeed = Random.nextLong(), createdAt = time.now())
        val id = dao.insert(entity)
        return entity.copy(id = id).asModel()
    }

    override suspend fun rename(profileId: Long, name: String) = dao.rename(profileId, name)

    override suspend fun addLux(profileId: Long, amount: Int) = dao.addLux(profileId, amount)
}

internal class OfflineChallengeRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val challengeDao: ChallengeDao,
) : ChallengeRepository {
    override val categories: Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.asModel() } }

    override val challenges: Flow<List<Challenge>> =
        challengeDao.observeAll().map { list -> list.map { it.asModel() } }

    override fun challengesIn(categoryId: Long): Flow<List<Challenge>> =
        challengeDao.observeInCategory(categoryId).map { list -> list.map { it.asModel() } }

    override suspend fun challenge(id: Long): Challenge? = challengeDao.get(id)?.asModel()

    override suspend fun addCustom(categoryId: Long, title: String, description: String, difficulty: Difficulty): Long =
        challengeDao.insert(
            ChallengeEntity(
                categoryId = categoryId,
                title = title,
                description = description,
                difficulty = difficulty,
                isCustom = true,
            ),
        )

    override suspend fun updateCustom(challenge: Challenge) {
        require(challenge.isCustom) { "only custom challenges can be changed" }
        challengeDao.update(challenge.asEntity())
    }

    override suspend fun deleteCustom(id: Long): Boolean = try {
        challengeDao.deleteCustom(id) > 0
    } catch (_: SQLiteConstraintException) {
        false
    }
}

internal class OfflineCaseRepository @Inject constructor(
    private val dao: CaseDao,
) : CaseRepository {
    override fun caseFiles(profileId: Long): Flow<List<CaseFile>> =
        dao.observeAll(profileId).map { list -> list.map { it.asCaseFile() } }

    override fun caseFilesAcceptedOn(profileId: Long, day: LocalDate): Flow<List<CaseFile>> =
        dao.observeAcceptedOn(profileId, day).map { list -> list.map { it.asCaseFile() } }

    override fun closedDays(profileId: Long): Flow<List<LocalDate>> = dao.observeClosedDays(profileId)

    override suspend fun accept(
        profileId: Long,
        challengeId: Long,
        isDaily: Boolean,
        at: Instant,
        on: LocalDate,
    ): Long =
        dao.insert(
            CaseEntity(
                profileId = profileId,
                challengeId = challengeId,
                status = CaseStatus.OPEN,
                isDaily = isDaily,
                acceptedAt = at,
                acceptedOn = on,
            ),
        )

    override suspend fun close(caseId: Long, at: Instant, on: LocalDate, luxEarned: Int): Boolean =
        dao.close(caseId, at, on, luxEarned) > 0

    override suspend fun drop(caseId: Long): Boolean = dao.setStatusIfOpen(caseId, CaseStatus.DROPPED) > 0

    private fun CaseWithChallenge.asCaseFile() = CaseFile(case = case.asModel(), challenge = challenge.asModel())
}

internal class OfflineAchievementRepository @Inject constructor(
    private val dao: AchievementDao,
) : AchievementRepository {
    override fun unlocked(profileId: Long): Flow<Set<String>> =
        dao.observeUnlocked(profileId).map { list -> list.map { it.achievementKey }.toSet() }

    override suspend fun unlock(profileId: Long, key: String, at: Instant): Boolean =
        dao.unlock(AchievementUnlockEntity(profileId = profileId, achievementKey = key, unlockedAt = at)) != -1L
}

internal class OfflineSettingsRepository @Inject constructor(
    private val source: SettingsDataSource,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = source.settings

    override suspend fun setDayStartHour(hour: Int) = source.setDayStartHour(hour)

    override suspend fun setTheme(theme: ThemeMode) = source.setTheme(theme)

    override suspend fun setEffectsLevel(level: Int) = source.setEffectsLevel(level)
}

internal class SystemTimeSource @Inject constructor() : TimeSource {
    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
