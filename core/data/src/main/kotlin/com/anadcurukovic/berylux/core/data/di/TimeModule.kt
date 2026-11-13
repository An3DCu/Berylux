package com.anadcurukovic.berylux.core.data.di

import com.anadcurukovic.berylux.core.data.SystemTimeSource
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The clock in a module of its own. A test can swap just the clock with `@TestInstallIn` and keep
 * every repository as it is.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides fun timeSource(): TimeSource = SystemTimeSource()
}
