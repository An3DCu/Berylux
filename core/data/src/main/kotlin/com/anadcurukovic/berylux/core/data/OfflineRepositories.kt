package com.anadcurukovic.berylux.core.data

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
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.anadcurukovic.berylux.core.model.MotionMode
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

internal class OfflineProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val time: TimeSource,
) : ProfileRepository {
    override val profile: Flow<Profile?> = dao.observeFirst().map { it?.asModel() }

    override suspend fun createGuest(name: String, pickSeed: Long): Profile {
        val entity = ProfileEntity(name = name, pickSeed = pickSeed, createdAt = time.now())
        val id = dao.insert(entity)
        return entity.copy(id = id).asModel()
    }

    override suspend fun rename(profileId: Long, name: String) = dao.rename(profileId, name)

    override suspend fun setSigilSeed(profileId: Long, seed: Long) = dao.setSigilSeed(profileId, seed)

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

    override suspend fun addCustom(title: String, description: String): Long =
        challengeDao.insert(
            ChallengeEntity(
                categoryId = MY_CASES_CATEGORY_ID,
                title = title,
                description = description,
                difficulty = Difficulty.EASY,
                isCustom = true,
            ),
        )

    override suspend fun updateCustom(id: Long, title: String, description: String): Boolean =
        challengeDao.updateCustom(id, title, description) > 0

    override suspend fun deleteCustom(id: Long): Boolean = challengeDao.deleteCustom(id) > 0
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
        dao.closeAndPay(caseId, at, on, luxEarned)

    override suspend fun drop(caseId: Long, penalty: Int): Boolean = dao.dropAndCharge(caseId, penalty)

    override suspend fun markColdBefore(profileId: Long, day: LocalDate): Int = dao.markColdBefore(profileId, day)

    private fun CaseWithChallenge.asCaseFile() = CaseFile(case = case.asModel(), challenge = challenge.asModel())
}

internal class OfflineAchievementRepository @Inject constructor(
    private val dao: AchievementDao,
) : AchievementRepository {
    override fun unlocked(profileId: Long): Flow<Map<String, Instant>> =
        dao.observeUnlocked(profileId).map { list -> list.associate { it.achievementKey to it.unlockedAt } }

    override suspend fun unlock(profileId: Long, key: String, at: Instant): Boolean =
        dao.unlock(AchievementUnlockEntity(profileId = profileId, achievementKey = key, unlockedAt = at)) != -1L
}

internal class OfflineSettingsRepository @Inject constructor(
    private val source: SettingsDataSource,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = source.settings

    override suspend fun setDayStartHour(hour: Int) = source.setDayStartHour(hour)

    override suspend fun scheduleDayStartHour(current: Int, next: Int?, from: Instant?) =
        source.scheduleDayStartHour(current, next, from)

    override suspend fun setTheme(theme: ThemeMode) = source.setTheme(theme)

    override suspend fun setEffectsLevel(level: Int) = source.setEffectsLevel(level)

    override suspend fun setMotion(motion: MotionMode) = source.setMotion(motion)

    override suspend fun setReminderOn(on: Boolean) = source.setReminderOn(on)

    override suspend fun setReminderTime(time: LocalTime) = source.setReminderTime(time)

    override suspend fun answerReminderOffer(yes: Boolean) = source.answerReminderOffer(yes)

    override val achievementsSeenAt: Flow<Instant> = source.achievementsSeenAt

    override suspend fun markAchievementsSeen(at: Instant) = source.markAchievementsSeen(at)
}

internal class SystemTimeSource @Inject constructor() : TimeSource {
    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
