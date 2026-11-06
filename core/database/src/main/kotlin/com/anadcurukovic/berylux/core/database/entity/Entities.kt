package com.anadcurukovic.berylux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Difficulty
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lux: Int = 0,
    @ColumnInfo(name = "pick_seed") val pickSeed: Long,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String,
    val kind: CategoryKind,
)

/** Seed challenges keep the ids from the seed file; custom ones get new ids above them. */
@Entity(
    tableName = "challenges",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_id")],
)
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean = false,
)

/**
 * A case keeps its challenge: a challenge with cases can't be deleted. Removing a custom challenge
 * without losing its history is decided with the editor in the core loop round.
 */
@Entity(
    tableName = "cases",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["challenge_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("challenge_id"),
        Index(value = ["profile_id", "accepted_on"]),
        Index(value = ["profile_id", "closed_on"]),
    ],
)
data class CaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "profile_id") val profileId: Long,
    @ColumnInfo(name = "challenge_id") val challengeId: Long,
    val status: CaseStatus,
    @ColumnInfo(name = "is_daily") val isDaily: Boolean,
    @ColumnInfo(name = "accepted_at") val acceptedAt: Instant,
    @ColumnInfo(name = "accepted_on") val acceptedOn: LocalDate,
    @ColumnInfo(name = "closed_at") val closedAt: Instant? = null,
    @ColumnInfo(name = "closed_on") val closedOn: LocalDate? = null,
    @ColumnInfo(name = "lux_earned") val luxEarned: Int = 0,
)

/**
 * Which achievements a profile has unlocked. The achievements themselves are rules in code
 * (core loop round), so only the key and the moment are stored.
 */
@Entity(
    tableName = "achievement_unlocks",
    primaryKeys = ["profile_id", "achievement_key"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AchievementUnlockEntity(
    @ColumnInfo(name = "profile_id") val profileId: Long,
    @ColumnInfo(name = "achievement_key") val achievementKey: String,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Instant,
)

/** A case with the challenge it's about, for lists like the tracker. */
data class CaseWithChallenge(
    @Embedded val case: CaseEntity,
    @Relation(parentColumn = "challenge_id", entityColumn = "id")
    val challenge: ChallengeEntity,
)
