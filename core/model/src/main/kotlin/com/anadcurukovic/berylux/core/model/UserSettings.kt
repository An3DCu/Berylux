package com.anadcurukovic.berylux.core.model

import java.time.Instant
import java.time.LocalTime

enum class ThemeMode {
    DARK,
    DAYLIGHT,
    SYSTEM,
}

/** How much the app moves: as the phone's animation setting says, always, or calmly. */
enum class MotionMode {
    FOLLOW_PHONE,
    FULL,
    REDUCED,
    ;

    /** Whether to move calmly, given whether the phone has turned its animations off. */
    fun isReduced(phoneRemovesAnimations: Boolean): Boolean = when (this) {
        FOLLOW_PHONE -> phoneRemovesAnimations
        FULL -> false
        REDUCED -> true
    }
}

/**
 * @property dayStartHour the hour a new game day starts, 0 to 23.
 * @property nextDayStartHour a new start hour the player picked, waiting for [nextHourFrom]: the end
 *   of the game day it was picked on. Today keeps its end; the new hour starts with the next day.
 * @property effectsLevel how strong grain, scanlines and glow are, 1 (clean) to 5 (heavy).
 * @property motion full or reduced motion, or whatever the phone's animation setting says.
 * @property reminderOn whether the morning docket, the daily reminder, is sent.
 * @property reminderTime when it's sent, on the phone's clock.
 * @property reminderAsked whether the player has answered the offer in Cases; it's never made again.
 */
data class UserSettings(
    val dayStartHour: Int = DEFAULT_DAY_START_HOUR,
    val theme: ThemeMode = ThemeMode.DARK,
    val effectsLevel: Int = DEFAULT_EFFECTS_LEVEL,
    val nextDayStartHour: Int? = null,
    val nextHourFrom: Instant? = null,
    val motion: MotionMode = MotionMode.FOLLOW_PHONE,
    val reminderOn: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    val reminderAsked: Boolean = false,
) {
    /** The hour the game day starts at [instant]: the next hour once its time has come. */
    fun startHourAt(instant: Instant): Int =
        if (nextDayStartHour != null && nextHourFrom != null && !instant.isBefore(nextHourFrom)) {
            nextDayStartHour
        } else {
            dayStartHour
        }

    /** The hour still waiting to take effect at [instant], or null when none is. */
    fun pendingStartHourAt(instant: Instant): Int? =
        nextDayStartHour?.takeIf { nextHourFrom != null && instant.isBefore(nextHourFrom) }

    companion object {
        const val DEFAULT_DAY_START_HOUR = 4
        const val DEFAULT_EFFECTS_LEVEL = 3
        val DAY_START_HOURS = 0..23
        val EFFECTS_LEVELS = 1..5
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
    }
}
