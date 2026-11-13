package com.anadcurukovic.berylux.core.domain.repository

import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MotionMode
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/*
 * What the rest of the app may ask for, with no word about where it's stored. The offline
 * versions in core:data use Room and DataStore; tests use the fakes in core:testing.
 */

interface ProfileRepository {
    /** The player's profile, or null before the first launch has made one. */
    val profile: Flow<Profile?>

    /**
     * [pickSeed] drives the daily pick and the rivals; the sigil starts from it too, so the welcome
     * screen can show the sigil first.
     */
    suspend fun createGuest(name: String, pickSeed: Long): Profile

    suspend fun rename(profileId: Long, name: String)

    /** A new sigil. The pick seed stays, so the day's picks and the rivals don't change. */
    suspend fun setSigilSeed(profileId: Long, seed: Long)

    suspend fun addLux(profileId: Long, amount: Int)
}

interface ChallengeRepository {
    val categories: Flow<List<Category>>

    val challenges: Flow<List<Challenge>>

    fun challengesIn(categoryId: Long): Flow<List<Challenge>>

    suspend fun challenge(id: Long): Challenge?

    /**
     * Custom challenges always go in "My cases". They have no difficulty: the column can't be
     * empty, so they're stored as [Difficulty.EASY], which no screen or rule ever reads.
     */
    suspend fun addCustom(title: String, description: String): Long

    /** Changes a custom challenge's title and description. False when it isn't custom or doesn't exist. */
    suspend fun updateCustom(id: Long, title: String, description: String): Boolean

    /** Deletes a custom challenge and its cases. False when it isn't custom or doesn't exist. */
    suspend fun deleteCustom(id: Long): Boolean
}

interface CaseRepository {
    fun caseFiles(profileId: Long): Flow<List<CaseFile>>

    fun caseFilesAcceptedOn(profileId: Long, day: LocalDate): Flow<List<CaseFile>>

    /** The game days with at least one closed case, oldest first. */
    fun closedDays(profileId: Long): Flow<List<LocalDate>>

    suspend fun accept(profileId: Long, challengeId: Long, isDaily: Boolean, at: Instant, on: LocalDate): Long

    /**
     * Closes an open case and adds [luxEarned] to its profile, both or neither.
     * False when the case isn't open any more.
     */
    suspend fun close(caseId: Long, at: Instant, on: LocalDate, luxEarned: Int): Boolean

    /**
     * Drops an open case and takes [penalty] Lux from its profile, both or neither. Lux never
     * goes below 0. False when the case isn't open any more.
     */
    suspend fun drop(caseId: Long, penalty: Int): Boolean

    /** Cases still open from a day before [day] go cold. Returns how many did. */
    suspend fun markColdBefore(profileId: Long, day: LocalDate): Int
}

interface AchievementRepository {
    /** The keys of the achievements the profile has unlocked, with when each one was. */
    fun unlocked(profileId: Long): Flow<Map<String, Instant>>

    /** False when it was already unlocked. */
    suspend fun unlock(profileId: Long, key: String, at: Instant): Boolean
}

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setDayStartHour(hour: Int)

    /** The start hour now, and [next] from [from] on; both null clear a waiting change. */
    suspend fun scheduleDayStartHour(current: Int, next: Int?, from: Instant?)

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setEffectsLevel(level: Int)

    suspend fun setMotion(motion: MotionMode)

    suspend fun setReminderOn(on: Boolean)

    suspend fun setReminderTime(time: LocalTime)

    /** The player's answer to the offer in Cases: yes turns the reminder on. Either way it's never offered again. */
    suspend fun answerReminderOffer(yes: Boolean)

    /** When the player last looked at Progress; unlocks after it are new. The epoch before the first look. */
    val achievementsSeenAt: Flow<Instant>

    suspend fun markAchievementsSeen(at: Instant)
}

/** The clock and the time zone, behind an interface so tests can set the time. */
interface TimeSource {
    fun now(): Instant

    fun zone(): ZoneId
}
