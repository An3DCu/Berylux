package com.anadcurukovic.berylux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.anadcurukovic.berylux.core.database.entity.AchievementUnlockEntity
import com.anadcurukovic.berylux.core.database.entity.CaseEntity
import com.anadcurukovic.berylux.core.database.entity.CaseWithChallenge
import com.anadcurukovic.berylux.core.database.entity.CategoryEntity
import com.anadcurukovic.berylux.core.database.entity.ChallengeEntity
import com.anadcurukovic.berylux.core.database.entity.ProfileEntity
import com.anadcurukovic.berylux.core.model.CaseStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface ProfileDao {
    /** The guest profile, or null before the first launch has made one. */
    @Query("SELECT * FROM profiles ORDER BY id LIMIT 1")
    fun observeFirst(): Flow<ProfileEntity?>

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Query("UPDATE profiles SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE profiles SET lux = lux + :amount WHERE id = :id")
    suspend fun addLux(id: Long, amount: Int)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observe(id: Long): Flow<CategoryEntity?>
}

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY id")
    fun observeAll(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE category_id = :categoryId ORDER BY id")
    fun observeInCategory(categoryId: Long): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE is_custom = 1 ORDER BY id")
    fun observeCustom(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun get(id: Long): ChallengeEntity?

    @Insert
    suspend fun insert(challenge: ChallengeEntity): Long

    @Update
    suspend fun update(challenge: ChallengeEntity)

    /** Only custom challenges can be deleted. Returns how many rows went. */
    @Query("DELETE FROM challenges WHERE id = :id AND is_custom = 1")
    suspend fun deleteCustom(id: Long): Int
}

@Dao
interface CaseDao {
    @Transaction
    @Query("SELECT * FROM cases WHERE profile_id = :profileId ORDER BY accepted_at DESC")
    fun observeAll(profileId: Long): Flow<List<CaseWithChallenge>>

    @Transaction
    @Query("SELECT * FROM cases WHERE profile_id = :profileId AND accepted_on = :day ORDER BY accepted_at")
    fun observeAcceptedOn(profileId: Long, day: LocalDate): Flow<List<CaseWithChallenge>>

    /** The game days with at least one closed case: what streaks are counted from. */
    @Query(
        "SELECT DISTINCT closed_on FROM cases " +
            "WHERE profile_id = :profileId AND status = 'CLOSED' AND closed_on IS NOT NULL ORDER BY closed_on",
    )
    fun observeClosedDays(profileId: Long): Flow<List<LocalDate>>

    @Insert
    suspend fun insert(case: CaseEntity): Long

    @Query(
        "UPDATE cases SET status = 'CLOSED', closed_at = :at, closed_on = :on, lux_earned = :lux " +
            "WHERE id = :id AND status = 'OPEN'",
    )
    suspend fun close(id: Long, at: Instant, on: LocalDate, lux: Int): Int

    @Query("UPDATE cases SET status = :status WHERE id = :id AND status = 'OPEN'")
    suspend fun setStatusIfOpen(id: Long, status: CaseStatus): Int

    /** Cases still open from a day before [day] go cold. Returns how many did. */
    @Query("UPDATE cases SET status = 'COLD' WHERE profile_id = :profileId AND status = 'OPEN' AND accepted_on < :day")
    suspend fun markColdBefore(profileId: Long, day: LocalDate): Int
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievement_unlocks WHERE profile_id = :profileId ORDER BY unlocked_at")
    fun observeUnlocked(profileId: Long): Flow<List<AchievementUnlockEntity>>

    /** Unlocking twice keeps the first time. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(unlock: AchievementUnlockEntity): Long
}
