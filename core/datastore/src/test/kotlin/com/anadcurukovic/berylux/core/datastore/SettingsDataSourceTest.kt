package com.anadcurukovic.berylux.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsDataSourceTest {
    private fun newStore() = InMemoryPreferencesDataStore()

    @Test
    fun `a new install gets the defaults`() = runTest {
        val settings = SettingsDataSource(newStore()).settings.first()
        assertThat(settings).isEqualTo(UserSettings(dayStartHour = 4, theme = ThemeMode.DARK, effectsLevel = 3))
    }

    @Test
    fun `changes are kept`() = runTest {
        val source = SettingsDataSource(newStore())
        source.setDayStartHour(6)
        source.setTheme(ThemeMode.DAYLIGHT)
        source.setEffectsLevel(1)
        assertThat(source.settings.first()).isEqualTo(
            UserSettings(dayStartHour = 6, theme = ThemeMode.DAYLIGHT, effectsLevel = 1),
        )
    }

    @Test
    fun `values out of range are refused`() = runTest {
        val source = SettingsDataSource(newStore())
        assertThrows(IllegalArgumentException::class.java) { runBlocking { source.setDayStartHour(24) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { source.setEffectsLevel(0) } }
    }

    @Test
    fun `broken stored values read as defaults`() = runTest {
        val store = newStore()
        store.edit {
            it[intPreferencesKey("day_start_hour")] = 99
            it[stringPreferencesKey("theme")] = "NEON"
            it[intPreferencesKey("effects_level")] = 42
        }
        assertThat(SettingsDataSource(store).settings.first()).isEqualTo(UserSettings())
    }
}
