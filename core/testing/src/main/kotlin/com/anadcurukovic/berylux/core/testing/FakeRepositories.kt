package com.anadcurukovic.berylux.core.testing

import com.anadcurukovic.berylux.core.domain.repository.AchievementRepository
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * In-memory repositories for ViewModel and rule tests. They behave like the offline ones:
 * the same Flows, the same "only while open" and "only custom" rules.
 */

class FakeProfileRepository(initial: Profile? = null) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: Flow<Profile?> = state

    override suspend fun createGuest(name: String, pickSeed: Long): Profile {
        val created = Profile(id = 1, name = name, lux = 0, pickSeed = pickSeed, createdAt = Instant.EPOCH)
        state.value = created
        return created
    }

    override suspend fun rename(profileId: Long, name: String) = state.update { it?.copy(name = name) }

    override suspend fun addLux(profileId: Long, amount: Int) = state.update { it?.copy(lux = it.lux + amount) }

    /** For the fake cases: the same floor at 0 as the database. */
    internal fun changeLux(profileId: Long, amount: Int) = state.update { profile ->
        if (profile == null || profile.id != profileId) {
            profile
        } else {
            profile.copy(lux = (profile.lux + amount).coerceAtLeast(0))
        }
    }
}

class FakeChallengeRepository(
    categories: List<Category> = emptyList(),
    challenges: List<Challenge> = emptyList(),
) : ChallengeRepository {
    private val categoryState = MutableStateFlow(categories)
    private val challengeState = MutableStateFlow(challenges)

    /** Called with the id of a deleted challenge, so the fake cases can go with it. */
    internal var onDeleted: (Long) -> Unit = {}

    override val categories: Flow<List<Category>> = categoryState
    override val challenges: Flow<List<Challenge>> = challengeState

    override fun challengesIn(categoryId: Long): Flow<List<Challenge>> =
        challengeState.map { list -> list.filter { it.categoryId == categoryId } }

    override suspend fun challenge(id: Long): Challenge? = challengeState.value.firstOrNull { it.id == id }

    override suspend fun addCustom(title: String, description: String, difficulty: Difficulty): Long {
        val id = (challengeState.value.maxOfOrNull { it.id } ?: 0) + 1
        challengeState.update {
            it + Challenge(id, MY_CASES_CATEGORY_ID, title, description, difficulty, isCustom = true)
        }
        return id
    }

    override suspend fun updateCustom(challenge: Challenge) {
        require(challenge.isCustom) { "only custom challenges can be changed" }
        challengeState.update { list -> list.map { if (it.id == challenge.id) challenge else it } }
    }

    override suspend fun deleteCustom(id: Long): Boolean {
        val target = challengeState.value.firstOrNull { it.id == id }
        if (target == null || !target.isCustom) return false
        challengeState.update { list -> list.filterNot { it.id == id } }
        onDeleted(id)
        return true
    }
}

class FakeCaseRepository(
    private val challenges: FakeChallengeRepository,
    private val profiles: FakeProfileRepository = FakeProfileRepository(),
) : CaseRepository {
    private val cases = MutableStateFlow(emptyList<Pair<Long, Case>>())
    private var nextId = 1L

    init {
        challenges.onDeleted = { id -> cases.update { list -> list.filterNot { it.second.challengeId == id } } }
    }

    override fun caseFiles(profileId: Long): Flow<List<CaseFile>> =
        combine(cases, challenges.challenges) { all, challengeList ->
            all.filter { it.first == profileId }
                .map { it.second }
                .sortedByDescending { it.acceptedAt }
                .map { case -> CaseFile(case, challengeList.first { it.id == case.challengeId }) }
        }

    override fun caseFilesAcceptedOn(profileId: Long, day: LocalDate): Flow<List<CaseFile>> =
        caseFiles(profileId).map { list -> list.filter { it.case.acceptedOn == day }.sortedBy { it.case.acceptedAt } }

    override fun closedDays(profileId: Long): Flow<List<LocalDate>> = cases.map { all ->
        all.filter { it.first == profileId && it.second.status == CaseStatus.CLOSED }
            .mapNotNull { it.second.closedOn }
            .distinct()
            .sorted()
    }

    override suspend fun accept(
        profileId: Long,
        challengeId: Long,
        isDaily: Boolean,
        at: Instant,
        on: LocalDate,
    ): Long {
        val id = nextId++
        val case = Case(id, challengeId, CaseStatus.OPEN, isDaily, acceptedAt = at, acceptedOn = on)
        cases.update { it + (profileId to case) }
        return id
    }

    override suspend fun close(caseId: Long, at: Instant, on: LocalDate, luxEarned: Int): Boolean =
        changeIfOpen(caseId, luxChange = luxEarned) {
            it.copy(status = CaseStatus.CLOSED, closedAt = at, closedOn = on, luxEarned = luxEarned)
        }

    override suspend fun drop(caseId: Long, penalty: Int): Boolean =
        changeIfOpen(caseId, luxChange = -penalty) { it.copy(status = CaseStatus.DROPPED) }

    override suspend fun markColdBefore(profileId: Long, day: LocalDate): Int {
        val goingCold = cases.value.filter { (owner, case) ->
            owner == profileId && case.status == CaseStatus.OPEN && case.acceptedOn.isBefore(day)
        }
        cases.update { list ->
            list.map { if (it in goingCold) it.first to it.second.copy(status = CaseStatus.COLD) else it }
        }
        return goingCold.size
    }

    private fun changeIfOpen(caseId: Long, luxChange: Int, change: (Case) -> Case): Boolean {
        val found = cases.value.firstOrNull { it.second.id == caseId && it.second.status == CaseStatus.OPEN }
            ?: return false
        cases.update { list -> list.map { if (it === found) it.first to change(it.second) else it } }
        profiles.changeLux(found.first, luxChange)
        return true
    }
}

class FakeAchievementRepository : AchievementRepository {
    private val unlocks = MutableStateFlow(emptyMap<Long, Set<String>>())

    override fun unlocked(profileId: Long): Flow<Set<String>> = unlocks.map { it[profileId].orEmpty() }

    override suspend fun unlock(profileId: Long, key: String, at: Instant): Boolean {
        val current = unlocks.value[profileId].orEmpty()
        if (key in current) return false
        unlocks.update { it + (profileId to current + key) }
        return true
    }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state

    override suspend fun setDayStartHour(hour: Int) = state.update { it.copy(dayStartHour = hour) }

    override suspend fun setTheme(theme: ThemeMode) = state.update { it.copy(theme = theme) }

    override suspend fun setEffectsLevel(level: Int) = state.update { it.copy(effectsLevel = level) }
}

/** A clock that only moves when a test moves it. */
class FakeTimeSource(
    var instant: Instant = Instant.parse("2026-11-06T12:00:00Z"),
    var zoneId: ZoneId = ZoneId.of("Europe/Sarajevo"),
) : TimeSource {
    override fun now(): Instant = instant

    override fun zone(): ZoneId = zoneId
}
