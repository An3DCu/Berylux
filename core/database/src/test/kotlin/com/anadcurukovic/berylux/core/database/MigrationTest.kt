package com.anadcurukovic.berylux.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Each test opens an old exported schema, fills it, migrates and checks the data survived.
 * `runMigrationsAndValidate` also makes Room compare the result with the new exported schema.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = context().getDatabasePath("migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = BeryluxDatabase::class,
    )

    private fun SQLiteConnection.longs(sql: String): List<Long> {
        val statement = prepare(sql)
        val values = mutableListOf<Long>()
        while (statement.step()) values += statement.getLong(0)
        statement.close()
        return values
    }

    private fun SQLiteConnection.text(sql: String): String {
        val statement = prepare(sql)
        statement.step()
        return statement.getText(0).also { statement.close() }
    }

    @Test
    fun `version 1 can be created from its exported schema`() {
        val connection = helper.createDatabase(1)
        assertThat(connection.longs("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'cases'"))
            .containsExactly(1L)
        connection.close()
    }

    @Test
    fun `1 to 2 keeps every case and adds My cases`() {
        helper.createDatabase(1).apply {
            execSQL("INSERT INTO profiles (id, name, lux, pick_seed, created_at) VALUES (1, 'Guest', 63, 42, 0)")
            execSQL("INSERT INTO categories (id, name, description, kind) VALUES (1, 'Body', '', 'REGULAR')")
            execSQL(
                "INSERT INTO challenges (id, category_id, title, description, difficulty, is_custom) " +
                    "VALUES (1, 1, 'Stretch break', '', 'EASY', 0), (57, 1, 'My own', '', 'EASY', 1)",
            )
            execSQL(
                "INSERT INTO cases (id, profile_id, challenge_id, status, is_daily, accepted_at, accepted_on, " +
                    "closed_at, closed_on, lux_earned) VALUES " +
                    "(1, 1, 1, 'CLOSED', 1, 1000, 20398, 2000, 20398, 15), " +
                    "(2, 1, 57, 'OPEN', 0, 3000, 20399, NULL, NULL, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(2, ALL_MIGRATIONS)

        assertThat(migrated.longs("SELECT id FROM cases ORDER BY id")).containsExactly(1L, 2L).inOrder()
        assertThat(migrated.longs("SELECT lux_earned FROM cases WHERE id = 1")).containsExactly(15L)
        assertThat(migrated.longs("SELECT closed_on FROM cases WHERE id = 1")).containsExactly(20398L)
        assertThat(migrated.text("SELECT kind FROM categories WHERE id = 8")).isEqualTo("CUSTOM")
        assertThat(migrated.longs("SELECT lux FROM profiles WHERE id = 1")).containsExactly(63L)
        migrated.close()
    }

    @Test
    fun `after 1 to 2, deleting a challenge deletes its cases`() {
        helper.createDatabase(1).apply {
            execSQL("INSERT INTO profiles (id, name, lux, pick_seed, created_at) VALUES (1, 'Guest', 0, 42, 0)")
            execSQL("INSERT INTO categories (id, name, description, kind) VALUES (1, 'Body', '', 'REGULAR')")
            execSQL(
                "INSERT INTO challenges (id, category_id, title, description, difficulty, is_custom) " +
                    "VALUES (57, 1, 'My own', '', 'EASY', 1)",
            )
            execSQL(
                "INSERT INTO cases (id, profile_id, challenge_id, status, is_daily, accepted_at, accepted_on, " +
                    "closed_at, closed_on, lux_earned) VALUES (1, 1, 57, 'OPEN', 0, 0, 20399, NULL, NULL, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(2, ALL_MIGRATIONS)
        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL("DELETE FROM challenges WHERE id = 57")
        assertThat(migrated.longs("SELECT count(*) FROM cases")).containsExactly(0L)
        migrated.close()
    }

    @Test
    fun `2 to 3 gives each profile a sigil seed equal to its pick seed, and keeps everything else`() {
        helper.createDatabase(2).apply {
            execSQL("INSERT INTO profiles (id, name, lux, pick_seed, created_at) VALUES (1, 'Guest', 63, 42, 0)")
            execSQL(
                "INSERT INTO cases (id, profile_id, challenge_id, status, is_daily, accepted_at, accepted_on, " +
                    "closed_at, closed_on, lux_earned) VALUES (1, 1, 1, 'CLOSED', 1, 1000, 20398, 2000, 20398, 15)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(3, ALL_MIGRATIONS)

        assertThat(migrated.longs("SELECT sigil_seed FROM profiles WHERE id = 1")).containsExactly(42L)
        assertThat(migrated.longs("SELECT pick_seed FROM profiles WHERE id = 1")).containsExactly(42L)
        assertThat(migrated.longs("SELECT lux FROM profiles WHERE id = 1")).containsExactly(63L)
        assertThat(migrated.longs("SELECT lux_earned FROM cases WHERE id = 1")).containsExactly(15L)
        migrated.close()
    }
}
