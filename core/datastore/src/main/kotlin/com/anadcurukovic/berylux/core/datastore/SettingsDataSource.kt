package com.anadcurukovic.berylux.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The player's settings, kept in Preferences DataStore. A missing or broken value reads as its
 * default, so a bad file can't break the app.
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
        )
    }

    suspend fun setDayStartHour(hour: Int) {
        require(hour in UserSettings.DAY_START_HOURS) { "hour must be 0 to 23, was $hour" }
        dataStore.edit { it[DAY_START_HOUR] = hour }
    }

    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[THEME] = theme.name }
    }

    suspend fun setEffectsLevel(level: Int) {
        require(level in UserSettings.EFFECTS_LEVELS) { "level must be 1 to 5, was $level" }
        dataStore.edit { it[EFFECTS_LEVEL] = level }
    }

    private companion object {
        val DAY_START_HOUR = intPreferencesKey("day_start_hour")
        val THEME = stringPreferencesKey("theme")
        val EFFECTS_LEVEL = intPreferencesKey("effects_level")
    }
}
