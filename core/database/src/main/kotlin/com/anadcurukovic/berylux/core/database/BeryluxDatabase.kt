package com.anadcurukovic.berylux.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anadcurukovic.berylux.core.database.dao.AchievementDao
import com.anadcurukovic.berylux.core.database.dao.CaseDao
import com.anadcurukovic.berylux.core.database.dao.CategoryDao
import com.anadcurukovic.berylux.core.database.dao.ChallengeDao
import com.anadcurukovic.berylux.core.database.dao.ProfileDao
import com.anadcurukovic.berylux.core.database.entity.AchievementUnlockEntity
import com.anadcurukovic.berylux.core.database.entity.CaseEntity
import com.anadcurukovic.berylux.core.database.entity.CategoryEntity
import com.anadcurukovic.berylux.core.database.entity.ChallengeEntity
import com.anadcurukovic.berylux.core.database.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        CategoryEntity::class,
        ChallengeEntity::class,
        CaseEntity::class,
        AchievementUnlockEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(TimeConverters::class)
abstract class BeryluxDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun caseDao(): CaseDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val NAME = "berylux.db"
    }
}
