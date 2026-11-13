package com.anadcurukovic.berylux.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anadcurukovic.berylux.core.model.MotionMode
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

/**
 * The player's settings, kept in Preferences DataStore. A missing or broken value reads as its
 * default. A bad file can't break the app.
 */
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            dayStartHour = prefs[DAY_START_HOUR]
                ?.takeIf { it in UserSettings.DAY_START_HOURS }
                ?: UserSettings.DEFAULT_DAY_START_HOUR,
            theme = prefs[THEME]
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.DARK,
            effectsLevel = prefs[EFFECTS_LEVEL]
                ?.takeIf { it in UserSettings.EFFECTS_LEVELS }
                ?: UserSettings.DEFAULT_EFFECTS_LEVEL,
            nextDayStartHour = prefs[NEXT_DAY_START_HOUR]?.takeIf { it in UserSettings.DAY_START_HOURS },
            nextHourFrom = prefs[NEXT_HOUR_FROM]?.let(Instant::ofEpochMilli),
            motion = prefs[MOTION]
                ?.let { name -> MotionMode.entries.firstOrNull { it.name == name } }
                ?: MotionMode.FOLLOW_PHONE,
            reminderOn = prefs[REMINDER_ON] ?: false,
            reminderTime = prefs[REMINDER_MINUTE]
                ?.takeIf { it in MINUTES_IN_DAY }
                ?.let { LocalTime.ofSecondOfDay(it * SECONDS_IN_MINUTE) }
                ?: UserSettings.DEFAULT_REMINDER_TIME,
            reminderAsked = prefs[REMINDER_ASKED] ?: false,
        )
    }

    suspend fun setDayStartHour(hour: Int) {
        require(hour in UserSettings.DAY_START_HOURS) { "hour must be 0 to 23, was $hour" }
        dataStore.edit { it[DAY_START_HOUR] = hour }
    }

    /**
     * The start hour now, and the one that takes over at [from]; both null clear a waiting change.
     * Written together. The clock never reads one without the other.
     */
    suspend fun scheduleDayStartHour(current: Int, next: Int?, from: Instant?) {
        require(current in UserSettings.DAY_START_HOURS) { "hour must be 0 to 23, was $current" }
        require(next == null || next in UserSettings.DAY_START_HOURS) { "hour must be 0 to 23, was $next" }
        dataStore.edit { prefs ->
            prefs[DAY_START_HOUR] = current
            if (next != null && from != null) {
                prefs[NEXT_DAY_START_HOUR] = next
                prefs[NEXT_HOUR_FROM] = from.toEpochMilli()
            } else {
                prefs.remove(NEXT_DAY_START_HOUR)
                prefs.remove(NEXT_HOUR_FROM)
            }
        }
    }

    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[THEME] = theme.name }
    }

    /** Not a setting the player changes: when Progress was last looked at, for its "new" marks. */
    val achievementsSeenAt: Flow<Instant> = dataStore.data.map { prefs ->
        Instant.ofEpochMilli(prefs[ACHIEVEMENTS_SEEN_AT] ?: 0L)
    }

    suspend fun markAchievementsSeen(at: Instant) {
        dataStore.edit { it[ACHIEVEMENTS_SEEN_AT] = at.toEpochMilli() }
    }

    suspend fun setEffectsLevel(level: Int) {
        require(level in UserSettings.EFFECTS_LEVELS) { "level must be 1 to 5, was $level" }
        dataStore.edit { it[EFFECTS_LEVEL] = level }
    }

    suspend fun setMotion(motion: MotionMode) {
        dataStore.edit { it[MOTION] = motion.name }
    }

    suspend fun setReminderOn(on: Boolean) {
        dataStore.edit { it[REMINDER_ON] = on }
    }

    /** Kept as the minute of the day; seconds are dropped. */
    suspend fun setReminderTime(time: LocalTime) {
        dataStore.edit { it[REMINDER_MINUTE] = time.toSecondOfDay() / SECONDS_IN_MINUTE.toInt() }
    }

    /** Written together. The offer can't come back after a yes. */
    suspend fun answerReminderOffer(yes: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMINDER_ASKED] = true
            if (yes) prefs[REMINDER_ON] = true
        }
    }

    private companion object {
        val DAY_START_HOUR = intPreferencesKey("day_start_hour")
        val THEME = stringPreferencesKey("theme")
        val EFFECTS_LEVEL = intPreferencesKey("effects_level")
        val ACHIEVEMENTS_SEEN_AT = longPreferencesKey("achievements_seen_at")
        val NEXT_DAY_START_HOUR = intPreferencesKey("next_day_start_hour")
        val NEXT_HOUR_FROM = longPreferencesKey("next_hour_from")
        val MOTION = stringPreferencesKey("motion")
        val REMINDER_ON = booleanPreferencesKey("reminder_on")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_ASKED = booleanPreferencesKey("reminder_asked")
        val MINUTES_IN_DAY = 0 until 24 * 60
        const val SECONDS_IN_MINUTE = 60L
    }
}
