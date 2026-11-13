package com.anadcurukovic.berylux.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.anadcurukovic.berylux.core.model.MotionMode
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

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
    fun `a waiting start hour is kept with its time, and clearing it removes both`() = runTest {
        val source = SettingsDataSource(newStore())
        val from = Instant.parse("2026-11-10T03:00:00Z")
        source.scheduleDayStartHour(current = 4, next = 6, from = from)
        source.settings.first().let {
            assertThat(it.dayStartHour).isEqualTo(4)
            assertThat(it.nextDayStartHour).isEqualTo(6)
            assertThat(it.nextHourFrom).isEqualTo(from)
        }

        source.scheduleDayStartHour(current = 4, next = null, from = null)
        source.settings.first().let {
            assertThat(it.nextDayStartHour).isNull()
            assertThat(it.nextHourFrom).isNull()
        }
    }

    @Test
    fun `values out of range are refused`() = runTest {
        val source = SettingsDataSource(newStore())
        assertThrows(IllegalArgumentException::class.java) { runBlocking { source.setDayStartHour(24) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { source.setEffectsLevel(0) } }
    }

    @Test
    fun `motion is kept, and an unknown one reads as following the phone`() = runTest {
        val store = newStore()
        val source = SettingsDataSource(store)
        source.setMotion(MotionMode.REDUCED)
        assertThat(source.settings.first().motion).isEqualTo(MotionMode.REDUCED)

        store.edit { it[stringPreferencesKey("motion")] = "SLOW" }
        assertThat(source.settings.first().motion).isEqualTo(MotionMode.FOLLOW_PHONE)
    }

    @Test
    fun `the morning docket starts off at 9, and its time is kept to the minute`() = runTest {
        val source = SettingsDataSource(newStore())
        source.settings.first().let {
            assertThat(it.reminderOn).isFalse()
            assertThat(it.reminderTime).isEqualTo(LocalTime.of(9, 0))
            assertThat(it.reminderAsked).isFalse()
        }
        source.setReminderTime(LocalTime.of(7, 45, 30))
        source.setReminderOn(true)
        assertThat(source.settings.first().reminderTime).isEqualTo(LocalTime.of(7, 45))
        assertThat(source.settings.first().reminderOn).isTrue()
    }

    @Test
    fun `answering the offer marks it answered, and only yes turns the reminder on`() = runTest {
        val no = SettingsDataSource(newStore())
        no.answerReminderOffer(yes = false)
        assertThat(no.settings.first().reminderAsked).isTrue()
        assertThat(no.settings.first().reminderOn).isFalse()

        val yes = SettingsDataSource(newStore())
        yes.answerReminderOffer(yes = true)
        assertThat(yes.settings.first().reminderAsked).isTrue()
        assertThat(yes.settings.first().reminderOn).isTrue()
    }

    @Test
    fun `a stored minute outside the day reads as 9 AM`() = runTest {
        val store = newStore()
        store.edit { it[intPreferencesKey("reminder_minute")] = 1440 }
        assertThat(SettingsDataSource(store).settings.first().reminderTime).isEqualTo(LocalTime.of(9, 0))
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
