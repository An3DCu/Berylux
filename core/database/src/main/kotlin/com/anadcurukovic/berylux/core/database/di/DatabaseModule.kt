package com.anadcurukovic.berylux.core.database.di

import android.content.Context
import androidx.room.Room
import com.anadcurukovic.berylux.core.database.ALL_MIGRATIONS
import com.anadcurukovic.berylux.core.database.BeryluxDatabase
import com.anadcurukovic.berylux.core.database.dao.AchievementDao
import com.anadcurukovic.berylux.core.database.dao.CaseDao
import com.anadcurukovic.berylux.core.database.dao.CategoryDao
import com.anadcurukovic.berylux.core.database.dao.ChallengeDao
import com.anadcurukovic.berylux.core.database.dao.ProfileDao
import com.anadcurukovic.berylux.core.database.seed.SeedCallback
import com.anadcurukovic.berylux.core.database.seed.SeedData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BeryluxDatabase =
        Room.databaseBuilder(context, BeryluxDatabase::class.java, BeryluxDatabase.NAME)
            .apply { ALL_MIGRATIONS.forEach { addMigrations(it) } }
            .addCallback(SeedCallback { context.assets.open(SeedData.ASSET_PATH).use(SeedData::parse) })
            .build()

    @Provides fun profileDao(db: BeryluxDatabase): ProfileDao = db.profileDao()

    @Provides fun categoryDao(db: BeryluxDatabase): CategoryDao = db.categoryDao()

    @Provides fun challengeDao(db: BeryluxDatabase): ChallengeDao = db.challengeDao()

    @Provides fun caseDao(db: BeryluxDatabase): CaseDao = db.caseDao()

    @Provides fun achievementDao(db: BeryluxDatabase): AchievementDao = db.achievementDao()
}
