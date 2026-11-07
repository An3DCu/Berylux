package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Which game day it is now, using the player's start hour. */
class GameClock @Inject constructor(
    private val time: TimeSource,
    private val settings: SettingsRepository,
) {
    suspend fun today(): LocalDate = GameDay.of(time.now(), time.zone(), settings.settings.first().dayStartHour)

    /**
     * The game day, and again each time it changes: at the start hour, or when the player moves
     * the start hour. It looks at the clock once every [tick], which is enough for a day counter.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun days(tick: Duration = 1.minutes): Flow<LocalDate> =
        settings.settings
            .map { it.dayStartHour }
            .distinctUntilChanged()
            .flatMapLatest { hour ->
                flow {
                    while (true) {
                        emit(GameDay.of(time.now(), time.zone(), hour))
                        delay(tick)
                    }
                }
            }
            .distinctUntilChanged()
}
