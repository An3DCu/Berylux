package com.anadcurukovic.berylux.core.data.di

import com.anadcurukovic.berylux.core.data.OfflineAchievementRepository
import com.anadcurukovic.berylux.core.data.OfflineCaseRepository
import com.anadcurukovic.berylux.core.data.OfflineChallengeRepository
import com.anadcurukovic.berylux.core.data.OfflineProfileRepository
import com.anadcurukovic.berylux.core.data.OfflineSettingsRepository
import com.anadcurukovic.berylux.core.data.SystemTimeSource
import com.anadcurukovic.berylux.core.domain.repository.AchievementRepository
import com.anadcurukovic.berylux.core.domain.repository.CaseRepository
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.domain.repository.ProfileRepository
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Which implementation answers each repository. The cloud round swaps these for synced ones. */
@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds fun profileRepository(impl: OfflineProfileRepository): ProfileRepository

    @Binds fun challengeRepository(impl: OfflineChallengeRepository): ChallengeRepository

    @Binds fun caseRepository(impl: OfflineCaseRepository): CaseRepository

    @Binds fun achievementRepository(impl: OfflineAchievementRepository): AchievementRepository

    @Binds fun settingsRepository(impl: OfflineSettingsRepository): SettingsRepository

    @Binds fun timeSource(impl: SystemTimeSource): TimeSource
}
