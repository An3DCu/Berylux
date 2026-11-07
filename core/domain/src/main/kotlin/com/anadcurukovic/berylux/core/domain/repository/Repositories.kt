package com.anadcurukovic.berylux.core.domain.repository

import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * What the rest of the app may ask for, with no word about where it's stored. The offline
 * versions in core:data use Room and DataStore; tests use the fakes in core:testing.
 */

interface ProfileRepository {
    /** The player's profile, or null before the first launch has made one. */
    val profile: Flow<Profile?>

    /** [pickSeed] drives the daily pick and the sigil, so the welcome screen can show it first. */
    suspend fun createGuest(name: String, pickSeed: Long): Profile

    suspend fun rename(profileId: Long, name: String)

    suspend fun addLux(profileId: Long, amount: Int)
}

interface ChallengeRepository {
    val categories: Flow<List<Category>>

    val challenges: Flow<List<Challenge>>

    fun challengesIn(categoryId: Long): Flow<List<Challenge>>

    suspend fun challenge(id: Long): Challenge?

    /** Custom challenges always go in "My cases". */
    suspend fun addCustom(title: String, description: String, difficulty: Difficulty): Long

    suspend fun updateCustom(challenge: Challenge)

    /** Deletes a custom challenge and its cases. False when it isn't custom or doesn't exist. */
    suspend fun deleteCustom(id: Long): Boolean
}

interface CaseRepository {
    fun caseFiles(profileId: Long): Flow<List<CaseFile>>

    fun caseFilesAcceptedOn(profileId: Long, day: LocalDate): Flow<List<CaseFile>>

    /** The game days with at least one closed case, oldest first. */
    fun closedDays(profileId: Long): Flow<List<LocalDate>>

    suspend fun accept(profileId: Long, challengeId: Long, isDaily: Boolean, at: Instant, on: LocalDate): Long

    /** False when the case isn't open any more. */
    suspend fun close(caseId: Long, at: Instant, on: LocalDate, luxEarned: Int): Boolean

    /** False when the case isn't open any more. */
    suspend fun drop(caseId: Long): Boolean

    /** Cases still open from a day before [day] go cold. Returns how many did. */
    suspend fun markColdBefore(profileId: Long, day: LocalDate): Int
}

interface AchievementRepository {
    /** The keys of the achievements the profile has unlocked. */
    fun unlocked(profileId: Long): Flow<Set<String>>

    /** False when it was already unlocked. */
    suspend fun unlock(profileId: Long, key: String, at: Instant): Boolean
}

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setDayStartHour(hour: Int)

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setEffectsLevel(level: Int)
}

/** The clock and the time zone, behind an interface so tests can set the time. */
interface TimeSource {
    fun now(): Instant

    fun zone(): ZoneId
}
