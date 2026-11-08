package com.anadcurukovic.berylux.di

import com.anadcurukovic.berylux.core.data.di.TimeModule
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

/** Every app test runs on a clock it moves itself. The rest of the app stays as it ships. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [TimeModule::class])
object TestTimeModule {
    /** 9 PM in Sarajevo on Sunday 8 November 2026, seven hours before the game day ends. */
    val START: Instant = Instant.parse("2026-11-08T20:00:00Z")
    val ZONE: ZoneId = ZoneId.of("Europe/Sarajevo")

    @Provides
    @Singleton
    fun clock(): FakeTimeSource = FakeTimeSource(START, ZONE)

    @Provides
    fun timeSource(clock: FakeTimeSource): TimeSource = clock
}
